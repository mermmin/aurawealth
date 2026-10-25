# AuraWealth

A full-stack personal wealth and subscriptions dashboard. AuraWealth links bank accounts through the Plaid sandbox to track net worth, spending by category, and recurring subscriptions.

> **Status:** in progress. The backend auth and Plaid link-token flow are done; account syncing, the dashboard logic, and the React front end are next.

## Tech stack

- **Backend:** Java 17, Spring Boot 4.1, Spring Security, Maven
- **Database:** PostgreSQL 17, Flyway migrations
- **Auth:** stateless JWT (HS256), BCrypt password hashing
- **Integration:** Plaid API (sandbox)
- **Testing:** JUnit 5, Mockito, MockMvc, `@DataJpaTest` against a real Postgres database
- **Local setup:** Docker Compose

## What works today

| Endpoint | Description |
|---|---|
| `POST /api/auth/register` | Create an account and get a JWT |
| `POST /api/auth/login` | Log in and get a JWT |
| `GET /api/me` | Return the current user (needs a Bearer token) |
| `POST /api/plaid/link-token` | Create a Plaid sandbox link token (needs a Bearer token) |

Errors are returned as RFC 9457 problem JSON (400 validation, 401 bad credentials, 409 duplicate email, 502 Plaid failure).

## Run it locally

Requires Java 17+ and Docker Desktop.

```bash
cp .env.example .env          # add your Plaid sandbox keys
docker compose up -d          # start Postgres
./mvnw spring-boot:run        # app runs on http://localhost:8080
```

## Run the tests

```bash
docker compose up -d          # tests use the real database
./mvnw test
```

## Roadmap

- [x] Project skeleton, Postgres in Docker, Flyway
- [x] JWT authentication
- [ ] Plaid account and transaction sync
- [ ] Net worth, category spending, and subscription detection
- [ ] Figma mockups
- [ ] React + TypeScript front end
- [ ] CI with GitHub Actions and a live deployment
