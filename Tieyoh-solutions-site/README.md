# Tieyoh Solutions Site

Public information site and gated member/investor portal for **Tieyoh Solutions** (the investor-facing brand for the Tieyoh digital gifting platform).

Built with the same stack you're already running for the core Tieyoh product, so it reuses your existing Azure Postgres instance and Docker workflow instead of introducing a second stack:

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Views | Thymeleaf + Bootstrap 5 (server-rendered, fully responsive) |
| Database | PostgreSQL (your existing Azure instance, or local via Docker) |
| ORM / Migrations | Spring Data JPA + Flyway |
| Auth | Spring Security (form login, BCrypt, role-based) |
| Build | Maven |
| Containers | Docker / docker-compose |

There is no separate mobile app or React/Node build in this project — pages are server-rendered and responsive via Bootstrap's grid, so they work the same in a desktop browser or a phone browser. No Android emulator needed for this piece.

## What's included

**Public pages** (no login required): Home, About, Investors, Events, Publications, Contact, Login, Request Access (signup).

**Member area** (`/member/**`, requires an approved account): Dashboard, Documents (data room), Events (public + member-only), Publications (public + member-only), Profile.

**Admin area** (`/admin/**`, requires an admin account): approve/reject/invite members, upload documents (file or external link), create events, create publications.

Content on the About/Investors pages was pulled from `Tieyoh Mission_ValueProp_HighLevelFlow.pdf` — review and edit the copy in `src/main/resources/templates/about.html` and `investors.html` before this goes live.

## Access model

- Anyone can submit a request at `/auth/signup`. New accounts start as `PENDING_APPROVAL` and can't log in yet.
- An admin reviews pending requests at `/admin/members` and approves or rejects them.
- Admins can also invite someone directly — this creates an already-active account and shows a one-time temporary password to hand off manually (no email service is wired up yet, see below).
- A default admin account is created automatically on first boot from the `ADMIN_EMAIL` / `ADMIN_PASSWORD` environment variables. **Change that password immediately after your first login.**

## Running locally with Docker (recommended)

```bash
cd tieyoh-solutions-site
docker compose up --build
```

This starts a local Postgres container plus the app, runs Flyway migrations automatically, and creates the default admin account. Visit `http://localhost:8080`.

To point at your **existing Azure Postgres** instance instead of the local container, skip the `postgres` service and set the connection env vars before starting just the app:

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://<your-azure-host>:5432/tieyoh_solutions?sslmode=require"
export SPRING_DATASOURCE_USERNAME="<username>"
export SPRING_DATASOURCE_PASSWORD="<password>"
docker compose up --build app
```

(Copy `.env.example` to `.env` and fill it in if you'd rather not export vars manually — docker-compose reads `.env` automatically.)

Note: you'll need to create the `tieyoh_solutions` database on your Azure instance first (or point `SPRING_DATASOURCE_URL` at a database that already exists) — Flyway creates the tables, not the database itself.

## Running in VS Code without Docker

Prereqs: JDK 21, Maven (or use the included `mvnw` if you generate one), and the "Extension Pack for Java" + "Spring Boot Extension Pack" VS Code extensions.

```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/tieyoh_solutions?sslmode=require"
export SPRING_DATASOURCE_USERNAME="<username>"
export SPRING_DATASOURCE_PASSWORD="<password>"
mvn spring-boot:run
```

Or just hit F5 in VS Code once the Java extension has indexed the project — it will pick up `SolutionsApplication.java` as the entry point.

## Deploying to Azure

The project is a standard Spring Boot fat jar in a container, so it deploys the same way as your core Tieyoh service:

1. Build and push the image (`docker build -t <your-registry>/tieyoh-solutions-site:latest .`).
2. Deploy to **Azure Container Apps** (or App Service) pointed at that image.
3. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` as app settings / secrets (pull the DB credentials from **Azure Key Vault** the same way your core product does).
4. Mount a persistent volume (Azure Files) at `/app/uploads` if you want uploaded documents to survive container restarts — otherwise, prefer switching `DocumentService` to store files in Azure Blob Storage instead of local disk (see below).

If you already have Bicep templates for the core Tieyoh service, this app is a good candidate to reuse them for — it needs one container app, one Postgres database (can be a schema on the same server), and one Key Vault secret set.

## Things intentionally left as follow-ups

This is a working scaffold, not a finished product. Known gaps to close before go-live:

- **Email delivery** isn't wired up. Signup confirmations, approval notifications, and the Contact page form all need an email provider (SendGrid, Azure Communication Services, etc.) connected.
- **File storage** is local disk (`UPLOADS_DIR`) by default. Fine for a single container; move to Azure Blob Storage before scaling to multiple instances.
- **Password reset** isn't implemented.
- **Profile editing** is read-only in the UI; the service layer supports updates, the form doesn't exist yet.
- Placeholder copy (mission/investor content) should be reviewed against the latest version of your source materials before publishing.

## Project structure

```
src/main/java/com/tieyoh/solutions/
  config/        Security, default-admin seeding, global template attributes
  controller/    Public pages, auth, member area, admin area, file download
  domain/        JPA entities (User, DocumentAsset, EventItem, Publication)
  dto/           Form-binding objects
  repository/    Spring Data JPA repositories
  security/      UserDetails + auth success handler
  service/       Business logic

src/main/resources/
  templates/     Thymeleaf pages (public/, auth/, member/, admin/, fragments/)
  db/migration/  Flyway SQL migrations
  application.yml
```
