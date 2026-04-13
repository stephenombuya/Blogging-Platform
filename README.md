#  Blogging Platform — Production-Ready Back-End

A fully-featured, production-ready RESTful blogging platform built with **Java 17**, **Spring Boot 3**, **MySQL 8**, **Redis**, and **JWT authentication**.

<div>

  <!-- Repository Analytics -->
![GitHub repo size](https://img.shields.io/github/repo-size/stephenombuya/Blogging-Platform)
![GitHub language count](https://img.shields.io/github/languages/count/stephenombuya/Blogging-Platform)
![GitHub top language](https://img.shields.io/github/languages/top/stephenombuya/Blogging-Platform)
![GitHub last commit](https://img.shields.io/github/last-commit/stephenombuya/Blogging-Platform)
![GitHub contributors](https://img.shields.io/github/contributors/stephenombuya/Blogging-Platform)

</div>

##  Architecture

```
com.blogplatform
├── controller/          # REST controllers — HTTP layer
│   ├── AuthController       # /auth/** — register, login, tokens
│   ├── PostController        # /posts/** — CRUD, search, filter
│   ├── CommentController     # /posts/{id}/comments, /comments/**
│   ├── CategoryController    # /categories/**
│   ├── TagController         # /tags/**
│   ├── LikeController        # /posts/{id}/reactions, /comments/{id}/reactions
│   ├── UserController        # /users/**
│   └── AdminController       # /admin/** — dashboard, moderation
│
├── service/             # Business logic layer
│   ├── AuthService          # Registration, login, tokens, password
│   ├── PostService          # Post CRUD, publishing, slugging
│   ├── CommentService       # Threaded comments, moderation
│   ├── CategoryService      # Category management (Redis-cached)
│   ├── TagService           # Tag find-or-create, autocomplete
│   ├── LikeService          # Polymorphic like/dislike reactions
│   ├── UserService          # Profile management
│   └── AdminService         # Dashboard stats, token cleanup
│
├── repository/          # Spring Data JPA repositories
│   └── *Repository          # @EntityGraph on all list queries (no N+1)
│
├── model/               # JPA entity classes
│   ├── Post                 # @SQLDelete + @SQLRestriction (soft delete)
│   ├── Comment              # @SQLDelete + @SQLRestriction (soft delete)
│   └── ...                  # User, Category, Tag, Like, tokens
│
├── dto/                 # Request/Response transfer objects
├── config/              # Security, JWT filter, OpenAPI, CORS, Redis
│   └── RateLimitFilter      # Bucket4j — 10 req/min per IP on /auth/**
├── exception/           # Custom exceptions + GlobalExceptionHandler
└── util/                # JwtUtil (+ Redis blacklist), SlugUtil, EmailService, SecurityUtil
```

---

##  Features

| Feature | Details |
|---|---|
| **Authentication** | JWT access tokens + refresh tokens, email verification |
| **Authorization** | Role-based: `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN` |
| **Token Blacklist** | Redis-backed — logged-out access tokens are immediately invalidated |
| **Rate Limiting** | Bucket4j — 10 requests/minute per IP on all `/auth/**` endpoints |
| **Post Management** | CRUD, slugs, DRAFT / PUBLISHED / ARCHIVED statuses |
| **Soft Deletes** | Posts and comments use `deleted_at` — recoverable via admin restore |
| **Categories** | Admin-managed, Redis-cached, slug-based routing |
| **Tags** | Auto-created on post save, full autocomplete search |
| **Comments** | Threaded (parent/reply), moderation approval flow |
| **Reactions** | Like/dislike on both posts and comments (polymorphic) |
| **Admin Dashboard** | Stats, user management, content moderation, restore deleted content |
| **Email** | Async HTML emails: verification, password reset, welcome |
| **Search** | Full-text search across post title/content |
| **Pagination** | All list endpoints paginated via `page` + `size` |
| **N+1 Prevention** | `@EntityGraph` on all collection-loading repository queries |
| **API Docs** | Swagger UI at `/swagger-ui.html` |
| **DB Migrations** | Flyway — versioned, repeatable migrations |
| **Security** | BCrypt-12 passwords, stateless JWT, CORS configured |
| **Scheduled Jobs** | Nightly expired-token cleanup |

---

##  Database Schema

```
users ──────────────────────── posts  (deleted_at → soft delete)
  │  (1:N author)               │  (N:1 category) ── categories
  │                             │  (N:M tags)      ── tags
  │                             │  (1:N)
  └──────────────────── comments  (deleted_at → soft delete)
                           │  (self-join parent/reply)
                           └── likes (polymorphic: POST | COMMENT)
```

### Migrations

| File | Description |
|---|---|
| `V1__Initial_Schema.sql` | Full schema, indexes, constraints, seed data |
| `V2__Soft_Deletes.sql` | Adds `deleted_at` to `posts` and `comments` |

---

## 🚀 Quick Start

### Prerequisites

- Java 17+
- MySQL 8+
- Redis 7+
- Maven 3.9+

### Local Setup

```bash
# 1. Clone
git clone https://github.com/stephenombuya/Blogging-Platform
cd blogging-platform

# 2. Configure
cp .env.example .env
# Edit .env with your DB credentials, JWT secret, mail settings

# 3. Build
mvn clean install

# 4. Run
mvn spring-boot:run
```

### Docker Compose (Recommended)

Spins up the app, MySQL, Redis, and (optionally) Adminer in one command.

```bash
# Copy and fill in environment variables
cp .env.example .env
nano .env

# Start everything (production mode)
docker compose up -d

# Start with Adminer DB UI at :8081 (dev mode)
docker compose --profile dev up -d

# View logs
docker compose logs -f app

# Stop
docker compose down
```

---

##  Authentication Flow

```
Register ──► Email Verification ──► Login ──► Access Token (24h)
                                         └──► Refresh Token (7d)

Access Token expired? ──► POST /auth/refresh ──► New Access Token
Logout?               ──► Access Token added to Redis blacklist immediately
```

**Request header for protected routes:**
```
Authorization: Bearer <access_token>
```

---

##  API Reference

Swagger UI: `http://localhost:8080/api/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

### Authentication

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | — | Create account |
| POST | `/auth/login` | — | Login, get tokens |
| POST | `/auth/refresh` | — | Refresh access token |
| POST | `/auth/logout` | ✓ | Invalidate tokens (blacklists access token via Redis) |
| GET | `/auth/verify-email?token=` | — | Verify email address |
| POST | `/auth/forgot-password` | — | Request password reset email |
| POST | `/auth/reset-password` | — | Reset password via email token |
| POST | `/auth/change-password` | ✓ | Change own password |

### Posts

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/posts` | — | List published posts (paginated, sortable) |
| GET | `/posts/search?query=` | — | Full-text search |
| GET | `/posts/{slug}` | — | Get post by slug (increments view count) |
| GET | `/posts/category/{slug}` | — | Posts in a category |
| GET | `/posts/tag/{slug}` | — | Posts with a tag |
| GET | `/posts/user/{username}` | — | Posts by a user |
| POST | `/posts` | ✓ | Create post |
| PUT | `/posts/{id}` | ✓ Owner/Admin | Update post |
| DELETE | `/posts/{id}` | ✓ Owner/Admin | Soft-delete post |
| PATCH | `/posts/{id}/publish` | ✓ Owner/Admin | Publish draft |
| PATCH | `/posts/{id}/archive` | ✓ Owner/Admin | Archive post |

### Comments

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/posts/{id}/comments` | — | Get approved comments (threaded) |
| POST | `/posts/{id}/comments` | ✓ | Add comment or reply |
| PUT | `/comments/{id}` | ✓ Owner/Admin | Edit comment |
| DELETE | `/comments/{id}` | ✓ Owner/Admin | Soft-delete comment |

### Reactions

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| POST | `/posts/{id}/reactions` | ✓ | `{"reaction":"LIKE"}` | Like or dislike a post |
| DELETE | `/posts/{id}/reactions` | ✓ | — | Remove post reaction |
| POST | `/comments/{id}/reactions` | ✓ | `{"reaction":"DISLIKE"}` | React to comment |
| DELETE | `/comments/{id}/reactions` | ✓ | — | Remove comment reaction |

### Categories

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/categories` | — | List categories (paginated) |
| GET | `/categories/all` | — | All categories as flat list (for dropdowns) |
| GET | `/categories/{slug}` | — | Get category by slug |
| POST | `/categories` | Admin | Create category |
| PUT | `/categories/{id}` | Admin | Update category |
| DELETE | `/categories/{id}` | Admin | Delete category |

### Tags

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tags/popular` | — | Most-used tags |
| GET | `/tags/search?q=` | — | Autocomplete tag search |
| GET | `/tags/{slug}` | — | Tag details by slug |
| DELETE | `/tags/{id}` | Admin | Delete tag |

### Users

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/users/{username}/profile` | — | Public profile |
| GET | `/users/me` | ✓ | Own profile |
| PUT | `/users/{id}` | ✓ Owner/Admin | Update profile |
| DELETE | `/users/{id}` | ✓ Owner/Admin | Delete account |

### Moderator

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/moderator/comments/pending` | Mod/Admin | Comments awaiting approval |
| PATCH | `/moderator/comments/{id}/approve` | Mod/Admin | Approve comment |
| PATCH | `/moderator/comments/{id}/reject` | Mod/Admin | Reject/hide comment |

### Admin

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/dashboard` | Platform statistics |
| GET | `/admin/users` | All users (searchable, paginated) |
| PUT | `/admin/users/{id}` | Update any user (role, enabled, locked) |
| PATCH | `/admin/users/{id}/enable` | Enable user account |
| PATCH | `/admin/users/{id}/disable` | Disable user account |
| PATCH | `/admin/users/{id}/lock` | Lock user account |
| PATCH | `/admin/users/{id}/unlock` | Unlock user account |
| DELETE | `/admin/users/{id}` | Delete user |
| GET | `/admin/posts` | All posts (all statuses) |
| PATCH | `/admin/posts/{id}/publish` | Force-publish any post |
| PATCH | `/admin/posts/{id}/archive` | Archive any post |
| PATCH | `/admin/posts/{id}/restore` | Restore a soft-deleted post |
| DELETE | `/admin/posts/{id}` | Delete any post |

---

##  Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report
# Open: target/site/jacoco/index.html

# Run a specific test class
mvn test -Dtest=AuthServiceTest

# Run integration tests only (requires Docker)
mvn test -Dgroups=integration
```

### Test Coverage

| Test Class | Type | What it covers |
|---|---|---|
| `AuthServiceTest` | Unit | Registration, login, token flows, password management |
| `PostServiceTest` | Unit | CRUD, access control, publishing, soft delete |
| `CommentServiceTest` | Unit | Creation, editing, approval, access control |
| `CategoryServiceTest` | Unit | CRUD, duplicate detection, caching |
| `SlugUtilTest` | Unit | Slug generation edge cases (parameterized) |
| `AuthControllerTest` | Integration | HTTP layer, validation, response shapes |
| `AbstractIntegrationTest` | Base | Testcontainers — real MySQL via Docker |
| `PostIntegrationTest` | Integration | Full request lifecycle against real DB |

---

##  Configuration Reference

| Property | Default | Description |
|----------|---------|-------------|
| `app.jwt.secret` | — | **Required** — min 256-bit secret |
| `app.jwt.expiration-ms` | `86400000` | Access token lifetime (24h) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Refresh token lifetime (7d) |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `spring.cache.redis.time-to-live` | `600000` | Cache TTL in ms (10 min) |
| `app.mail.from` | — | Sender address for system emails |
| `app.frontend.url` | — | Base URL used in email links |
| `app.pagination.default-size` | `10` | Default page size |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Never use `create` in production |
| `spring.flyway.enabled` | `true` | Runs migrations on startup |

---

##  Security Model

### Rate Limiting
All `/auth/**` endpoints are protected by Bucket4j: **10 requests per minute per IP**. Exceeding this returns `HTTP 429` with a JSON error body. In a multi-instance deployment, upgrade to the Redis-backed Bucket4j adapter so limits are shared across instances.

### JWT Blacklist
On logout, the access token is stored in Redis with a TTL equal to its remaining validity. Every authenticated request checks the blacklist before proceeding. This means logout is truly immediate — no waiting for token expiry.

### Soft Deletes
Posts and comments are never physically removed from the database. `DELETE` calls set `deleted_at = NOW()`. Hibernate's `@SQLRestriction` automatically filters these rows from all queries. Admins can restore content via `PATCH /admin/posts/{id}/restore`.

### Password Policy
Passwords must be 8+ characters and contain uppercase, lowercase, a digit, and a special character. BCrypt with cost factor 12 is used for hashing.

---

##  Production Checklist

- [ ] Set a strong, random `app.jwt.secret` (32+ chars) via environment variable
- [ ] Use environment variables or a secrets manager — never commit credentials
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure real SMTP credentials for email delivery
- [ ] Enable HTTPS / TLS termination at load balancer or reverse proxy
- [ ] Restrict CORS origins in `SecurityConfig` to your actual frontend domain
- [ ] Review actuator exposure (`management.endpoints.web.exposure.include`)
- [ ] Ensure Redis is password-protected in production (`spring.data.redis.password`)
- [ ] Set up log rotation and ship logs to a centralised system (ELK, Datadog, etc.)
- [ ] Tune HikariCP pool size to match your expected database connections
- [ ] Never use `ddl-auto=create-drop` — rely on Flyway exclusively
- [ ] Upgrade rate-limit buckets to Redis-backed for multi-instance deployments

---

##  Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6, JWT (jjwt 0.11) |
| Database | MySQL 8 |
| Cache / Blacklist | Redis 7 |
| ORM | Spring Data JPA / Hibernate 6 |
| Migrations | Flyway |
| Rate Limiting | Bucket4j 8 |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Email | Spring Mail (JavaMailSender) |
| Build | Maven 3.9 |
| Containerisation | Docker + Docker Compose |
| Testing | JUnit 5, Mockito, Spring Boot Test, Testcontainers |

---

##  Project Structure

```
blogging-platform/
├── src/
│   ├── main/
│   │   ├── java/com/blogplatform/   # All application source (66 Java files)
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-test.properties
│   │       └── db/migration/
│   │           ├── V1__Initial_Schema.sql
│   │           └── V2__Soft_Deletes.sql
│   └── test/
│       └── java/com/blogplatform/   # Unit + integration tests
├── Dockerfile                        # Multi-stage build, non-root user
├── docker-compose.yml                # App + MySQL + Redis + Adminer
├── .env.example                      # Environment variable template
├── pom.xml
└── README.md
```

---

##  Contributing

Contributions are welcome. Please open an issue before submitting a pull request for significant changes.

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Commit your changes: `git commit -m "feat: add your feature"`
4. Push the branch: `git push origin feat/your-feature`
5. Open a pull request

Please make sure all tests pass (`mvn test`) before submitting.

---

##  License

MIT License — see [LICENSE](LICENSE) for details.
