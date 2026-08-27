# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A workout planner/tracker Spring Boot backend. The domain feature that drives most design decisions is **automatic progressive overload**: an exercise is planned with a set count, a rep range (min/max) and a target weight; when every set of an exercise reaches the top of its rep range, the working weight for the next session is raised automatically (+2.5 kg upper body, +5 kg lower body). See `README.md` for the full feature set (guided set/rest timer mode, weekly calendar planning, planned roadmap items).

**What exists today: the exercise library only** — a public, shared catalog of movements with full CRUD, search and per-language names, at `/api/v1/exercises`. Session planning, the progressive-overload calculation itself, the guided timer mode and the calendar are all still unwritten, as is the frontend (React/TS + React Native, or Flutter — undecided).

`BodyPart` already carries the overload increment (`weightIncrementKg()`, 2.5 / 5.0), so the planning feature should read it from there rather than re-encode the rule.

## Commands

Use the wrapper (`./mvnw`), not a system `mvn`. `.mvn/wrapper/maven-wrapper.properties` is
required for it to work and was missing from the initial commit — do not let `.gitignore`
swallow it again.

```bash
./mvnw spring-boot:run                                    # run the app (port 8080)
./mvnw clean package                                      # build the executable jar
./mvnw test                                               # all tests
./mvnw test -Dtest=ExerciseServiceTest                    # single test class
./mvnw test -Dtest=SomeTest#someMethod                    # single test method
```

Docker must be running: `spring-boot:run` starts the PostgreSQL container in `docker-compose.yaml` itself, and the test suite starts a throwaway one via Testcontainers. `spring.docker.compose.lifecycle-management=start_only` leaves the database up when the app stops; `docker compose down` stops it.

Formatting is **Google Java Style**, enforced by Spotless with google-java-format:

```bash
./mvnw spotless:apply                                     # reformat all Java sources
./mvnw spotless:check                                     # report violations without fixing
```

`spotless:check` is bound to the `verify` phase, so `./mvnw clean verify` (and `package`)
fails on unformatted code while `./mvnw test` does not. Write new code 2-space indented at
100 columns, or just run `spotless:apply` before committing. There is no linter configured.

`.vscode/` formats on save with `tools/format-java.sh`, which runs the same
google-java-format jar at the version parsed out of `pom.xml`. **Do not** switch VS Code to
the built-in Eclipse formatter for this: Google's own Eclipse profile diverges from
google-java-format on roughly 590 lines across this codebase (argument wrapping, enum
constant layout), so it would fight `verify` on every save.

Once running: Swagger UI at `/swagger-ui.html`, OpenAPI spec at `/v3/api-docs`, health at `/actuator/health`.

## Conventions established by the exercise slice

Follow these when adding the next feature; they were chosen deliberately, not by default.

- **Feature packages**, not layers: `exercise/` holds its controller, service, repository, mapper, exceptions, plus `domain/` and `dto/`. `common/` and `config/` are cross-cutting.
- **Flyway owns the schema.** `spring.jpa.hibernate.ddl-auto=validate` — Hibernate only checks that the mappings agree. Every schema change is a new `V<n>__*.sql`; reference data (like the seeded catalog) belongs in a migration too.
- **Entities use Lombok; DTOs are records.** Auditing is `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`/`@LastModifiedDate`.
- **Translations are rows, never columns.** A translatable entity maps an `@ElementCollection` of an `@Embeddable`, keyed by language tag (`@MapKeyColumn(name = "locale")`), in its own `*_translation` table with PK `(<entity>_id, locale)`. Adding a language is config (`app.i18n.supported-locales`) plus rows — never a migration. Give the collection a `@BatchSize` or a paged list costs one extra SELECT per row.
- **Locale comes from `Accept-Language`**, resolved by the `localeResolver` bean in `config/LocaleConfig`. Controllers take a `Locale` parameter and pass it down; the mapper resolves requested → default (`en`) → any, and reports the winner as `resolvedLocale` so clients can see a fallback. The default language is required on write, enforced in the service.
- **Errors are RFC 9457** `ProblemDetail`, via `common/ApiExceptionHandler` (which extends `ResponseEntityExceptionHandler`, so framework-raised failures get the same shape).
- **Paged endpoints return `PageResponse<T>`**, not Spring's `Page` — its JSON shape is not a stable contract.
- **No security yet, by design.** Authentication and authorization will be added later via Keycloak; do not add ownership checks or `spring-boot-starter-security` without being asked.
- Mapping is hand-written. **MapStruct was deliberately not introduced** (see the Lombok note below).

## Stack notes

Boot 4 is new enough that recalled answers and most online examples are Boot 3 and will be wrong here. Each of the following cost a failing build once already.

- **Java 25 / Spring Boot 4.1.1.** Starters were renamed and split: `spring-boot-starter-webmvc` (not `-web`), and test starters are per-module — `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`. `spring-boot-starter-validation` is separate; without it `@Valid` silently does nothing.
- **Jackson 3.** The `ObjectMapper` bean is `tools.jackson.databind.ObjectMapper`. A `com.fasterxml.jackson` copy is on the classpath as a springdoc transitive but has no bean — importing that one fails at injection time.
- **Test annotations moved** to `org.springframework.boot.webmvc.test.autoconfigure.{WebMvcTest, AutoConfigureMockMvc}`. `@MockBean` is gone; use `@MockitoBean`.
- **Testcontainers 2.0.5** renamed its modules: `org.testcontainers:testcontainers-postgresql` and `testcontainers-junit-jupiter` (the bare `postgresql` / `junit-jupiter` artifactIds no longer resolve), and the container class is `org.testcontainers.postgresql.PostgreSQLContainer`. Versions come from the parent BOM — do not pin them.
- **Flyway 12** ships database support separately: `spring-boot-starter-flyway` brings only the core, so `org.flywaydb:flyway-database-postgresql` is a required runtime dependency.
- **PostgreSQL 18** moved `PGDATA` to `/var/lib/postgresql/<major>/docker`. `docker-compose.yaml` mounts the volume at `/var/lib/postgresql` for that reason; the pre-18 `/var/lib/postgresql/data` path persists nothing and lets a stale anonymous volume shadow it.
- **Lombok** is wired through explicit `annotationProcessorPaths` in the `maven-compiler-plugin` config, not just as a dependency. A new annotation processor must be registered in *both* the `default-compile` and `default-testCompile` executions or it will silently not run — which is why mapping is hand-written rather than MapStruct.

## Gotchas

- **Null parameters in JPQL need a cast.** An unfiltered `ExerciseRepository.search` binds `:search` as an untyped null, which PostgreSQL types as `bytea` — and there is no `lower(bytea)`. Hence `cast(:search as String)`. Expect the same for any optional string filter.
- **Request DTOs reject unknown fields** (`spring.jackson.deserialization.fail-on-unknown-properties=true`). The read model exposes a top-level localized `name`/`description` that the write model does not accept, so without this, editing a GET response and PUTting it back returns 200 having changed nothing. `ApiExceptionHandler` names the offending field.
- **An `@ElementCollection` change does not dirty the owning row**, so `@PreUpdate` — and with it `@LastModifiedDate` — does not fire on a translation-only edit. `ExerciseService.update` sets `updatedAt` explicitly. A `@Version` column would fix this more cleanly and add lost-update protection; worth doing before the catalog gets concurrent editors.
- **Assert persistence by reading back**, not on the response body of the write itself — and compare timestamps that both came from PostgreSQL, since it rounds to microseconds and can round an in-memory `Instant` *upward*.
- **Search across a translation collection uses `EXISTS`, not a join**, so an entity translated into several languages still counts once and Spring Data's derived count query stays correct without `DISTINCT`.
- Integration tests share one container through `support/PostgresTestContainerConfig` (registered as a bean so the Spring context cache keeps it alive across classes). They run against the seeded catalog and are **not** transactional, so any test that writes must clean up after itself.
