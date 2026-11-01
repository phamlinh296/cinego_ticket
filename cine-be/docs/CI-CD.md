# CineGo backend CI — Giai đoạn B

Tài liệu mô tả CI và evidence local của Giai đoạn B. CI đã được kiểm chứng trên
[draft PR #2](https://github.com/phamlinh296/cinego_ticket/pull/2):
[run 41340915843 thành công](https://github.com/phamlinh296/cinego_ticket/actions/runs/41340915843),
49 test pass và Docker build pass. AI trả phí, GHCR, staging và production chưa bật.

Source cho các giai đoạn tiếp theo hiện có tại
[AI-WORKFLOWS.md](AI-WORKFLOWS.md) và [RELEASE-RUNBOOK.md](RELEASE-RUNBOOK.md).
[DELIVERY-STATUS.md](DELIVERY-STATUS.md) ghi kết quả mới nhất; bảng 15 test dưới đây
là evidence của Giai đoạn B trước khi bổ sung health/security tests.

## Phạm vi file

- Root `.github/workflows/ci.yml`: một workflow CI, giữ tên workflow và job `build` hiện có.
- Root `.dockerignore`: chỉ cho phép metadata reactor và source cần để build backend.
- `cine-be/pom.xml`: JUnit/Mockito dùng version từ Spring Boot BOM;
  Testcontainers BOM được override đồng bộ lên `1.21.4` để tương thích Docker Engine mới;
  Failsafe chạy `*IT` trong `verify` và báo lỗi nếu không tìm thấy integration test.
- `BookingServiceImpl.createBooking()`: thêm `@Transactional` sau khi người dùng
  duyệt bản sửa cho hai lỗi đã tái hiện bằng MySQL.
- `cine-be/Dockerfile.production`: build từ root, runtime non-root.
- `cine-be/src/test`: unit/MVC tests và integration test MySQL riêng.

## Chạy local

Chạy từ **root Git repository `cinego_ticket`**, dùng JDK 21. Docker Desktop phải
đang chạy Linux containers; không cần khởi động Compose của ứng dụng.

```powershell
# Windows: unit/MVC tests, không chạy integration test
.\mvnw.cmd -B -ntp -pl cine-be -am test

# Windows: build + unit/MVC + MySQL integration tests
.\mvnw.cmd -B -ntp -pl cine-be -am verify
```

```sh
# Linux/macOS; chuẩn hóa wrapper nếu checkout có CRLF từ Windows
sed -i 's/\r$//' ./mvnw
chmod +x ./mvnw
./mvnw -B -ntp -pl cine-be -am verify
```

`test` pass chưa chứng minh integration test pass. Không dùng `skipTests`,
`skipITs`, `testFailureIgnore` hoặc `continue-on-error` để vượt cổng CI.

## MySQL integration tests

`BookingPersistenceIT` dùng MySQL `8.0.36` qua Testcontainers, cổng host ngẫu nhiên,
database và credential tạm. Container bị xóa sau test. Không dùng URL/credential
trong `application.yaml`, không seed từ `db/data.sql`, không đổi schema DB hiện có.

Context test chỉ nạp JPA, các repository booking cần thiết và service thật.
MySQL, repository và pessimistic lock đều thật. Mail, thanh toán và các dependency
không thuộc kiểm thử được cô lập; Kafka/Redis/HBase/Spark không được khởi động.

Các hành vi kiểm tra:

1. Đặt ghế thành công lưu booking và các ghế `PENDING`.
2. Request thứ hai tuần tự bị từ chối khi ghế đã `PENDING`.
3. Lỗi ở bước lưu booking phải rollback mọi thay đổi ghế trước đó.
4. Hai user đồng thời đặt cùng ghế: chỉ một booking được commit; user còn lại
   bị từ chối vì ghế đã được giữ.

Fixture được commit trước khi gọi service. Không bọc transaction quanh lời gọi
service trong test vì cách đó sẽ che thiếu transaction trong production.
Test concurrency dùng hai thread và latch, không dùng `sleep` hoặc mock lock.
Một proxy quan sát bên ngoài repository thật mở rộng khoảng thời gian sau đọc;
thời gian chờ hữu hạn để test vẫn hoạt động khi service giữ lock đúng đến commit.
Test rollback chèn lỗi tại ranh giới ghi booking rồi đọc lại trạng thái đã commit
qua repository thật.

Nếu Docker không sẵn sàng, integration test **fail**, không tự skip hay chuyển H2.
Lần đầu cần mạng để tải Maven dependencies, MySQL và Ryuk; không cần production secrets.

## GitHub Actions

Trigger: PR vào `main`, push `main`, hoặc `workflow_dispatch`.

Job `build`:

1. Checkout với `persist-credentials: false`.
2. JDK 21 và Maven cache.
3. Chạy Node/Python tests cho helpers AI/release/rollback, không gọi API ngoài.
4. Kiểm tra Docker daemon.
5. `./mvnw -B -ntp -pl cine-be -am verify` từ root reactor.
6. Upload Surefire/Failsafe reports kể cả khi test fail, giữ 7 ngày.

Job `docker-build-check` chỉ chạy sau `build` thành công và build image với
`push: false`. Hai job kiểm chứng dùng `contents: read`; mọi action được pin SHA.
Job reusable `release` chỉ được gọi sau hai job pass trên push `main` khi
`CINEGO_RELEASE_ENABLED=true`; job publish riêng có `packages: write`, deploy
dùng environment secrets. PR không được gọi release. AI nằm trong workflow riêng.
Không tạo song song một CI khác.

**Render deploy hook cũ đã được bỏ khỏi file CI trong workspace.** Khi các thay
đổi được merge sau này, push `main` chạy CI và chỉ gọi release khi opt-in bật.
Render auto-deploy cấu hình ngoài repository vẫn cần chủ dịch vụ kiểm tra;
chưa thay đổi setting đó.

Branch protection/required checks và GitHub Environments chưa được cấu hình.
Run/PR và snapshot cấu hình GitHub cụ thể đã ghi trong
[DELIVERY-STATUS.md](DELIVERY-STATUS.md); không suy ra từ test local.

## Docker build

```sh
docker build --progress=plain -f cine-be/Dockerfile.production -t cinego-be:ci .
docker image inspect cinego-be:ci --format '{{.Config.User}}'
docker run --rm --entrypoint id cinego-be:ci -u
```

Build context là root; Maven cần đọc cả `spark-processor/pom.xml` để khám phá
reactor, nhưng không build module Spark. Context không chứa frontend, local data,
IDE files, `.git`, tài liệu bundle, test source hoặc các JAR đã build trên host.
Wrapper được chuẩn hóa CRLF trong image trước khi chạy.

Docker build bỏ qua test vì CI đã kiểm thử trên host trước đó. Build image riêng
lẻ không chứng minh test pass và không chứng minh ứng dụng runtime đã ready.
Image chưa được publish hay deploy. Credential fallback đã được bỏ trong PR;
credential từng dùng thật vẫn cần chủ tài khoản rotate/revoke trước phát hành.
Base images được pin digest; cần cập nhật có kiểm tra để nhận security patches.
BuildKit cache Maven dependencies trong build stage, không đưa cache vào runtime image.

## Báo cáo và phân loại lỗi

- Unit/MVC reports: `cine-be/target/surefire-reports/`.
- Integration reports: `cine-be/target/failsafe-reports/`.
- **Môi trường:** Docker socket/daemon, image download, registry rate limit,
  Maven network; sửa môi trường rồi chạy lại cùng lệnh.
- **Test/config:** context/fixture/schema database tạm không tạo được; xác định
  nguyên nhân trong reports, không thay production để chữa lỗi fixture.
- **Ứng dụng:** assertion về booking count hoặc seat status sai dù MySQL và
  fixture đã hoạt động; giữ test đỏ, đề xuất bản sửa production để duyệt riêng.

## Evidence local ban đầu — 02/11/2026

Sau khi bổ sung test, `verify` đã chạy trên MySQL thật:

- Unit/MVC: **11 pass**, 0 fail/error.
- Integration: **2 pass, 2 fail**, 0 error/skip.
- Fail `concurrentRequestsForSameSeatAllowOnlyOneCommittedBooking`: cả hai user
  nhận booking thành công, DB có hai booking cho cùng ghế.
- Fail `bookingSaveFailureRollsBackEverySeatChange`: booking không được lưu nhưng
  cả hai ghế đã commit thành `PENDING`.

Hai lỗi trên xuất hiện khi `createBooking()` chưa có transaction bao trùm.
Người dùng đã duyệt bản sửa tối thiểu: thêm Spring `@Transactional` cho phương thức
đó, giữ nguyên API/schema. Không đổi kỳ vọng hoặc bỏ integration test.

Nguồn lựa chọn Testcontainers:
[release 1.21.4 chính thức](https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4)
điều chỉnh tương thích Docker Engine mới. Không nâng major Testcontainers hoặc Spring Boot.

## Evidence sau bản sửa đã duyệt — 02/11/2026

Lệnh chạy từ root:

```powershell
.\mvnw.cmd -B -ntp -pl cine-be -am clean verify
```

Kết quả thực tế: **BUILD SUCCESS**.

| Suite | Pass | Fail | Error | Skip |
|---|---:|---:|---:|---:|
| BookingServiceImplTest | 7 | 0 | 0 | 0 |
| BookingControllerTest | 1 | 0 | 0 | 0 |
| MovieControllerTest | 3 | 0 | 0 | 0 |
| BookingPersistenceIT (MySQL thật) | 4 | 0 | 0 | 0 |
| Tổng | 15 | 0 | 0 | 0 |

Hai test từng fail đã pass sau khi thêm transaction, với cùng kỳ vọng về dữ liệu.
Không có test trong `shared-model`; authentication/context test cũ vẫn bị comment,
không được tính là pass hoặc skip. Compile còn các warning có sẵn về Lombok,
MapStruct, deprecated API và unchecked operations.

Workflow đã qua `actionlint 1.7.12`; binary được tải từ release chính thức và kiểm
tra SHA256. `git diff --check` không phát hiện lỗi whitespace trong các file sửa.

Image cuối build thành công với lệnh Docker ở trên. `docker image inspect` xác
nhận `user=app`; `docker run --rm --entrypoint id cinego-be:ci -u` trả về `999`.
Sau khi test kết thúc không còn container mang label Testcontainers đang chạy.
Đây là image local; không push registry hoặc chạy service ứng dụng.

Lần thiết lập đầu có lỗi test config do `create-drop` thử drop foreign key trên
database rỗng khi bật `halt_on_error`. Đã đổi sang `create-only` chỉ trong test;
Testcontainers vẫn hủy toàn bộ DB tạm sau khi chạy. Docker build ban đầu gặp CRLF
ở wrapper; bước chuẩn hóa line endings đã khắc phục lỗi này.

Giới hạn: bằng chứng này kiểm tra `createBooking`, không bao phủ toàn bộ luồng
cancel/payment/scheduler, deadlock khi chọn nhiều ghế hoặc phân quyền API. Các
rủi ro đó cần test và thay đổi được review riêng. Chưa có bằng chứng GitHub run,
khởi động runtime đầy đủ hoặc triển khai Render.
