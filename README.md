## 🎸 Bandage: Band Practice Management Service
### 📝 프로젝트 개요 (Overview)
밴드 합주 일정 관리 및 셋리스트 협업을 위한 B2B2C 솔루션. 
합주곡 선정, 세션 할당, 일정 확정 및 외부 캘린더 연동을 자동화하여 밴드 운영의 효율성을 극대화.

A B2B2C solution for band practice scheduling and setlist collaboration. 
It maximizes band operational efficiency by automating song selection, session assignment, schedule confirmation, and external calendar integration.

### 🛠 기술 스택 (Tech Stack)
**Backend & Language**

- Language: Kotlin 1.9 (JVM 21)
- Framework: Spring Boot 3.2+
- Database: PostgreSQL (Main), Redis (Cache/Session)
- Communication: gRPC, OpenFeign, REST API

**Infrastructure & DevOps**

[//]: # (- Cloud: AWS &#40;EC2, S3, ECS Fargate, ELB&#41;)

[//]: # (- Networking: WAF, Global Accelerator)

- CI/CD: Docker, GitHub CI
- Monitoring: Prometheus, Grafana

### 🏗 아키텍처 및 기술 (Architectural Challenges)
**모듈러 모놀리스 (Modular Monolith)**
- Spring Modulith를 적용하여 도메인 간 결합도를 낮추고 독립적인 모듈 구조를 유지함. 향후 트래픽 증가 시 MSA(Microservices Architecture)로의 원활한 전환을 고려한 설계. 
- Applied Spring Modulith to reduce coupling between domains and maintain independent module structures. Designed for a seamless transition to Microservices Architecture (MSA) as traffic scales.

**이벤트 기반 설계 (Event-Driven Architecture)**
- 도메인 이벤트를 활용하여 비즈니스 로직을 분리함. 합주 확정 시 알림 발송 및 캘린더 동기화 등 부가 기능을 비동기적으로 처리하여 시스템 성능 최적화.
- Decoupled business logic using domain events. Optimized system performance by asynchronously processing auxiliary functions such as notifications and calendar synchronization upon practice confirmation.

**코틀린 코루틴 (Kotlin Coroutines)**
- Spring MVC 환경에서 Coroutines와 Java 21의 Virtual Threads를 조합하여 논블로킹(Non-blocking) 비동기 애플리케이션 구현.
- Implemented a non-blocking asynchronous application by combining Coroutines with Java 21 Virtual Threads in a Spring MVC environment.

### 📂 도메인 모델 (Domain Models)
- Member: 사용자 인증 및 프로필 관리 (Auth & Profile)

- Band: 밴드 조직, 권한 및 멤버 관리 (Organization & Permissions)

- Practice: 합주 일정, 곡 선정 및 세션 할당 (Scheduling & Sessions)

- Performance: 공연 셋리스트 구성 및 외부 공유 (Setlist & Public Sharing)

### 🚀 시작하기 (Getting Started)
**Prerequisites**
- JDK 21
- PostgreSQL 15+
- Docker

**Installation**
```bash
# Repository Clone
git clone https://github.com/swjung11/bandage.git

# Environment Setup (PostgreSQL)
psql -U postgres -f setup.sql

# Build & Run
./gradlew bootRun
```