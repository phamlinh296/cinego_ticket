# **1. Tên dự án:**
Website bán vé xem phim sử dụng Java Sping Boot.
<br/>

# **2. Giới thiệu:**
Xây dựng website quản lý bán vé xem phim với các tính năng chính: tìm kiếm phim, đặt vé, chọn suất chiếu và thanh toán.
Trong đó, tôi phụ trách phần Back-end
- Back-end: https://github.com/phamlinh296/cinego_ticket/tree/main/cine_be
- Front end: https://github.com/phamlinh296/cinego_ticket/tree/main/cine_fe/fe-src
<br/>

# **3. Công nghệ:**
- **Database: MySQL**

- **Backend: Restful API**
  - Java 21
  - Spring Boot 3.3.4
  - Maven 
  - OAuth2 Resource Server

- **Frontend:**
	- HTML
	- CSS
	- JS
<br/><br/>

# **4. Thông tin:**
### **A. Các Website:**
- Website chính (Front-end): http://localhost:80 
	- Hiển thị nội dung liên quan đến phim và cho phép đặt/thanh toán vé.
<br/><br/>

- Website cho API (Back-end): http://localhost:9595
	- Xử lý các request được gửi từ front-end.
<br/><br/>


# **5. Mô hình hoạt động:**
### &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**Mô hình Database:**
<div align='center'>
	<img src='images/cine_database.png' />
</div>
<br/>

# **6. Chức năng của trang web:**
### **A. Chức năng của User:**
- Đăng ký
- Đăng nhập
- Đăng xuất
- Tìm kiếm phim theo tên phim, thể loại
- Đặ̣t vé:
	+ Tạo/hủy booking
	+ Tạo thanh toán (payment)
	+ Thanh toán VNPAY (chưa hoàn thiện)

### **B. Chức năng của Admin:**
- Quản lý user.
- Định kỳ check user spam và update danh sách user blacklist.
- Thêm, xóa, sữa dữ liệu liên quan đến các suất chiếu phim như: phim chiếu, lịch chiếu, phòng chiếu, số lượng ghế và lưu lại thông tin thanh toán.
- Định kỳ xử lý booking Pending khi quá thời gian chờ.
<br/><br/>

# **7. Bảo mật:**
### Sử dụng JWT (JSON Web Token) để phân quyền truy cập và xác thực user.
### Cấu trúc của token:
#### &nbsp;&nbsp;1. Thuật toán sử dụng: `HS512` với key có kích thước `256 bit`
#### &nbsp;&nbsp;2. Loại data có trong token bao gồm:
- `scope` : Dùng để phân quyền người dùng, bao gồm `ADMIN`, `USER` 
- `sub` : Chứa username của người dùng.
- `iat` : Thời điểm tạo token.
- `exp` : Thời điểm hết hạn của token (sau 1 giờ kể từ lúc tạo)
- `jti` : ID của token
<br/>
