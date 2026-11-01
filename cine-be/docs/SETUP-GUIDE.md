# Thiết lập tài khoản để chạy demo AI và Render

Đối chiếu source và tài liệu chính thức ngày **02/11/2026**. Làm phần AI trước;
phần Render có thể thực hiện sau khi đã chuẩn bị hạ tầng. Tạo tài khoản, key,
chọn gói/thanh toán do chủ tài khoản thao tác. Agent chưa bật API hoặc deploy.

## 1. Giữ cờ tắt trong lúc chuẩn bị

Mở [GitHub Actions variables của CineGo](https://github.com/phamlinh296/cinego_ticket/settings/variables/actions),
chọn **New repository variable** và tạo:

| Name | Value ban đầu |
|---|---|
| `CINEGO_AI_REVIEW_ENABLED` | `false` |
| `CINEGO_RELEASE_ENABLED` | `false` |

Chưa bật `true` trong bước thiết lập. PR bootstrap hiện là
[PR #2](https://github.com/phamlinh296/cinego_ticket/pull/2); source toolkit cần
được merge vào trusted `main` trước khi AI có thể review PR tiếp theo.
Trước merge, kiểm tra service Render cũ và tắt Git auto-deploy nếu đang bật,
để việc merge không phát hành ngoài pipeline. Merge cần chủ repo duyệt riêng.

## 2. Tạo OpenAI project và key

1. Đăng nhập [OpenAI Platform](https://platform.openai.com/), tạo/chọn project
   riêng cho pilot, ví dụ `cinego-ci-pilot` trong [Settings](https://platform.openai.com/settings/).
2. Kiểm tra [Billing](https://platform.openai.com/settings/organization/billing):
   credit/quota, điều kiện tài khoản và giá model. Nếu cần thêm phương thức thanh
   toán/credit, bạn chọn ngân sách trước khi xác nhận. Action dùng Platform API key;
   không dùng phiên đăng nhập IDE để xác thực.
3. Trong project **Limits → Spend**, đặt monthly spend limit do bạn chọn, bật
   **Enforce a hard limit** và thêm alert. Alert đơn thuần không chặn request;
   hard limit có thể trễ một chút khi cập nhật usage. Theo
   [OpenAI Docs về spend limits](https://developers.openai.com/api/docs/guides/spend-limits).
4. Mở [API keys](https://platform.openai.com/api-keys) khi đang chọn đúng project,
   tạo key riêng cho CI, đặt tên/expiry phù hợp và lưu trong password manager.
   Nếu hạn chế quyền key/project, cần cho phép model đã chọn và Responses API.
   [OpenAI Docs hướng dẫn tạo key](https://developers.openai.com/api/docs/quickstart).
5. Mở [GitHub Actions secrets](https://github.com/phamlinh296/cinego_ticket/settings/secrets/actions)
   → **New repository secret** → Name `OPENAI_API_KEY`, Secret là key vừa tạo.
   Key chỉ nhập ở trang Settings; không gửi qua chat, source hoặc PR.
6. Trong Actions variables, tạo `CINEGO_AI_MODEL`. Ví dụ `gpt-6.1-sol` nếu
   project có quyền truy cập và bạn đã xem giá. Đây là model được
   [OpenAI Docs dùng với Codex](https://learn.chatgpt.com/docs/models?surface=cli);
   chưa được chạy thật trong repo này. Giữ `CINEGO_AI_REVIEW_ENABLED=false`.

Không cần cài OpenAI SDK vào backend: workflow hiện dùng
[Codex GitHub Action](https://learn.chatgpt.com/docs/github-action).

## 3. Chạy pilot AI khi đã sẵn sàng chi phí

Sau khi PR bootstrap đã merge và bạn đồng ý chạy API:

1. Đặt `CINEGO_AI_REVIEW_ENABLED=true`; giữ `CINEGO_RELEASE_ENABLED=false`.
2. Tạo PR nội bộ mới vào `main`, từ tài khoản OWNER/MEMBER/COLLABORATOR, có diff
   nhỏ trong `cine-be/` hoặc `shared-model/`, ở trạng thái ready for review.
3. Trong Actions, mở **AI PR Review**. Cần thấy `eligibility`, `codex` và
   `post-feedback` thực sự chạy thành công, cùng comment tiếng Việt đúng head SHA.
   Kết quả skip hoặc workflow xanh chỉ vì eligibility chưa chứng minh AI hoạt động.
4. Developer kiểm tra findings, điền run/PR/model/chi phí thực vào
   `automation/evaluation-results.csv`; không bịa số liệu từ test helper.

Mỗi PR synchronize có thể gọi API thêm. Đặt variable `false` để dừng trigger mới;
hủy run đang chạy nếu muốn dừng ngay. Một lần chạy có thể gồm nhiều request model.

Đến đây có thể nghiệm thu **CI + AI review** mà chưa cần tạo dịch vụ Render.

## 4. Chuẩn bị runtime trước khi tạo Render services

Hai web services chỉ chạy backend. Chúng vẫn cần các dependency dưới đây,
tách dữ liệu staging/production và có network route truy cập được từ Render:

| Dependency | Biến runtime trong `.env.example` |
|---|---|
| MySQL có schema phù hợp entity hiện tại | `DATABASE_URL` dạng JDBC MySQL, `DATABASE_USERNAME`, `DATABASE_PASSWORD` |
| Redis | `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL_ENABLED` |
| Kafka | `KAFKA_BOOTSTRAP_SERVERS` |
| HBase/ZooKeeper và các RegionServer được client truy cập | `HBASE_ZOOKEEPER_QUORUM`, `HBASE_ZOOKEEPER_PORT` |
| Mail | `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SENDER` |
| JWT và URL app | `JWT_SECRET`, `APP_PUBLIC_URL` |

Copy **tên biến** từ [.env.example](../.env.example) vào Render Environment và
nhập giá trị riêng ở đó. JWT dùng HS512, cần secret ngẫu nhiên đủ mạnh (ít nhất
64 byte); sinh/lưu trong công cụ quản lý secret, không tái dùng key cũ trong Git.
Không upload file chứa credential vào repo.

Profile prod dùng `ddl-auto=validate`, không tự tạo schema/seed. Chuẩn bị schema
là bước riêng cần duyệt; không đổi sang `update` để vượt lỗi startup. KafkaConfig
hiện tạo producer/consumer riêng chỉ từ bootstrap server; dịch vụ yêu cầu
SASL/TLS cần bổ sung cấu hình được review, không chỉ dán connection string.
Không mở Kafka/ZooKeeper công khai để né cấu hình network/security.

Chọn tài khoản/nhà cung cấp và tổng ngân sách hạ tầng trước khi tạo. Xem
[Render pricing](https://render.com/pricing) và điều kiện trong dashboard;
không giả định hai web services miễn phí sẽ chạy được toàn bộ stack này.

## 5. Bootstrap image GHCR

Sau khi được duyệt publish artifact, dùng source đã kiểm thử trên `main` và
các lệnh trong [RELEASE-RUNBOOK.md](RELEASE-RUNBOOK.md#thứ-tự-bootstrap).
Build `linux/amd64`; Dockerfile gắn `org.opencontainers.image.source` với repo
CineGo để liên kết package và hỗ trợ lần publish sau bằng `GITHUB_TOKEN`.

Lần login/push thủ công dùng PAT classic có `write:packages`, nhập tại prompt
`docker login ghcr.io -u phamlinh296`, không đặt token trong command/history.
Workflow publish sau đó dùng `GITHUB_TOKEN`, không cần thêm PAT vào workflow.
Xác nhận package đã liên kết repo và repo có Actions access/write trước release.
[GitHub Docs về GHCR](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).

Package mới mặc định private. Giữ private và tạo credential pull cho Render với
PAT classic `read:packages`; nếu muốn đổi public cần chủ repo duyệt việc public
artifact. Lưu digest từ lần push thật, không dùng digest image local làm evidence GHCR.

## 6. Tạo Render staging, rồi production khi được duyệt

1. Đăng nhập [Render Dashboard](https://dashboard.render.com/).
2. Chọn **New → Web Service → Existing Image**, nhập GHCR image/digest đã publish.
   Thêm registry credential ở bước trên nếu image private. Đây là image-backed
   service; chọn Git repository sẽ tạo sai loại service cho script hiện tại.
3. Đặt tên staging, ví dụ `cinego-be-staging`; chọn region/gói phù hợp ngân sách.
4. Trong Environment, nhập runtime vars ở bước 4 và `SPRING_PROFILES_ACTIVE=prod`.
   Repo đã dùng `server.port=${PORT:9595}`; Render mặc định cung cấp `PORT=10000`.
   Có thể đặt `SERVER_ADDRESS=0.0.0.0` rõ ràng. Xem
   [Render port binding](https://render.com/docs/web-services#port-binding).
5. Đặt **Health Check Path** là `/actuator/health/readiness`.
   [Render health checks](https://render.com/docs/health-checks).
6. **Create Web Service khởi động deploy thật**, chọn bước này sau khi đã duyệt
   chi phí/deploy. Đợi staging UP, chạy smoke nghiệp vụ và lưu digest đã kiểm chứng.
7. Production dùng cùng digest đã qua staging; tạo service riêng khi chủ repo
   duyệt bootstrap production. Lần tạo ban đầu là deploy qua dashboard, ngoài gate
   GitHub của những lần release sau; không gọi đó là deploy đã qua GitHub approval.
8. Lưu hai service IDs `srv-...` và URL native `https://<service>.onrender.com`.
   Hai service dùng dữ liệu/credential riêng. Image-backed services không hỗ trợ
   Git auto-deploy; service Git-backed cũ vẫn cần kiểm tra riêng trước merge.

Nguồn: [Render deploy prebuilt image](https://render.com/docs/deploying-an-image).

## 7. Nối GitHub với Render và giữ production gate

Tạo Render API key tại [Account Settings → API Keys](https://dashboard.render.com/u/settings?add-api-key=).
Theo [Render API Docs](https://render.com/docs/api), lưu key ngay khi tạo và nhập
thẳng vào GitHub environment secrets. Dùng key với quyền phù hợp tài khoản;
không đưa key vào backend Environment vì chỉ job deploy cần dùng nó.

Trong [GitHub Environments](https://github.com/phamlinh296/cinego_ticket/settings/environments),
tạo đúng hai tên `staging`, `production`:

| Nơi | Loại | Name | Giá trị do bạn nhập |
|---|---|---|---|
| Repository | Variable | `RENDER_STAGING_SERVICE_ID` | Staging `srv-...` |
| Repository | Variable | `RENDER_PRODUCTION_SERVICE_ID` | Production `srv-...` khác staging |
| Environment `staging` | Secret | `RENDER_API_KEY` | Key có quyền deploy staging |
| Environment `staging` | Variable | `HEALTH_URL` | URL native staging + `/actuator/health/readiness` |
| Environment `production` | Secret | `RENDER_API_KEY` | Key có quyền deploy production |
| Environment `production` | Variable | `HEALTH_URL` | URL native production + `/actuator/health/readiness` |

Trong production, bật **Required reviewers**, chọn reviewer có thể duyệt run,
và giới hạn deployment branch `main`. Nếu bật **Prevent self-review** phải có
người khác duyệt. Workflow kiểm tra reviewer thật; chỉ tạo environment chưa đủ.
[GitHub Docs về environments](https://docs.github.com/en/actions/how-tos/deploy/configure-and-manage-deployments/manage-environments).

Trong branch protection/ruleset của `main`, review required checks `build`,
`docker-build-check` và PR review phù hợp nhóm; AI advisory không làm required gate.

## 8. Nghiệm thu release/rollback

Khi đã được duyệt phát hành, đặt `CINEGO_RELEASE_ENABLED=true`, tạo thay đổi
được review rồi merge. CI main phải pass trước publish/staging. Production chỉ
tiếp tục sau approval và dùng đúng digest staging; lưu artifacts và smoke kết quả.

Để diễn tập rollback staging trước production: khi staging đã pass và run đang
**chờ production approval**, lưu artifact rồi reject/cancel run đó để nhả concurrency
group. Sau đó chạy **Backend rollback** theo runbook với digest tốt trước đó và
`expected_current_image` đúng live digest. Chỉ phát hành lượt tiếp theo khi rollback
đã được kiểm chứng. Không cancel giữa một Render deploy đang chạy để giả định đã dừng nó.

Hoàn tất cấu hình có thể báo lại **tên model, hai service IDs và URLs**, cùng mức
chi phí được duyệt; không gửi key. Agent kiểm tra metadata/CI rồi chỉ chạy những
thao tác merge, publish, API trả phí hoặc deploy đã được bạn cho phép cụ thể.
