# Trạng thái bàn giao CineGo AI-assisted CI/CD

Ngày kiểm chứng local: **02/11/2026 (Asia/Saigon)**. Branch:
`feat/ai-cicd-pipeline`. Chủ repo đã cho phép commit/push và tạo draft PR sau
kiểm chứng local; chưa cho phép merge, gọi AI trả phí hoặc deploy. Trạng thái
PR/CI online cụ thể được ghi trong báo cáo bàn giao cuối phiên và trên GitHub.

## Đã triển khai trong source

| Phần | File/chức năng | Evidence |
|---|---|---|
| CI backend | Root `ci.yml`: automation tests, Maven reactor, MySQL, reports, Docker check | Test/build/lint local pass |
| Sửa booking đã duyệt | `createBooking()` có transaction bao trùm | Hai lỗi từng tái hiện trên MySQL đã được regression test |
| AI review | `ai-pr-review.yml` + `reusable-ai-review.yml`, prompt/schema và helpers | Test filtering/redaction/validation/stale comment pass; chưa gọi model |
| Artifact/release | `backend-release.yml`, GHCR digest, staging → reviewer gate → production cùng digest | YAML lint và guard tests pass; chưa publish |
| Render/rollback | Script dry-run mặc định, preflight, polling, resolved SHA, readiness, previous digest; `backend-rollback.yml` | HTTP giả lập pass; chưa diễn tập staging |
| Readiness | Actuator, GET probes public, status-only, MySQL/Redis đưa readiness xuống 503 khi lỗi | 5 health/security tests pass |
| Config | Profile prod validate schema, endpoint bằng env, bỏ credential fallback mail/JWT và legacy comment | Build/test pass; chưa rotate credential ngoài hệ thống |
| Chia sẻ workflow | Reusable workflow, caller template, prompt IDE, case/evaluation CSV và onboarding | Bộ tài nguyên có sẵn; chưa có adoption thực tế |

## Kết quả thực tế

| Nhóm | Test pass | Fail/error/skip cuối cùng |
|---|---:|---:|
| Java unit/MVC/health-security | 16 | 0 |
| MySQL Testcontainers integration | 4 | 0 |
| Node helpers AI/release | 13 | 0 |
| Python Render/rollback (HTTP giả lập) | 16 | 0 |
| **Tổng** | **49** | **0** |

- Maven reactor: **BUILD SUCCESS**.
- Docker image `cinego-be:ci`: build thành công, user `app`, UID `999`.
- `actionlint 1.7.12`: cả 5 active workflows pass; đã kiểm checksum binary.
- `git diff --check`: không có lỗi whitespace; Git có warning CRLF của working tree Windows.
- Lần đầu test health fail do mock HealthIndicator không thực thi default method
  mà Actuator aggregator gọi. Đã thay bằng real controllable indicator trong
  fixture; không đổi kỳ vọng 503 hoặc bỏ test. Lần chạy cuối mọi test pass.
- Authentication/context test cũ vẫn bị comment; không được tính là pass/skip.
- Compile còn warning đã có về Lombok/MapStruct/deprecated/unchecked operations.

Các lệnh kiểm chứng đã chạy từ root (Windows dùng `python` thay `python3`):

```powershell
.\mvnw.cmd -B -ntp -pl cine-be -am verify
node --test cine-be/automation/tests/ai-review.test.cjs
python -m unittest discover -s cine-be/automation/tests -p 'test_*.py' -v
docker build --progress=plain -f cine-be/Dockerfile.production -t cinego-be:ci .
docker run --rm --entrypoint id cinego-be:ci -u
.\cine-be\target\tools\actionlint\actionlint.exe .github/workflows/ci.yml .github/workflows/ai-pr-review.yml .github/workflows/reusable-ai-review.yml .github/workflows/backend-release.yml .github/workflows/backend-rollback.yml
git diff --check
```

## Files đã đọc/đối chiếu

Handoff và toàn bộ bundle reference; POM root/backend/shared-model/spark;
workflow CI thật; Dockerfile và build context; config/security/JWT decoder;
booking service/repositories/entities/controller và các test liên quan; README
root/backend và hướng dẫn infrastructure. Đã kiểm tra tài liệu/schema chính thức
của Codex Action, Render API và GitHub environment trước khi dùng các input/field.

## Files tạo/sửa trong phần C–E

- Root `.github/workflows/ci.yml`, `ai-pr-review.yml`, `reusable-ai-review.yml`,
  `backend-release.yml`, `backend-rollback.yml`; root `README.md` thêm liên kết.
- Backend `pom.xml`, `Dockerfile.production`, `.gitignore`, `.env.example`, `readme.md` thêm
  liên kết và lưu ý env; giữ nội dung hướng dẫn backend cũ.
- `src/main/resources/application.yaml`, `application.properties`,
  `application-health.yaml`, `application-prod.yaml`.
- `src/main/java/.../config/SecurityConfig.java` chỉ thêm public GET probes.
- `src/test/java/.../config/HealthProbeTest.java`; bỏ literal signer key cũ trong
  authentication test đang comment, không kích hoạt test đó.
- `automation/`: helpers, Render client, tests, review schema/prompts, mẫu caller
  chưa kích hoạt, synthetic evaluation cases và CSV chưa có số liệu model.
- `docs/CI-CD.md`, `AI-WORKFLOWS.md`, `RELEASE-RUNBOOK.md`, `DELIVERY-STATUS.md`.

Không sửa frontend, schema DB hiện có, GitHub settings hoặc các thay đổi có sẵn
trong `.idea`/Spark JAR. Không merge/deploy hay gọi API tính phí.

## Chưa thể gọi là hoàn thành end-to-end

1. Commit/push/draft PR đã được duyệt; cần kiểm tra run GitHub thực tế sau khi
   publish. Không suy ra CI online pass chỉ từ kết quả local trong bảng trên.
2. Cần chủ repo cấu hình model/key, budget và opt-in có phê duyệt để chạy AI thật.
3. Cần GHCR pull access, hai Render services và runtime dependencies/config;
   GitHub Environments/reviewers/branch rules phải được chủ repo cấu hình.
4. Cần staging smoke nghiệp vụ, production approval và diễn tập rollback thật.
5. Cần các repo/nhóm thực sự dùng thử và ghi feedback trước khi tuyên bố mở rộng
   đến bộ phận hoặc công ty.

Readiness hiện chưa bao phủ đầy đủ Kafka/HBase/payment. Phân quyền nghiệp vụ,
SAST/dependency scanning, provenance kiểm chứng, migration/backup, alerting và
incident ownership cần review trước dữ liệu production. Không có kết luận
production-grade hoặc adoption tổ chức chỉ từ các YAML và test local.

Credential cũ có thể tồn tại trong lịch sử Git, logs/artifacts hoặc image local
đã build trước khi cleanup. Nếu từng dùng thật cần rotate/revoke; chưa được tự
thay credential hay rewrite history. Không dán giá trị bí mật vào báo cáo.
