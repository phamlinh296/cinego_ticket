# CINEGO TICKETING SYSTEM

## HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY ỨNG DỤNG

---

## 1. Giới thiệu

CineGo là hệ thống đặt vé xem phim trực tuyến, bao gồm:

- **Backend**: Spring Boot (Java)
- **Frontend User**: HTML / CSS / JavaScript thuần
- **Frontend Admin**: HTML / CSS / JavaScript, chạy trên Nginx (Docker)
- **Database & Services**: MySQL, Redis (Docker)

Tài liệu này hướng dẫn chi tiết cách cài đặt và chạy toàn bộ hệ thống để phục vụ mục đích demo, học tập và chấm đồ án.

---

## 2. Yêu cầu môi trường

Trước khi chạy ứng dụng, máy tính cần cài đặt:

- Java JDK **17**
- Apache Maven **3.8+**
- Docker & Docker Compose
- IDE Java (IntelliJ IDEA / Eclipse / VS Code)
- Trình duyệt Web (Chrome hoặc Edge)

---

## 3. Cấu trúc project

```
cinego_ticket/
├── cine_be/            # Backend (Spring Boot)
├── cine_fe/            # Frontend User
├── admin/              # Frontend Admin (Nginx Docker)
├── docker-compose.yml  # MySQL, Redis, Kafka
└── README.md           # File hướng dẫn chạy chương trình
```

---

## 4. Cài đặt và chạy ứng dụng

### 4.1. Chạy Backend (cine_be)

#### Bước 1: Khởi động các dịch vụ phụ trợ bằng Docker

Mở Terminal tại **thư mục gốc project cinego_ticket** và chạy:

```bash
docker-compose up -d
```

Lệnh này sẽ khởi chạy các dịch vụ:
- MySQL (port 3306)
- Redis
- Kafka

---

#### Bước 2: Thiết lập Database

1. Sau khi MySQL, Redis, Kafka được bật trên Docker, mở Data Grip, MySQL Workbench, DBeaver hoặc công cụ tương đương. Kết nối tới MySQL tại cổng **3306**
2. Mở file insert data:
   ```bash
   cine_be/src/main/resources/db/data.sql
   ```
3. Thực thi câu lệnh Tạo database abc (3 query đầu tiên) hoặc tạo db 'abc' thủ công


---

#### Bước 3: Khởi chạy Backend, tự động tạo bảng

1. Mở project **cine_be** bằng IDE bất kỳ
2. Chạy file:
   ```
   src/main/java/.../CineGoTicketApplication.java
   ```
3. Backend khởi động thành công khi console hiển thị:

```
Started CineGoTicketApplication in X seconds
```

Backend mặc định chạy tại: http://localhost:9595
Sau khi backend chạy thành công, sẽ tự động tạo bảng trong db abc vừa kết nối ở bước 2

---
#### Bước 4: Insert data
Mở file để run các câu lệnh insert data:
   ```
   cine_be/src/main/resources/db/data.sql
   ```
Lưu ý: Nếu run all bị lỗi, hãy run từng câu insert một.


### 4.2. Chạy Frontend User (cine_fe)

Frontend dành cho người dùng cuối, được xây dựng bằng HTML/CSS/JavaScript thuần.

#### Cách chạy:

1. Mở thư mục **cine_fe** bằng Visual Studio Code
2. Cài đặt extension **Live Server** (Ritwick Dey)
3. Chuột phải vào file `index.html` → **Open with Live Server**

Ứng dụng sẽ tự động mở tại:

```
http://127.0.0.1:5500
```

Đăng nhập với tài khoản:
- username= user3
- password= user3

---

### 4.3. Chạy Frontend Admin (admin)

Frontend Admin được triển khai bằng **Nginx chạy trong Docker**.

#### Bước 1: Di chuyển vào thư mục admin

```bash
cd admin
```

---

#### Bước 2: Build Docker Image

```bash
docker build -t admin-nginx .
```

---

#### Bước 3: Chạy Container

```bash
docker run -d --name admin-nginx -p 80:80 \
-v ./ad-src:/var/www/html \
admin-nginx
```

Giải thích:
- Ánh xạ mã nguồn frontend admin vào Nginx
- Cho phép cập nhật giao diện ngay khi chỉnh sửa code

---

#### Bước 4: Truy cập giao diện Admin

Mở trình duyệt và truy cập:

```
http://localhost
```
Đăng nhập admin với tài khoản:
- username= admin
- password= admin
---

## 5. Lưu ý quan trọng

- Backend **phải được khởi động trước** để các API hoạt động đúng
- Đảm bảo cổng **80** không bị chiếm bởi các ứng dụng khác (XAMPP, IIS, Skype, ...)
- Nếu thay đổi cấu hình Nginx, cần rebuild lại Docker image

---

## 6. Ghi chú triển khai

- Backend được chạy trực tiếp bằng Spring Boot để thuận tiện cho việc debug và trình bày logic
- Các dịch vụ phụ trợ (MySQL, Redis) được docker-compose hóa để đảm bảo môi trường nhất quán
- Frontend Admin sử dụng Docker + Nginx nhằm mô phỏng mô hình triển khai thực tế

---

## 7. Tác giả

Sinh viên thực hiện: **Phạm Mỹ Linh**  
Đề tài: **Hệ thống đặt vé xem phim CineGo**

