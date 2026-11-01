"""Deploy/rollback an immutable GHCR image. Stdlib only; dry-run by default.

No Render request is made without --execute. Do not retry POST: a timeout can
mean Render accepted it. Inspect the deploy list before retrying manually.
"""
import argparse
from dataclasses import dataclass, field
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import re
import sys
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import HTTPRedirectHandler, Request, build_opener

IMAGE = re.compile(r"^(ghcr\.io/[a-z0-9._-]+/[a-z0-9._/-]+)@sha256:([a-f0-9]{64})$")
FAILED = {"build_failed", "update_failed", "pre_deploy_failed", "canceled", "deactivated"}
HEALTH_PATH = "/actuator/health/readiness"


class ReleaseError(RuntimeError):
    """Messages are safe for CI logs; upstream response bodies are never logged."""


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


@dataclass(frozen=True)
class Config:
    image: str
    repository: str
    service: str
    health_url: str
    token: str = field(default="", repr=False)
    expected_current: str = ""
    rollback: bool = False

    def validate(self, execute=False):
        match = IMAGE.fullmatch(self.image)
        if not match or match.group(1) != self.repository:
            raise ReleaseError("IMAGE_REF must be a sha256 digest in EXPECTED_IMAGE_REPOSITORY")
        if not re.fullmatch(r"srv-[a-z0-9]+", self.service):
            raise ReleaseError("Invalid RENDER_SERVICE_ID")
        url = urlsplit(self.health_url)
        if (url.scheme != "https" or not url.hostname or url.username or url.password
                or url.query or url.fragment or url.path != HEALTH_PATH or url.port not in (None, 443)):
            raise ReleaseError("HEALTH_URL must be an HTTPS readiness endpoint without credentials/query/redirects")
        if self.expected_current:
            current = IMAGE.fullmatch(self.expected_current)
            if not current or current.group(1) != self.repository:
                raise ReleaseError("EXPECTED_CURRENT_IMAGE must be a digest from the same repository")
        if self.rollback and not self.expected_current:
            raise ReleaseError("Rollback requires EXPECTED_CURRENT_IMAGE to prevent a stale rollback")
        if execute and not self.token:
            raise ReleaseError("Missing RENDER_API_KEY; no deployment was started")


class RenderAPI:
    def __init__(self, token, sleep=time.sleep):
        self.token = token
        self.sleep = sleep
        self.opener = build_opener(NoRedirect())

    def request(self, method, path, payload=None):
        # The API host is fixed. Never take an API URL from PR or environment input.
        url = "https://api.render.com/v1" + path
        attempts = 3 if method == "GET" else 1
        for attempt in range(attempts):
            req = Request(url, method=method,
                          data=json.dumps(payload).encode() if payload is not None else None,
                          headers={"Authorization": "Bearer " + self.token,
                                   "Accept": "application/json", "Content-Type": "application/json"})
            try:
                with self.opener.open(req, timeout=25) as response:
                    return json.load(response)
            except HTTPError as error:
                if method == "GET" and error.code in {429, 500, 502, 503, 504} and attempt + 1 < attempts:
                    self.sleep(2 ** attempt)
                    continue
                raise ReleaseError(f"Render {method} failed (HTTP {error.code}); inspect the dashboard") from None
            except (URLError, TimeoutError, OSError, ValueError):
                message = ("Render POST outcome is unknown; inspect existing deploys before retrying"
                           if method == "POST" else "Render GET failed; inspect network/service access")
                raise ReleaseError(message) from None

    def smoke(self, url):
        # No Render API token is sent to the application health endpoint.
        req = Request(url, headers={"Accept": "application/json", "User-Agent": "CineGo-Release/1"})
        try:
            with self.opener.open(req, timeout=15) as response:
                body = response.read(16385)
                return (response.status == 200 and len(body) <= 16384
                        and json.loads(body).get("status") == "UP")
        except (HTTPError, URLError, TimeoutError, OSError, ValueError, AttributeError):
            return False


def digest_from_deploy(deployment, repository):
    image = deployment.get("image") or {}
    ref = image.get("ref", "")
    match = IMAGE.fullmatch(ref)
    sha = image.get("sha", "").removeprefix("sha256:")
    if not re.fullmatch(r"[a-f0-9]{64}", sha):
        raise ReleaseError("Render did not return a verifiable resolved image SHA")
    ref_repository = ref.split("@", 1)[0] if "@" in ref else ref.rsplit(":", 1)[0]
    if ref_repository != repository or (match and match.group(2) != sha):
        raise ReleaseError("Render image identity does not match the configured repository/digest")
    return repository + "@sha256:" + sha


def service_preflight(config, service):
    if not isinstance(service, dict):
        raise ReleaseError("Invalid Render service response")
    details = service.get("serviceDetails") or {}
    image_path = service.get("imagePath", "")
    image_repository = image_path.split("@", 1)[0] if "@" in image_path else image_path.rsplit(":", 1)[0]
    if (service.get("id") != config.service or service.get("type") != "web_service"
            or image_repository != config.repository or service.get("repo")
            or service.get("autoDeploy") != "no" or service.get("suspended") != "not_suspended"):
        raise ReleaseError("Service must be an active image-backed web service with matching image and autoDeploy=no")
    expected = urlsplit(config.health_url)
    actual = urlsplit(details.get("url", ""))
    if (actual.scheme, actual.netloc) != (expected.scheme, expected.netloc) or details.get("healthCheckPath") != HEALTH_PATH:
        raise ReleaseError("HEALTH_URL/Render healthCheckPath must match this service's readiness endpoint")


def deploy(config, client, *, sleep=time.sleep, clock=time.monotonic, timeout=900, result=None):
    config.validate(execute=True)
    result = result if result is not None else {}
    result.update(service_id=config.service, image_ref=config.image, rollback=config.rollback,
                  previous_image="", status="preflight", timestamp=datetime.now(timezone.utc).isoformat())
    base = "/services/" + config.service
    service_preflight(config, client.request("GET", base))
    recent = client.request("GET", base + "/deploys?limit=20")
    if not isinstance(recent, list) or any(not isinstance(entry, dict) for entry in recent):
        raise ReleaseError("Invalid Render deployment list")
    for entry in recent:
        item = entry.get("deploy", entry)
        if item.get("status") == "live":
            result["previous_image"] = digest_from_deploy(item, config.repository)
            break
    if config.expected_current and result["previous_image"] != config.expected_current:
        raise ReleaseError("Current live digest changed; refusing a stale rollback/deploy")

    created = client.request("POST", base + "/deploys", {"imageUrl": config.image})
    deployment_id = created.get("id") if isinstance(created, dict) else None
    if not re.fullmatch(r"dep-[a-z0-9]+", deployment_id or ""):
        raise ReleaseError("Render did not return a deployment ID; inspect the dashboard before retrying")
    result.update(deploy_id=deployment_id, status="deploying")
    deadline = clock() + timeout
    while clock() < deadline:
        current = client.request("GET", base + "/deploys/" + deployment_id)
        if not isinstance(current, dict):
            raise ReleaseError("Invalid Render deployment response")
        status = current.get("status")
        if status in FAILED:
            raise ReleaseError("Render deployment failed: " + status)
        if status == "live":
            if digest_from_deploy(current, config.repository) != config.image:
                raise ReleaseError("Live deployment digest differs from requested image")
            break
        sleep(10)
    else:
        raise ReleaseError("Render deployment timed out; inspect before retrying")

    for _ in range(12):
        if client.smoke(config.health_url):
            result.update(status="live", smoke_passed=True)
            return result
        sleep(5)
    raise ReleaseError("Deploy is live but readiness failed; use previous_image to investigate/rollback")


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--execute", action="store_true", help="Explicitly allow Render API mutation")
    parser.add_argument("--rollback", action="store_true")
    parser.add_argument("--result-file", default="render-result.json")
    args = parser.parse_args(argv)
    config = Config(os.getenv("IMAGE_REF", ""), os.getenv("EXPECTED_IMAGE_REPOSITORY", ""),
                    os.getenv("RENDER_SERVICE_ID", ""), os.getenv("HEALTH_URL", ""),
                    os.getenv("RENDER_API_KEY", ""), os.getenv("EXPECTED_CURRENT_IMAGE", ""), args.rollback)
    result = {}
    try:
        config.validate(execute=args.execute)
        if not args.execute:
            print("DRY RUN: configuration valid; no Render API request or deployment was made")
            return 0
        deploy(config, RenderAPI(config.token), result=result)
        print("Deployment digest and readiness verified")
        return 0
    except (ReleaseError, ValueError, TypeError) as error:
        result.update(status="failed", error=str(error) if isinstance(error, ReleaseError) else "Invalid response/configuration")
        print("RELEASE FAILED: " + result["error"], file=sys.stderr)
        return 1
    finally:
        if args.execute:
            Path(args.result_file).write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
            if os.getenv("GITHUB_OUTPUT"):
                with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
                    for name in ("previous_image", "image_ref", "deploy_id"):
                        value = str(result.get(name, ""))
                        if "\n" not in value and "\r" not in value:
                            output.write(name + "=" + value + "\n")


if __name__ == "__main__":
    sys.exit(main())
