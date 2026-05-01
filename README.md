# 🔐 Auth Service – E-commerce Platform

This service handles **authentication, authorization, and user management** for the e-commerce platform.

It includes:

* JWT-based authentication
* OTP email verification
* Refresh token mechanism
* Role-based access (USER, SELLER, ADMIN)
* Seller approval workflow
* Secure logout with token blacklisting

---

# 🚀 Features

## 👤 User Management

* User Registration with OTP verification
* Email verification required before login
* Secure password storage using BCrypt

## 🔑 Authentication

* JWT-based authentication
* Access Token (short-lived)
* Refresh Token (stored in HttpOnly cookie)
* Stateless session management

## 🔄 Token Management

* Refresh token stored in Redis
* Access token blacklisting on logout
* Automatic token renewal

## 🛡 Role-Based Access Control

* Roles supported:

  * USER
  * SELLER
  * ADMIN
* Multi-role support using `Set<String> roles`

## 🛍 Seller Workflow

* User applies to become seller
* Admin approves/rejects seller requests
* Only approved sellers can access seller APIs

---

# 🧩 Tech Stack

* Java 17
* Spring Boot
* Spring Security
* JWT (jjwt)
* Redis
* MySQL
* Lombok

---

# 🏗 System Flow

```
Register → OTP → Verify → Login
        ↓
Access Token + Refresh Token
        ↓
API Requests (JWT)
        ↓
Refresh Token (if expired)
        ↓
Logout → Blacklist + Delete Refresh
```

---

# 🔐 Authentication Flow

## 1. Register

* User provides email, password
* OTP is sent
* User stored as `isVerified = false`

## 2. Verify OTP

* User submits OTP
* Account is activated (`isVerified = true`)

## 3. Login

* Only verified users can login
* Generates:

  * Access Token (JWT)
  * Refresh Token (cookie)

---

# 🔄 Token Flow

| Token Type    | Storage         | Purpose                   |
| ------------- | --------------- | ------------------------- |
| Access Token  | Frontend        | API authentication        |
| Refresh Token | HttpOnly Cookie | Generate new access token |

---

# 🚪 Logout Flow

* Access token is **blacklisted in Redis**
* Refresh token is **deleted from Redis**
* Cookie is cleared

---

# 🧠 Role Management

| Role   | Permissions         |
| ------ | ------------------- |
| USER   | Browse, cart        |
| SELLER | Add/update products |
| ADMIN  | Full control        |

---

# 🛍 Seller Approval Flow

```
Register → USER
↓
Apply Seller → PENDING
↓
Admin Reviews
↓
APPROVED → SELLER role added
REJECTED → No access
```

---

# 📡 API Endpoints

## 🔹 Auth APIs

| Endpoint               | Method | Description   |
| ---------------------- | ------ | ------------- |
| `/api/auth/register`   | POST   | Register user |
| `/api/auth/verify`     | POST   | Verify OTP    |
| `/api/auth/resend-otp` | POST   | Resend OTP    |
| `/api/auth/login`      | POST   | Login         |
| `/api/auth/refresh`    | POST   | Refresh token |
| `/api/auth/logout`     | POST   | Logout        |

---

## 🔹 Seller APIs

| Endpoint                    | Method | Description            |
| --------------------------- | ------ | ---------------------- |
| `/api/auth/apply-seller`    | POST   | Apply for seller       |
| `/api/auth/approve-seller`  | POST   | Approve seller (ADMIN) |
| `/api/auth/pending-sellers` | GET    | View pending sellers   |

---

# 🗄 Database Schema

## users

```
id | name | email | password | is_verified | seller_status
```

## user_roles

```
user_id | role
```

---

# 🔐 Security Highlights

* Password encrypted using BCrypt
* JWT signed with secret key
* Refresh token stored in HttpOnly cookie
* Redis used for:

  * Refresh token validation
  * Access token blacklist
* Stateless authentication (no sessions)

---

# ⚙️ Configuration

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user_db
    username: root
    password: ****

  jpa:
    hibernate:
      ddl-auto: update
```

---

# ▶️ Running the Project

```bash
mvn clean install
mvn spring-boot:run
```

---

# 🧪 Testing

* Use Swagger UI or Postman
* Check cookies in browser DevTools
* Verify Redis keys:

  * REFRESH:<email>
  * BLACKLIST:<token>

---

# 📌 Important Notes

* Refresh token is NOT returned in response body (security)
* Role is stored inside JWT payload
* User must re-login after role update
* Email verification is mandatory before seller application

---

# 🚀 Future Enhancements

* Seller KYC verification
* Email notifications (approval/rejection)
* API Gateway integration
* Microservices communication security
* OAuth2 / Social login

---

# 👨‍💻 Author

Developed as part of an **E-commerce Microservices Architecture** project.
