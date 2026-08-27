# WORKOUT-TRACKER

> A workout planner and tracker with automatic progressive overload, built as a Spring Boot learning project.

workout-tracker is a personal project I'm building to plan my gym sessions and track my progress. Beyond simple logging, its core idea is **automatic progressive overload**: when I hit my target reps across all sets of an exercise, the app raises the working weight for the next session on its own.

## Tech Stack

**Backend**
- Java 25
- Spring Boot 4.1 — Spring Web MVC, Spring Data JPA, Spring Security, Actuator
- PostgreSQL
- springdoc-openapi (Swagger UI)
- Maven, Lombok

**Frontend** (planned)

A web client and a mobile client are planned. The two directions under consideration are React + TypeScript (web) with React Native / Expo (mobile), or Flutter for a single cross-platform codebase.

## Features

### Progressive overload (core)

Plan each exercise with a number of sets, a repetition range (minimum and maximum), and a target weight. When every set of an exercise reaches the upper end of its rep range, the app automatically increases the load for the next session:

- **+2.5 kg** for upper-body exercises
- **+5 kg** for lower-body exercises

### Guided exercise mode

Starting an exercise launches a timed, set-by-set session:

1. The set timer starts — I perform my reps.
2. I tap **"Next"** when the set is done, which starts the rest timer.
3. When the rest timer ends, the next set starts automatically.
4. The exercise completes once all programmed sets are done.

### Weekly planning

Plan sessions across the week in a calendar view. When planning a session, I pick exercises from a catalog and define, for each one, the number of sets, the repetition range (minimum and maximum), and the target weights. A planned session can be set to repeat every week to build a recurring routine, and calendar reminders help me not miss a session.

## Roadmap

- [ ] **Shuffle** — reorder the week's sessions to avoid the body adapting to a fixed schedule, always keeping an alternating upper/lower pattern (never two upper-body or two lower-body sessions back to back).
- [ ] **Muscle map** — a visual of the muscles worked, shown per session or per exercise.
- [ ] **Exercise demos** — an explanatory video or GIF showing each movement and correct form.

## Getting Started

### Prerequisites

- JDK 25 (make sure `javac` is available, not just `java`)
- Docker + Docker Compose (used to run PostgreSQL locally)

Maven itself is not needed — the wrapper (`./mvnw`) fetches the right version.

### Run locally

```bash
git clone https://github.com/mgll/workout-tracker.git
cd workout-tracker
./mvnw spring-boot:run
```

The application starts the PostgreSQL container defined in `docker-compose.yaml` itself and
applies the Flyway migrations, which seed a starter exercise library. The database is
left running when the application stops, so its data survives a restart; use
`docker compose down` to stop it.

```bash
./mvnw test             # unit tests, plus integration tests against a throwaway PostgreSQL
./mvnw spotless:apply   # reformat to Google Java Style
./mvnw clean verify     # tests + formatting check
./mvnw clean package    # build the executable jar
```

### Languages

Exercise names and descriptions are stored per language. Responses follow the
`Accept-Language` header and fall back to English when a language is missing:

```bash
curl -H 'Accept-Language: fr' localhost:8080/api/v1/exercises
```

Each response reports the language it actually used in `resolvedLocale`. The set of
accepted languages is `app.i18n.supported-locales` in `application.properties` — adding
one is a configuration change plus new rows, never a schema migration.

### API documentation

Once the app is running:

- Swagger UI — http://localhost:8080/swagger-ui.html
- OpenAPI spec — http://localhost:8080/v3/api-docs
- Health check — http://localhost:8080/actuator/health

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.