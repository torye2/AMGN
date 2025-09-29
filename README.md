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

![Architecture Diagram](./docs/architecture.png) <!-- 나중에 추가 -->

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

`.env.example`
DB_HOST=localhost \
DB_PORT=3306 \
DB_NAME={{amugeona}} \
DB_USER={{root}} \
DB_PASS={{password}} \
OAUTH_GOOGLE_CLIENT_ID=... \
OAUTH_GOOGLE_CLIENT_SECRET=... \
OAUTH_KAKAO_CLIENT_ID=... \
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

HTTP p95 ~74ms · Error 0.00% · Login 100% (k6 5VU×1m, local)
List/Detail p95 ~15.6ms

## 요약
Spring Security 기반 인증(소셜 로그인),
MySQL 8 스키마/인덱스 최적화,
간단한 부하 측정 결과 등을 포함한 중고거래 플랫폼입니다.
