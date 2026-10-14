# reminder-service

Java / Spring Boot backend that will send email reminders (for example "follow up on this
application today") for the Job Application CRM Chrome extension.

It is a **separate, optional service**. The extension keeps working exactly as it does today
without it, and `chrome.storage.local` stays the source of truth for job data. The service
will only ever store minimal reminder records (job id, title, company, URL, follow-up date).

## Status: Part 1 of 8 - foundation only

| Part | Scope | State |
|---|---|---|
| 1 | Project foundation, config, health endpoint, error format | **this drop** |
| 2 | PostgreSQL + Flyway, users/reminders tables | not started |
| 3 | Reminder API | not started |
| 4 | Scheduler / dispatcher | not started |
| 5 | Email delivery (Mailpit in dev) | not started |
| 6 | Email OTP + JWT authentication | not started |
| 7 | Chrome extension integration | not started |
| 8 | Production hosting + real email provider | not started |

What exists now: a runnable service with `GET /actuator/health`, typed and validated
configuration, a uniform JSON error format, and tests. **There is no database, no auth, and
no reminder logic yet.** Do not deploy it publicly before Part 6.

## Prerequisites

- JDK 21 (17 or newer works)
- Maven 3.9 or newer
- Check with `java -version` and `mvn -version`

## Run (development)

PowerShell (Windows) - the quotes around `-D...` are required:

```powershell
cd backend\reminder-service
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

macOS / Linux:

```bash
cd backend/reminder-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Check it is up (in PowerShell use `curl.exe`, because plain `curl` is an alias for
`Invoke-WebRequest`):

```powershell
curl.exe http://localhost:8080/actuator/health
```

Expected: `{"status":"UP"}`

## Test and build

```
mvn test       # run the tests
mvn verify     # tests + build target/reminder-service-0.1.0-SNAPSHOT.jar
```

Run the built jar:

```
java -jar target/reminder-service-0.1.0-SNAPSHOT.jar
```

## Profiles

| Profile | How to enable | Purpose |
|---|---|---|
| *(none)* | default | Production-safe defaults. No developer-only features. |
| `dev` | `SPRING_PROFILES_ACTIVE=dev` | Local development: debug logging, localhost CORS origin. |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | Deployment. Everything environment-specific comes from environment variables. |
| `test` | set by the tests | Test settings. |

Developer-only features added later (such as the development login shortcut in Part 3)
will be available **only** under `dev`/`test` and will refuse to start under any other profile.

## Configuration

Spring maps environment variables to settings automatically.

| Environment variable | Meaning | Default |
|---|---|---|
| `PORT` | HTTP port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile(s) | none |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API, e.g. `chrome-extension://<id>`. Format: `https://host[:port]` or `chrome-extension://<id>`, no path, no trailing slash. | empty (no cross-origin access) |

An invalid value stops the service at startup with a clear error rather than failing later.
Later parts add database, mail and JWT settings the same way.

Spring Boot does **not** read `.env` files by itself. Set variables in your shell or your
host's secret store.

### Secrets

Never commit credentials. This repository contains no passwords, keys or tokens. `.gitignore`
excludes `.env*`, `application-local.*`, and key/certificate files. Real values (database
password, SMTP credentials, JWT signing secret) are supplied through environment variables.

## Error format

Every error uses the same envelope:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "Request validation failed.", "fields": { "name": "must not be blank" } } }
```

`fields` appears only for validation errors. Unexpected errors return a generic
`INTERNAL_ERROR` and are logged server-side; internals are never sent to the client.

## Project layout

```
backend/reminder-service/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/jobcrm/reminders/
    │   │   ├── ReminderServiceApplication.java
    │   │   ├── config/AppProperties.java
    │   │   └── common/ApiError.java, GlobalExceptionHandler.java
    │   └── resources/application.yml, application-dev.yml, application-prod.yml
    └── test/
        ├── java/com/jobcrm/reminders/
        │   ├── HealthEndpointTest.java
        │   ├── config/AppPropertiesTest.java
        │   └── common/GlobalExceptionHandlerTest.java
        └── resources/application-test.yml
```

## Notes

- Spring Boot is pinned in `pom.xml` (currently 4.1.0). Before release, move to the latest 4.1.x
  patch: `mvn versions:update-parent -DallowSnapshots=false`, or check start.spring.io.
- No Maven wrapper is included. If you want `mvnw`, run `mvn wrapper:wrapper` once and commit
  the generated files.
- Only the `health` actuator endpoint is exposed, with no component details.
