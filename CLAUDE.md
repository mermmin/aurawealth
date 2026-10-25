# CLAUDE.md

## Start of every session
**Read [AuraWealth-PROJECT.md](AuraWealth-PROJECT.md) first.** It holds the scope, decisions log, milestones, current status, and the next micro-task.

## End of every session
Update "Current status", "Next micro-task", and "Session log" in AuraWealth-PROJECT.md, then commit all changes with a message like "Session N complete: <summary>" and push to `origin main`.

## Ground rules
- Stay inside v1 scope. Do not start v2 items until v1 is live.
- Walk the user through each step before running commands.

## Stack quick reference
- Backend: Java 17, Spring Boot 4.1, Maven (use `./mvnw`, Maven is not installed globally)
- Database: PostgreSQL, migrations in `src/main/resources/db/migration` (Flyway)
- Base package: `com.aurawealth`
