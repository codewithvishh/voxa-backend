# Music Streaming App - Spring Boot Backend

Production-ready RESTful backend built with **Spring Boot 3 / Java 21**, **Spring Security + JJWT**, **Spring Data JPA**, **PostgreSQL / H2**, and **OpenAPI Swagger UI**.

---

## 🌟 Key Features

1. **Authentication & Roles**:
   - Secure BCrypt password hashing
   - Stateless JWT access tokens + refresh tokens
   - Role-based authorization (`ROLE_GUEST`, `ROLE_USER`, `ROLE_ADMIN`)
   - Pre-seeded default credentials:
     - **Admin**: `admin@musicapp.com` / `admin123`
     - **User**: `user@musicapp.com` / `user123`

2. **Music Catalog & Discovery**:
   - Songs, Artists, Albums, and Genres
   - Paginated listings (`/api/songs?page=0&size=20&genre=Synthwave`)
   - Instant search across Song Title, Artist Name, and Album
   - Debounced search autocomplete suggestions (`/api/search/suggest?q=...`)
   - Home sections (`/api/home`): Trending, New Releases, Top Genres, Personalized "Made for you"
   - "More like this" recommendations (`/api/songs/{id}/similar`)

3. **Audio Playback Tracking**:
   - Streams directly from remote audio sources (HTML5 `<audio>` compatible)
   - Play counter incremented after 30s playback (`POST /api/songs/{id}/play`)
   - Recent playback history capped at 50 tracks per user (`GET /api/me/history`)

4. **Playlists & Library**:
   - Full playlist CRUD (Create, Rename, Public/Private, Delete)
   - Add/Remove songs from playlists
   - Liked Songs library with instant toggle (`POST /api/songs/{id}/like`)

5. **Jamendo Music Catalog Ingestion**:
   - Asynchronous batch importer (`POST /api/admin/import?max=50&tag=rock`)
   - Idempotent upserting by Jamendo `sourceId`
   - Throttled requests to prevent rate limits
   - Status & error tracking via `ImportJob`

6. **Interactive Documentation**:
   - Swagger / OpenAPI UI at: `http://localhost:8080/swagger-ui.html`
   - OpenAPI 3 JSON spec at: `http://localhost:8080/v3/api-docs`
   - H2 Database Console at: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:musicappdb`, user: `sa`, password: `password`)

---

## 🚀 How to Run Locally

### Prerequisites
- Java 21+ installed

### Step 1: Run with Maven Wrapper
```bash
cd backend
./mvnw.cmd spring-boot:run
```
*(On Linux/macOS use `./mvnw spring-boot:run`)*

The server will start on port `8080` with in-memory H2 database pre-loaded with:
- 12 Creative Commons streaming songs with album art & artist metadata
- 5 Artists & Albums
- Pre-configured Admin and Demo User accounts
- Sample playlist

### Step 2: Test the API
```bash
# Health check
curl http://localhost:8080/actuator/health

# Home feed
curl http://localhost:8080/api/home

# Login as demo user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"user@musicapp.com\",\"password\":\"user123\"}"
```
