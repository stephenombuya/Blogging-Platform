# 📝 Blogging Platform — Production-Ready Back-End

A fully-featured, production-ready RESTful blogging platform built with **Java 17**, **Spring Boot 3**, **MySQL**, and **JWT authentication**.

---

## 🏗️ Architecture

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
│   ├── CategoryService      # Category management (cached)
│   ├── TagService           # Tag find-or-create, autocomplete
│   ├── LikeService          # Polymorphic like/dislike reactions
│   ├── UserService          # Profile management
│   └── AdminService         # Dashboard stats, token cleanup
│
├── repository/          # Spring Data JPA repositories
├── model/               # JPA entity classes
├── dto/                 # Request/Response transfer objects
├── config/              # Security, JWT filter, OpenAPI, CORS
├── exception/           # Custom exceptions + GlobalExceptionHandler
└── util/                # JwtUtil, SlugUtil, EmailService, SecurityUtil
```

---

## ✨ Features

| Feature | Details |
|---|---|
| **Authentication** | JWT access tokens + refresh tokens, email verification |
| **Authorization** | Role-based: `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN` |
| **Post Management** | CRUD, slugs, DRAFT / PUBLISHED / ARCHIVED statuses |
| **Categories** | Admin-managed, cached, slug-based routing |
| **Tags** | Auto-created on post save, full autocomplete search |
| **Comments** | Threaded (parent/reply), moderation approval flow |
| **Reactions** | Like/dislike on both posts and comments (polymorphic) |
| **Admin Dashboard** | Stats, user management, content moderation |
| **Email** | Async HTML emails: verification, password reset, welcome |
| **Search** | Full-text search across post title/content |
| **Pagination** | All list endpoints are paginated via `page` + `size` |
| **API Docs** | Swagger UI at `/swagger-ui.html` |
| **DB Migrations** | Flyway — versioned, repeatable migrations |
| **Security** | BCrypt-12 passwords, stateless JWT, CORS configured |
| **Scheduled Jobs** | Nightly expired-token cleanup |

---

## 🗄️ Database Schema

```
users ──────────────────────── posts
  │  (1:N author)               │  (N:1 category) ── categories
  │                             │  (N:M tags)      ── tags
  │                             │  (1:N)
  └──────────────────── comments
                           │  (self-join parent/reply)
                           └── likes (polymorphic: POST | COMMENT)
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- MySQL 8+
- Maven 3.9+

### Local Setup

```bash
# 1. Clone
git clone https://github.com/stephenombuya/Blogging-Platform
cd blogging-platform

# 2. Configure database
cp src/main/resources/application.properties src/main/resources/application-local.properties
# Edit: spring.datasource.username, spring.datasource.password

# 3. Build
mvn clean install

# 4. Run
mvn spring-boot:run
```

### Docker Compose (Recommended)

```bash
# Copy and fill in environment variables
cp .env.example .env
nano .env

# Start with dev tools (includes Adminer at :8081)
docker compose --profile dev up -d

# Start production only
docker compose up -d

# View logs
docker compose logs -f app

# Stop
docker compose down
```

---

## 📖 API Reference

Swagger UI: `http://localhost:8080/api/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

### Authentication Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/register` | — | Create account |
| POST | `/auth/login` | — | Login, get tokens |
| POST | `/auth/refresh` | — | Refresh access token |
| POST | `/auth/logout` | ✓ | Invalidate refresh token |
| GET | `/auth/verify-email?token=` | — | Verify email |
| POST | `/auth/forgot-password` | — | Request reset email |
| POST | `/auth/reset-password` | — | Reset via email token |
| POST | `/auth/change-password` | ✓ | Change own password |

### Post Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/posts` | — | List published posts |
| GET | `/posts/search?query=` | — | Full-text search |
| GET | `/posts/{slug}` | — | Get post by slug |
| GET | `/posts/category/{slug}` | — | Posts in category |
| GET | `/posts/tag/{slug}` | — | Posts with tag |
| GET | `/posts/user/{username}` | — | Posts by user |
| POST | `/posts` | ✓ | Create post |
| PUT | `/posts/{id}` | ✓ Owner/Admin | Update post |
| DELETE | `/posts/{id}` | ✓ Owner/Admin | Delete post |
| PATCH | `/posts/{id}/publish` | ✓ Owner/Admin | Publish draft |
| PATCH | `/posts/{id}/archive` | ✓ Owner/Admin | Archive post |

### Comment Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/posts/{id}/comments` | — | Get approved comments |
| POST | `/posts/{id}/comments` | ✓ | Add comment/reply |
| PUT | `/comments/{id}` | ✓ Owner/Admin | Edit comment |
| DELETE | `/comments/{id}` | ✓ Owner/Admin | Delete comment |

### Reaction Endpoints

| Method | Path | Auth | Body | Description |
|--------|------|------|------|-------------|
| POST | `/posts/{id}/reactions` | ✓ | `{"reaction":"LIKE"}` | Like/dislike post |
| DELETE | `/posts/{id}/reactions` | ✓ | — | Remove reaction |
| POST | `/comments/{id}/reactions` | ✓ | `{"reaction":"DISLIKE"}` | React to comment |
| DELETE | `/comments/{id}/reactions` | ✓ | — | Remove reaction |

### Admin Endpoints (ROLE_ADMIN)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/admin/dashboard` | Platform stats |
| GET | `/admin/users` | All users (search) |
| PUT | `/admin/users/{id}` | Update any user |
| PATCH | `/admin/users/{id}/enable` | Enable account |
| PATCH | `/admin/users/{id}/lock` | Lock account |
| DELETE | `/admin/users/{id}` | Delete user |
| GET | `/admin/posts` | All posts (any status) |
| PATCH | `/admin/posts/{id}/publish` | Force-publish |
| DELETE | `/admin/posts/{id}` | Delete any post |

---

## 🔐 Authentication Flow

```
Register ──► Email Verification ──► Login ──► Access Token (24h)
                                         └──► Refresh Token (7d)

Access Token expired? ──► POST /auth/refresh ──► New Access Token
```

**Request header for protected routes:**
```
Authorization: Bearer <access_token>
```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage report
mvn test jacoco:report
# Open target/site/jacoco/index.html

# Run specific test class
mvn test -Dtest=AuthServiceTest

# Run only unit tests (exclude integration)
mvn test -Dtest="*Test" -DexcludedGroups=integration
```

**Test coverage includes:**
- `AuthServiceTest` — registration, login, token flows, password management
- `PostServiceTest` — CRUD, access control, publishing
- `CommentServiceTest` — creation, editing, moderation
- `CategoryServiceTest` — CRUD, duplicate detection
- `SlugUtilTest` — slug generation edge cases
- `AuthControllerTest` — HTTP layer, validation, response shapes

---

## ⚙️ Configuration Reference

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `app.jwt.secret` | — | **Required** — min 256-bit secret |
| `app.jwt.expiration-ms` | `86400000` | Access token lifetime (24h) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Refresh token lifetime (7d) |
| `app.mail.from` | — | Sender address for system emails |
| `app.frontend.url` | — | Base URL for email links |
| `app.pagination.default-size` | `10` | Default page size |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Use `create` only for dev |
| `spring.flyway.enabled` | `true` | Database migration on startup |

---

## 🏭 Production Checklist

- [ ] Set strong `app.jwt.secret` (32+ random chars)
- [ ] Use environment variables / secrets manager for credentials
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure real SMTP credentials
- [ ] Enable HTTPS / TLS termination at load balancer
- [ ] Set appropriate CORS origins in `SecurityConfig`
- [ ] Review actuator exposure (`management.endpoints.web.exposure.include`)
- [ ] Set up log rotation and monitoring
- [ ] Use connection pooling tuning for expected load
- [ ] Set `spring.flyway.enabled=true` and never use `ddl-auto=create-drop`

---

## 📦 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security 6, JWT (jjwt 0.11) |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate 6 |
| Migrations | Flyway |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Email | Spring Mail (JavaMailSender) |
| Build | Maven 3.9 |
| Containerization | Docker + Docker Compose |
| Testing | JUnit 5, Mockito, Spring Boot Test |

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.
