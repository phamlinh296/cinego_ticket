import io
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import Mock, patch
from urllib.error import HTTPError

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from render_deploy import Config, ReleaseError, RenderAPI, deploy, main, service_preflight

REPOSITORY = 'ghcr.io/owner/cinego-be'
IMAGE = REPOSITORY + '@sha256:' + 'a' * 64
PREVIOUS = REPOSITORY + '@sha256:' + 'b' * 64
HEALTH = 'https://cinego-stage.onrender.com/actuator/health/readiness'


def configuration(**changes):
    values = dict(image=IMAGE, repository=REPOSITORY, service='srv-stage', health_url=HEALTH,
                  token='unit-test-placeholder')
    values.update(changes)
    return Config(**values)


def live(image=IMAGE):
    return {'id': 'dep-one', 'status': 'live', 'image': {'ref': image, 'sha': image.split('@')[1]}}


def service():
    return {'id': 'srv-stage', 'type': 'web_service', 'imagePath': REPOSITORY + ':old',
            'autoDeploy': 'no', 'suspended': 'not_suspended',
            'serviceDetails': {'url': 'https://cinego-stage.onrender.com',
                               'healthCheckPath': '/actuator/health/readiness'}}


class FakeAPI:
    def __init__(self, statuses=None, smoke=True, previous=PREVIOUS):
        self.statuses = iter(statuses or [live()])
        self.smoke_result = smoke
        self.previous = previous
        self.calls = []

    def request(self, method, path, payload=None):
        self.calls.append((method, path, payload))
        if method == 'POST': return {'id': 'dep-one'}
        if '?limit=' in path: return [{'deploy': live(self.previous)}] if self.previous else []
        if '/deploys/dep-' in path: return next(self.statuses)
        return service()

    def smoke(self, url):
        return self.smoke_result


class RenderDeployTest(unittest.TestCase):
    def test_live_digest_and_readiness_are_verified_and_previous_image_is_preserved(self):
        api = FakeAPI([{'status': 'update_in_progress'}, live()])
        result = deploy(configuration(), api, sleep=lambda _: None)
        self.assertEqual('live', result['status'])
        self.assertEqual(PREVIOUS, result['previous_image'])
        self.assertTrue(result['smoke_passed'])
        self.assertEqual([{'imageUrl': IMAGE}], [call[2] for call in api.calls if call[0] == 'POST'])

    def test_mutable_image_and_wrong_repository_are_rejected_before_http(self):
        for image in [REPOSITORY + ':latest', 'ghcr.io/other/backend@sha256:' + 'a' * 64]:
            with self.subTest(image=image), self.assertRaises(ReleaseError):
                deploy(configuration(image=image), FakeAPI(), sleep=lambda _: None)

    def test_health_url_cannot_contain_credentials_query_or_a_different_path(self):
        for url in ['http://host/actuator/health/readiness', 'https://user:pass@host/actuator/health/readiness',
                    HEALTH + '?key=private', 'https://host/swagger-ui.html']:
            with self.subTest(url=url), self.assertRaises(ReleaseError):
                configuration(health_url=url).validate()

    def test_wrong_service_or_enabled_autodeploy_fails_preflight(self):
        for changes in [{'autoDeploy': 'yes'}, {'repo': 'https://github.com/owner/repo'},
                        {'id': 'srv-production'}, {'suspended': 'suspended'}]:
            with self.subTest(changes=changes), self.assertRaises(ReleaseError):
                service_preflight(configuration(), {**service(), **changes})

    def test_health_url_must_belong_to_the_service(self):
        with self.assertRaises(ReleaseError):
            service_preflight(configuration(health_url=HEALTH.replace('stage', 'production')), service())

    def test_terminal_deploy_failure_stops_before_smoke(self):
        api = FakeAPI([{'status': 'update_failed'}])
        api.smoke = Mock()
        with self.assertRaisesRegex(ReleaseError, 'update_failed'):
            deploy(configuration(), api, sleep=lambda _: None)
        api.smoke.assert_not_called()

    def test_a_live_deploy_with_wrong_digest_is_not_a_success(self):
        with self.assertRaises(ReleaseError):
            deploy(configuration(), FakeAPI([live(PREVIOUS)]), sleep=lambda _: None)

    def test_smoke_failure_keeps_rollback_evidence(self):
        evidence = {}
        with self.assertRaisesRegex(ReleaseError, 'readiness failed'):
            deploy(configuration(), FakeAPI(smoke=False), sleep=lambda _: None, result=evidence)
        self.assertEqual(PREVIOUS, evidence['previous_image'])
        self.assertEqual('dep-one', evidence['deploy_id'])
        self.assertNotEqual('live', evidence['status'])

    def test_timeout_stops_polling(self):
        times = iter([0, 0, 1000])
        with self.assertRaisesRegex(ReleaseError, 'timed out'):
            deploy(configuration(), FakeAPI([{'status': 'update_in_progress'}]),
                   sleep=lambda _: None, clock=lambda: next(times))

    def test_stale_rollback_never_posts(self):
        api = FakeAPI()
        with self.assertRaisesRegex(ReleaseError, 'changed'):
            deploy(configuration(rollback=True, expected_current=IMAGE), api, sleep=lambda _: None)
        self.assertFalse(any(call[0] == 'POST' for call in api.calls))

    def test_rollback_uses_the_selected_previous_digest(self):
        api = FakeAPI(statuses=[live(PREVIOUS)], previous=IMAGE)
        result = deploy(configuration(image=PREVIOUS, rollback=True, expected_current=IMAGE),
                        api, sleep=lambda _: None)
        self.assertTrue(result['rollback'])
        self.assertEqual(PREVIOUS, result['image_ref'])

    def test_rollback_requires_current_digest(self):
        with self.assertRaisesRegex(ReleaseError, 'requires'):
            configuration(rollback=True).validate()

    def test_dry_run_never_instantiates_the_render_client(self):
        env = {'IMAGE_REF': IMAGE, 'EXPECTED_IMAGE_REPOSITORY': REPOSITORY,
               'RENDER_SERVICE_ID': 'srv-stage', 'HEALTH_URL': HEALTH}
        with patch.dict(os.environ, env, clear=True), patch('render_deploy.RenderAPI') as client:
            self.assertEqual(0, main([]))
            client.assert_not_called()

    def test_post_timeout_is_not_retried_or_logged_with_secrets(self):
        api = RenderAPI('unit-test-placeholder')
        api.opener = Mock()
        api.opener.open.side_effect = TimeoutError('must-not-leak')
        with self.assertRaisesRegex(ReleaseError, 'outcome is unknown') as error:
            api.request('POST', '/services/srv-stage/deploys', {'imageUrl': IMAGE})
        self.assertNotIn('must-not-leak', str(error.exception))
        self.assertEqual(1, api.opener.open.call_count)

    def test_get_retries_temporary_errors_only(self):
        api = RenderAPI('unit-test-placeholder', sleep=lambda _: None)
        api.opener = Mock()
        api.opener.open.side_effect = [HTTPError('ignored', 503, 'ignored', {}, None), io.BytesIO(b'{"id":"ok"}')]
        self.assertEqual({'id': 'ok'}, api.request('GET', '/services/srv-stage'))
        self.assertEqual(2, api.opener.open.call_count)

    def test_smoke_sends_no_render_authorization_and_requires_up_json(self):
        api = RenderAPI('unit-test-placeholder')
        response = io.BytesIO(b'{"status":"UP"}')
        response.status = 200
        api.opener = Mock()
        api.opener.open.return_value = response
        self.assertTrue(api.smoke(HEALTH))
        request = api.opener.open.call_args.args[0]
        self.assertFalse(request.has_header('Authorization'))


if __name__ == '__main__':
    unittest.main()
