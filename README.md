# {{AMUGEONA}} — Second-hand Marketplace (Spring Boot + MySQL 8)

[![Java](https://img.shields.io/badge/Java-21-007396)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F)]()
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)]()
[![License](https://img.shields.io/badge/License-MIT-blue)]()

A production-like second-hand marketplace focusing on **secure authentication (OAuth2)**, clean **MySQL schema**, and pragmatic **performance tuning**.

---

## ✨ Features

- Users / Listings / Offers / Chat / Reviews / Orders / Shop / Report
- **Auth**: Spring Security, **OAuth2 (Google/Kakao/Naver)**, CSRF
- **DB**: MySQL 8 (InnoDB, utf8mb4), normalized schema, composite indexes
- **Ops**: Seed SQL, simple load test notes, error-handling & logging
- **Docs**: Architecture, Security flows, API examples

---

## 🏗 Architecture
Client (HTML/CSS/JS) \
↓ REST/JSON \
Spring Boot (Security, OAuth2, TOTP, Services) \
↓ JPA/MyBatis \
MySQL 8 (InnoDB, utf8mb4, indexes)

![Architecture Diagram](./docs/architecture.png) <!-- 이미지가 없으면 주석 처리하거나 나중에 추가 -->

---

## 📦 Tech Stack

- **Backend**: Java 21, Spring Boot 3.x, Spring Security, JPA, MyBatis
- **DB**: MySQL 8 (InnoDB, utf8mb4)
- **Auth**: OAuth2 (Google/Kakao/Naver), TOTP (MFA), CSRF
- **Build/Dev**: Gradle, IntelliJ/STS, Docker (optional)

---

## 📂 Project Structure (excerpt)
src/ \
├─ main/ \
│ ├─ java/amgn/amu/... \
│ │ ├─ controller/ # REST controllers \
│ │ ├─ service/ # business logic \
│ │ ├─ repository/ # JPA/MyBatis repositories \
│ │ ├─ config/ # Security config, handlers, filters, Web config, PaymentConfig \
│ │ └─ dto/entity/... \
│ └─ resources/ \
│ ├─ application.yml \
│ ├─ mapper/ # MyBatis mappers (if any) \ 
│ └─ static/ # HTML/CSS/JS (if served) \
└─ test/... 

---

## ⚙️ Setup

### 1) Requirements
- Java 21, Gradle, MySQL 8.0.x  
- (Optional) Docker & Docker Compose

### 2) Environment

`.env.example` (필요 시 프로젝트 루트에)
DB_HOST=localhost
DB_PORT=3306
DB_NAME={{amugeona}}
DB_USER={{root}}
DB_PASS={{your-password}}
OAUTH_GOOGLE_CLIENT_ID=...
OAUTH_GOOGLE_CLIENT_SECRET=...
OAUTH_KAKAO_CLIENT_ID=...
OAUTH_NAVER_CLIENT_ID=...

`src/main/resources/application.properties`
```
spring.application.name=PJ_AMUGEONA

# DB
spring.datasource.url=jdbc:mysql://localhost:3306/amugeona?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=123456


# MyBatis
mybatis.type-aliases-package=amgn.amu.domain
mybatis.mapper-locations=classpath:/mapper/*.xml
mybatis.configuration.map-underscore-to-camel-case=true
mybatis.configuration.default-fetch-size=100
mybatis.configuration.default-statement-timeout=30

# Kakao Local API
KAKAO_REST_API_KEY=baaa3a6a76db4d9142c1e2617f531915

logging.level.org.springframework.security=DEBUG
logging.level.org.springframework.security.authentication.ProviderManager=DEBUG
logging.level.org.springframework.security.authentication.dao.DaoAuthenticationProvider=DEBUG
logging.level.org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder=TRACE
logging.level.org.springframework.security.oauth2=DEBUG

logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

logging.level.org.mybatis=DEBUG
logging.level.java.sql=DEBUG
logging.level.jdbc.sqlonly=DEBUG

server.error.include-message=always
server.error.include-stacktrace=always
server.error.include-binding-errors=ALWAYS

logging.level.amgn.amu=DEBUG
logging.level.org.springframework.jdbc.core=DEBUG
logging.level.org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver=TRACE

spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=300MB

kakao.rest.key=6f22b80447108cd2cb082746fa4d29b3

spring.security.oauth2.client.registration.google.client-id=71157529807-n3hp5sscmcia27sq7eiigoh2vb8q1rq4.apps.googleusercontent.com
spring.security.oauth2.client.registration.google.client-secret=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
spring.security.oauth2.client.registration.google.scope=openid,email,profile
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/login/oauth2/code/google

spring.security.oauth2.client.registration.kakao.client-id=baaa3a6a76db4d9142c1e2617f531915
spring.security.oauth2.client.registration.kakao.client-authentication-method=client_secret_post
spring.security.oauth2.client.registration.kakao.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.kakao.redirect-uri={baseUrl}/login/oauth2/code/kakao
spring.security.oauth2.client.registration.kakao.scope=profile_nickname,account_email
spring.security.oauth2.client.provider.kakao.authorization-uri=https://kauth.kakao.com/oauth/authorize
spring.security.oauth2.client.provider.kakao.token-uri=https://kauth.kakao.com/oauth/token
spring.security.oauth2.client.provider.kakao.user-info-uri=https://kapi.kakao.com/v2/user/me
spring.security.oauth2.client.provider.kakao.user-name-attribute=id

spring.security.oauth2.client.registration.naver.client-id=tV1grnIR3Vmzc_Oj6n3t
spring.security.oauth2.client.registration.naver.client-secret=1ouYUT09lL
spring.security.oauth2.client.registration.naver.client-authentication-method=client_secret_post
spring.security.oauth2.client.registration.naver.authorization-grant-type=authorization_code
spring.security.oauth2.client.registration.naver.redirect-uri={baseUrl}/login/oauth2/code/naver
spring.security.oauth2.client.registration.naver.scope=name,email
spring.security.oauth2.client.provider.naver.authorization-uri=https://nid.naver.com/oauth2.0/authorize
spring.security.oauth2.client.provider.naver.token-uri=https://nid.naver.com/oauth2.0/token
spring.security.oauth2.client.provider.naver.user-info-uri=https://openapi.naver.com/v1/nid/me
spring.security.oauth2.client.provider.naver.user-name-attribute=response

# v2 API
imp.key=store-5a5febb7-cd4d-4dba-80e1-45137984ce13
imp.secret=4jn1G2WglgCMMEuJCiej1oYXutvESuWBK4OSUvQVnz1h2SdEGzAGIsmbYHK2ASSSqttW9egIgaglndMp

kakao.admin-key=YOUR_TEST_ADMIN_KEY
```
### 3) database
mysql -h localhost -P 3306 -u root -p --default-character-set=utf8mb4 \
  {{amugeona}} < ./db/schema.sql

mysql -h localhost -P 3306 -u root -p --default-character-set=utf8mb4 \
  {{amugeona}} < ./db/seed.sql

## ▶️ Run
development mode:
./gradlew bootRun

packaging:
./gradlew clean build
java -jar build/libs/*.jar

## 🔐 Security Notes

CSRF: 쿠키의 XSRF-TOKEN 값을 요청 헤더 X-XSRF-TOKEN로 전송

OAuth2: Google/Kakao/Naver 지원 (리다이렉트 URI 환경별 분리)

## 📊 Metrics (to be updated) <!-- 갱신 필요 -->

p95 DB query latency: {{Y}} ms

Auth error rate: {{X}} % → Z % 개선

Page load time (listings): {{N}} ms

## 요약
Spring Security 기반 인증(소셜 로그인),
MySQL 8 스키마/인덱스 최적화,
간단한 부하 측정 결과 등을 포함한 중고거래 플랫폼 예제입니다.
