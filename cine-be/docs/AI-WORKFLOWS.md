# AI workflow dùng cho CineGo và chia sẻ trong nhóm

Trạng thái: đã triển khai source; CI và guardrail tests đã pass tại local và
[PR #2](https://github.com/phamlinh296/cinego_ticket/pull/2). AI job skip đúng
opt-in/draft guard, chưa có AI review thật hoặc số liệu chất lượng model.
Chưa có bằng chứng áp dụng trong nhóm/bộ phận/công ty. Đây là bộ workflow
production-inspired để pilot; [DELIVERY-STATUS.md](DELIVERY-STATUS.md) ghi run và
cấu hình tài khoản còn thiếu đã kiểm tra thực tế.

## Workflow có sẵn

| Công việc | Cách dùng | Đầu ra | Quyền quyết định |
|---|---|---|---|
| Review PR | `.github/workflows/ai-pr-review.yml` gọi reusable workflow | Một comment tiếng Việt, severity, file/line, commit SHA và phạm vi diff | Developer xác nhận; AI không merge |
| Lập kế hoạch test trong IDE | `automation/prompts/test-plan.md` | Ma trận hành vi, unit/integration cases và cách kiểm chứng | Developer duyệt thay đổi nghiệp vụ |
| Phân tích CI/test fail trong IDE | `automation/prompts/incident-triage.md` | Phân loại môi trường/test/ứng dụng, giả thuyết và bước xác minh | Developer duyệt fix production |

Hai prompt IDE là tài nguyên dùng thủ công; chưa có action tự động cho hai công
việc đó. Không gọi chúng là workflow đã được đánh giá hiệu quả ở tổ chức.

## Bật AI review sau khi được chủ repo chấp thuận chi phí

1. Review/commit/push/merge các file workflow và `cine-be/automation` khi được
   phép. Giữ cờ AI/release tắt trong lần đưa toolkit lên `main` đầu tiên.
2. Tạo project API phù hợp, đặt quyền, ngân sách/alerts và chọn model có quyền
   truy cập. Chi phí/quota cần kiểm tra trên tài khoản thực; không suy ra từ gói IDE.
3. Chủ repo tự tạo Actions secret `OPENAI_API_KEY` qua Settings. Không gửi key
   qua chat, issue, PR hay file source.
4. Tạo repository variables: `CINEGO_AI_MODEL` bằng model đã duyệt và
   `CINEGO_AI_REVIEW_ENABLED=true`. Chỉ bật sau khi được phép gọi API tính phí.
5. Tạo PR nội bộ vào `main`, từ OWNER/MEMBER/COLLABORATOR, không ở trạng thái draft.
6. Xem job summary, xác minh comment gắn đúng head SHA và điền evidence.

Workflow mặc định bỏ qua khi chưa opt-in, thiếu model, PR fork/draft hoặc tác giả
không thuộc nhóm tin cậy. Nếu key thiếu, summary ghi blocker và không gọi model.
PR đầu tiên đưa toolkit vào repo chưa thể review bằng toolkit chưa tồn tại ở
trusted base. Demo AI thực hiện trên PR sau khi bản bootstrap đã được merge.

## Ranh giới bảo vệ

- Job có API key checkout **base SHA**, không checkout PR head/merge. Chỉ lấy
  changed-file patches qua GitHub API; không chạy Maven/npm/script của PR.
- Codex làm việc trong thư mục temp chỉ chứa JSON diff. Prompt/schema đến từ
  trusted base; dùng read-only sandbox, `drop-sudo`, CLI và action được pin.
- Diff giới hạn 80.000 ký tự; bỏ secret files và che một số dạng credential.
  File thiếu patch/quá lớn hoặc patch bị cắt được đánh dấu partial.
- Che giá trị là biện pháp giảm rủi ro, không đảm bảo nhận diện mọi secret.
  Không đưa mã/dữ liệu bị cấm chia sẻ với nhà cung cấp model vào PR được review.
- Prompt coi PR text là dữ liệu không đáng tin. Test local xác nhận xử lý dữ
  liệu/guardrail; không chứng minh model miễn nhiễm prompt injection.
- Codex là bước cuối trong job có key; job đăng comment tách riêng, không có API key.
- Comment được kiểm tra JSON, path/severity/line và head SHA; cập nhật một comment
  bot thay vì spam. Khi PR thay commit, kết quả cũ không được đăng như review mới.
- Không dùng AI review làm required security gate hoặc căn cứ tự merge/deploy.

## Chia sẻ cho repo khác

1. Chọn một commit toolkit đã được nhóm review và phát hành; ghi version/owner.
2. Copy thư mục `cine-be/automation` vào trusted base của repo nhận, hoặc đặt
   tại đường dẫn khác rồi truyền `toolkit_directory` tương ứng.
3. Copy mẫu `automation/templates/ai-review-caller.yml.example` sang
   `.github/workflows/ai-review.yml` ở repo nhận. Thay SHA placeholder bằng
   commit đầy đủ đã review của repo cung cấp reusable workflow.
4. Thay `review_paths`, model và secret/variable theo chính sách của repo nhận.
5. Bootstrap với opt-in tắt; chỉ bật pilot sau khi được duyệt quyền, dữ liệu và chi phí.

Reusable workflow dùng token của repo gọi. Private toolkit repo cần cấu hình
quyền chia sẻ reusable workflows của GitHub; việc đó chưa được thực hiện trong
workspace này. Không hardcode credential, tài khoản riêng hoặc model mặc định
có thể phát sinh chi phí ngoài lựa chọn của chủ repo.

## Pilot và tinh chỉnh dựa trên bằng chứng

Giữ prompt/schema/guardrail cùng version. Chạy các case trong
`automation/evaluation-cases.json` khi được phép gọi model; lưu run URL, model,
prompt revision và nhận xét người duyệt trong `automation/evaluation-results.csv`.
Các case là dữ liệu tổng hợp, không phải kết quả model đã chạy.

Theo dõi: findings được developer xác nhận, false positives, lỗi bị bỏ sót,
thời gian review, chi phí thực mỗi PR và số lần chạy. Không ghi token, dữ liệu
khách hàng hoặc nội dung bị cấm vào bảng đánh giá.

| Mức áp dụng | Bằng chứng cần có trước khi gọi là hoàn thành |
|---|---|
| Cá nhân/CineGo | PR thật có AI comment, developer xác nhận, kiểm thử pass |
| Nhóm | Một repo khác dùng template và ghi feedback/chi phí, có người bảo trì |
| Bộ phận | Nhiều repo pilot, owner/support rõ ràng, review quyền và dữ liệu |
| Công ty | Chính sách được duyệt, quy trình cấp quyền/budget, đào tạo và đo adoption thực |

Tắt AI bằng `CINEGO_AI_REVIEW_ENABLED=false` và hủy các run đang chờ/chạy nếu
cần dừng ngay. Không giả định thay variable sẽ tự hủy các run đã bắt đầu.

Nguồn: [OpenAI Docs về GitHub Action](https://learn.chatgpt.com/docs/github-action),
[GitHub reusable workflows](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows).
