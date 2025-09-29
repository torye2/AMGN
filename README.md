# {{AMUGEONA}} — Second-hand Marketplace (Spring Boot + MySQL 8)

[![Java](https://img.shields.io/badge/Java-21-007396)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F)]()
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)]()
[![License](https://img.shields.io/badge/License-MIT-blue)]()

A production-like second-hand marketplace focusing on **secure authentication (OAuth2 + MFA/TOTP)**, clean **MySQL schema**, and pragmatic **performance tuning**.

---

## ✨ Features

- Users / Listings / Offers / Chat / Reviews
- **Auth**: Spring Security, **OAuth2 (Google/Kakao/Naver)**, **MFA (TOTP)**, CSRF
- **DB**: MySQL 8 (InnoDB, utf8mb4), normalized schema, composite indexes
- **Ops**: Seed SQL, simple load test notes, error-handling & logging
- **Docs**: Architecture, Security flows, API examples

---

## 🏗 Architecture
Client (HTML/CSS/JS)
↓ REST/JSON
Spring Boot (Security, OAuth2, TOTP, Services)
↓ JPA/MyBatis
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
src/
├─ main/
│ ├─ java/amgn/amu/...
│ │ ├─ controller/ # REST co약
Spring Security 기반 인증(소셜 로그인),
MySQL 8 스키마/인덱스 최적화,
간단한 부하 측정 결과 등을 포함한 중고거래 플랫폼 예제입니다.
