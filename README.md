# PipeForge

> A production-grade data pipeline orchestration platform for scheduling, executing, monitoring, and recovering distributed ETL workflows.

PipeForge lets engineering teams define pipelines as **DAGs**, schedule runs (manual / cron / API),
execute tasks on distributed workers, retry failures with exponential backoff, and observe everything
through metrics, logs, and execution history. It is a simplified, production-inspired analog of
Apache Airflow / Temporal / Prefect.

## Architecture

**Modular Monolith with Distributed Worker Execution.**

- **PostgreSQL** — source of truth (users, pipelines, DAG structure, execution history, logs, audit)
- **Redis** — runtime orchestration (ready queue, retry queue, distributed locks, rate limiting, hot metrics)
- **Workers** — claim jobs from Redis, lock them (`SETNX`+TTL), execute on Java virtual threads, persist state

```
pipeforge/
├── backend/        Spring Boot 3.x · Java 21 · Maven
├── frontend/       React · TypeScript · Tailwind · shadcn/ui
└── docker-compose.yml
```

### Backend modules (`com.pipeforge`)

`auth · pipeline · scheduler · execution · worker · metrics · config · common · exception`
— each following `controller / service / repository / dto / entity / mapper`.

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Java 21 (LTS, virtual threads) |
| Framework | Spring Boot 3.x (Web, Security, Data JPA, Validation, Actuator, Scheduling) |
| Build | Maven |
| Persistence | PostgreSQL + Hibernate/Spring Data JPA, Flyway migrations |
| Runtime store | Redis (queues, locks, rate limiting, metrics cache) |
| Scheduler | Quartz |
| Mapping | MapStruct |
| Auth | JWT (HS256, access + refresh) + BCrypt, RBAC (Admin / Engineer / Viewer) |
| API docs | springdoc-openapi (Swagger) |
| Observability | Spring Actuator + Micrometer + Prometheus, structured JSON logs (SLF4J + Logback) |
| Testing | JUnit 5, Mockito, Testcontainers |
| Frontend | React + TypeScript + Tailwind + shadcn/ui + Recharts + React Flow (dark theme, desktop-first) |

## Local Development

```bash
docker compose up
```

| Service | Port |
|---|---|
| pipeforge-app | 8080 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Prometheus | 9090 |
| pgAdmin | 5050 |
| RedisInsight | 5540 |

## Status

Under active development. Built milestone-by-milestone per the project's hardening addendum:
Project Setup → Core Infrastructure → Auth → Pipeline CRUD → DAG Engine → Execution Engine →
Scheduler + Retry → Worker Runtime → Observability → Frontend → Production Hardening.

## License

Proprietary — all rights reserved.
