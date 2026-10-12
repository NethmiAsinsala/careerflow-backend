# CareerFlow backend

Spring Boot 3.5.5 / Java 21+ / MySQL 8 / Flyway / JWT / local Ollama.

## Implemented modules

- Registration, login, current user, BCrypt passwords, bearer JWT, persistent logout token revocation.
- Candidate profiles, skills, education, experience, projects, certifications, and PDF resumes.
- Employer/company management and job creation, editing, closing, searching, filtering, sorting, and pagination.
- Applications, guarded status transitions, saved jobs, deadline checks, and ownership authorization.
- Deterministic job match percentages, missing skills, and ranked job recommendations.
- Local AI resume analysis with PDF extraction, structured feedback validation, concurrency limits, and timeouts.
- Candidate/employer/admin dashboards, user activation/deactivation, reports and admin review.
- Frontend CORS configuration, optional admin bootstrap, production configuration, Docker Compose, and GitHub Actions tests.

Forgot-password email delivery is marked future work in CAREERFLOW.pdf and is not implemented. The Angular frontend is separate. Deployment files are supplied; no public deployment is performed.

## Local run

1. Install Java 21+, Maven, and MySQL 8. Create `careerflow_db` if starting fresh.
2. Database settings use DB_URL, DB_USERNAME, and DB_PASSWORD. This workspace's existing settings were preserved in ignored `config/local.properties`. That file is loaded relative to the working directory; run Main from the project root. A fresh clone must supply its own DB settings.
3. Set JWT_SECRET in the IntelliJ Main run configuration. It must be Base64 for at least 32 random bytes. Existing configuration can stay unchanged. Example generation in PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
```

4. Reload Maven in IntelliJ, then run Main. Flyway applies migrations V1–V10 automatically. Do not edit already-applied migration files.
5. Open http://localhost:8080/swagger-ui/index.html. Register a JOB_SEEKER or EMPLOYER, log in, and authorize with its access token.
6. For local AI, start Ollama and run `ollama pull qwen2.5:1.5b`. See [AI setup](docs/AI_RESUME.md).

Do not commit `.env`, `config/local.properties`, API credentials, or IntelliJ workspace secrets. If old database credentials were committed previously, removing them from the current file does not erase Git history; rotate them before sharing that history.

## Admin bootstrap

Public registration cannot create admins. For the first admin, temporarily set these environment variables in the Main run configuration:

```text
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_EMAIL=your-admin-email
ADMIN_PASSWORD=your-unique-long-password
```

Use a valid email and a password of at least 12 characters and at most 72 UTF-8 bytes. After one successful startup, remove these variables or set ADMIN_BOOTSTRAP_ENABLED=false. Bootstrap never promotes an existing non-admin account or resets an existing admin password. Admin accounts cannot be disabled through the status endpoint.

## Newly added API routes

| Method | Route | Access / purpose |
| --- | --- | --- |
| POST | /api/auth/logout | Revoke the presented token; delete it in the frontend too |
| GET/POST | /api/job-seekers/me/certifications | Candidate list/create |
| PUT/DELETE | /api/job-seekers/me/certifications/{id} | Candidate-owned item |
| GET | /api/job-seekers/{jobSeekerId}/certifications | Owner, admin, or applicant's employer |
| GET | /api/job-seekers/me/recommendations?limit=20 | Candidate; limit 1–50 |
| POST | /api/ai/resume/analyze | Candidate's uploaded PDF; no body |
| POST | /api/reports | Authenticated user reports JOB, COMPANY, or USER |
| GET | /api/reports/me | Own reports, paginated |
| GET | /api/admin/reports | Admin report queue, paginated |
| PATCH | /api/admin/reports/{id} | Admin resolves OPEN report |

Swagger and [the exported OpenAPI file](docs/openapi.json) describe all existing routes and schemas.

Certification example:

```json
{"name":"Java Certification","issuer":"Training Institute","issuedOn":"2025-01-01","expiresOn":"2028-01-01","credentialUrl":"https://example.com/credential"}
```

Report example:

```json
{"targetType":"JOB","targetId":1,"reason":"The advertised company information appears incorrect"}
```

Resolve a report:

```json
{"status":"REVIEWED","resolution":"Checked the listing and contacted the employer"}
```

REVIEWED and DISMISSED are terminal report states. Review records the admin's decision; it does not automatically delete jobs or deactivate accounts. Use the existing moderation endpoints for those actions.

Recommendations consider all currently available, unapplied jobs from active employers, processing them in batches to bound memory use. They rank positive exact skill matches by percentage, then descending job ID. A candidate without profile skills receives an empty result and guidance to add skills. These are skill recommendations, not hiring predictions.

## Testing

```powershell
$env:CAREERFLOW_DB_TESTS='true'
mvn verify
```

Run from the project root with a MySQL test database configured through DB_URL, DB_USERNAME, and DB_PASSWORD. Integration tests roll back their fixture records; migrations remain applied. Use a dedicated database in CI. The supplied GitHub Actions workflow creates one.

The live model test is deliberately opt-in:

```powershell
$env:CAREERFLOW_AI_LIVE_TESTS='true'
mvn '-Dtest=OllamaLiveTest' test
```

The live test uses a synthetic resume, never a user's uploaded file. Regular model-client tests use a local fake HTTP server and test failure responses and timeout behavior.

## Docker / deployment

1. Copy `.env.example` to `.env`, replace the password/JWT placeholders, and set the frontend origin.
2. Start Docker Desktop, then run `docker compose up --build -d`.
3. The API is published on host loopback port 8080; MySQL has no published host port. Configure an HTTPS reverse proxy for a public deployment.
4. Swagger is off under the prod profile unless SWAGGER_ENABLED=true.
5. Optional local AI sidecar: `docker compose --profile ai up -d`, then `docker compose --profile ai exec ollama ollama pull qwen2.5:1.5b`. Ollama shares the API network namespace so the API can reach loopback without exposing Ollama publicly. Host Ollama models are separate from the container's volume.
6. Back up the mysql-data volume, which includes stored resumes. Do not run `docker compose down -v` unless you intend to erase the database.

The production profile requires external DB and JWT settings, disables SQL logging and Open Session in View, and keeps error stack traces out of HTTP responses. The container runs as a non-root user. The Docker configuration was validated syntactically; an engine-backed build/run still needs Docker Desktop running.

## Operational limits

Resume upload accepts at most 5 MiB and basic PDF markers; analysis performs full text extraction and rejects encrypted/image-only documents, more than 20 pages, or over 30,000 extracted characters. No OCR or malware scanning is included. AI feedback is not stored or applied automatically. One analysis runs per API instance; default model timeout is 120 seconds. Logout revokes one token, not every device. Revoked tokens are retained until expired and removed during subsequent logouts. Suspending an account preserves its data; reactivation restores unexpired, non-revoked tokens.
