# **1. Giới thiệu:**
Website quản lý bán vé xem phim với các tính năng chính: tìm kiếm phim, đặt vé, chọn suất chiếu và thanh toán.
Trong đó, tôi phụ trách phần Back-end
- Back-end: https://github.com/phamlinh296/cinego_ticket/tree/main/cine_be
- Front end: https://github.com/phamlinh296/cinego_ticket/tree/main/cine_fe/fe-src
<br/>

# **2. Công nghệ sử dụng:**
- **Database: MySQL**

- **Backend: Restful API**
  - Java 21, Spring Boot 3.3.4

  - OAuth2 Resource Server, JWT Authentication

  - Spring Data JPA

  - Redis Cache (@Cacheable), Redis Pub/Sub để gửi email bất đồng bộ

  - Docker, Maven

- **Frontend:**
	- HTML, CSS, JS

<br/><br/>

# **3. Thông tin hệ thống:**
### **Các Website:**
- Website chính (Front-end): http://localhost:80 
	- Hiển thị danh sách phim, thông tin suất chiếu, đặt vé và thanh toán.
<br/><br/>

- Website API (Back-end): http://localhost:9595
	- Xử lý các request từ front-end, xác thực người dùng, phân quyền, quản lý booking và thanh toán.
<br/><br/>


# **4. Mô hình hoạt động:**
### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Mô hình Database:**
<div align='center'>
	<img src='images/cine_database.png' />
</div>
<br/>

# **5. Chức năng chính:**
### **A. Chức năng của User:**
- Đăng ký, đăng nhập, đăng xuất
- Tìm kiếm phim theo tên, thể loại
- Đặ̣t vé:
	+ Lựa chọn suất chiếu (ngày, giờ, phòng chiếu)
	+ Chọn chỗ ngồi
	+ Thanh toán VNPAY (chưa hoàn thiện)

### **B. Chức năng của Admin:**
- Quản lý user, kiểm tra và cập nhật danh sách user spam/blacklist
- Quản lý phim, lịch chiếu, phòng chiếu, số lượng ghế
- Tự động xử lý các booking Pending quá hạn
<br/><br/>

# **6. Bảo mật và caching:**
### Authentication & Authorization (Sử dụng JWT - JSON Web Token)
#### &nbsp;&nbsp;1. Thuật toán sử dụng: `HS512` với key có kích thước `32 bit`
#### &nbsp;&nbsp;2. Loại data có trong token bao gồm:
- `scope` : Quyền truy cập (ADMIN, USER)
- `sub` : Username của người dùng.
- `iat` : Thời điểm tạo token.
- `exp` : Thời điểm hết hạn của token (sau 1 giờ kể từ lúc tạo)
- `jti` : ID của token
### Hiệu suất & Caching
🔹 Sử dụng Spring Cache (@Cacheable) để lưu kết quả truy vấn phim, giảm tải cho database.
🔹 Tích hợp Redis Pub/Sub để xử lý bất đồng bộ email thông báo khi người dùng thanh toán thành công.
<br/>

# **7. Cách chạy dự án:**
1. Clone dự án
git clone https://github.com/phamlinh296/cinego_ticket.git
cd cinego_ticket
2. Cấu hình .env để thiết lập database và Redis
3. Chạy bằng Docker (nếu đã cài đặt)
docker-compose up