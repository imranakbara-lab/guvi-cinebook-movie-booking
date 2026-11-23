# CineBook – Movie Ticket Booking System

![CI](https://github.com/imranakbara-lab/guvi-cinebook-movie-booking/actions/workflows/ci.yml/badge.svg)

**Live:** https://guvi-cinebook-movie-booking.vercel.app · **API docs:** https://cinebook-api.onrender.com/swagger-ui.html

Browse movies, view showtimes, pick seats on a live seat map, pay and get your ticket by email. Theaters manage movies, screens, showtimes and bookings from an admin panel. Seat locking prevents double booking.

| Layer | Tech |
|---|---|
| Backend | Spring Boot 3, Spring Data JPA, REST, Spring Security + JWT, Hibernate Validator, `@ControllerAdvice`, Spring Boot Starter Mail, `@Scheduled`, SpringDoc OpenAPI / Swagger |
| Payment | Razorpay (orders API + HMAC-SHA256 signature verification) or a simulated test-mode gateway when no keys are set |
| Frontend | React 18 (Vite), React Router, Axios, Bootstrap 5, Formik + Yup |
| Database | H2 (local) · PostgreSQL / MySQL (deployment) |
| Testing | JUnit 5 + Mockito – controller, service, repository tests + end-to-end booking workflow (54 tests) |

## Modules

| Module | What it does |
|---|---|
| **User** | Register / login (JWT, roles USER & ADMIN), profile update, change password, booking history |
| **Movie** | CRUD: title, genre, language, duration, certificate, rating, poster, release date, active flag. Browse by **date, genre, city, theater** and search |
| **Theater** | Theaters with city / address and **seat layout** (rows × seats, premium back rows) |
| **Show** | Showtimes for a movie at a theater with regular & premium prices; **clash check** so two shows never overlap on one screen (incl. 15-min cleaning gap) |
| **Seat booking** | Seat map with taken seats (auto-refreshes every 10 s). Selected seats are **locked for 10 minutes**; a database unique constraint on *(show, seat)* makes double booking impossible even for simultaneous clicks. Unpaid holds expire automatically (scheduled job) |
| **Booking** | Lifecycle `PENDING_PAYMENT → CONFIRMED → CANCELLED` (or `EXPIRED`), booking reference, cancellation up to 60 min before the show with refund status |
| **Payment** | Razorpay order + signature verification when `RAZORPAY_KEY_ID/SECRET` are set; otherwise simulated payment confirmation |
| **Notification** | Emails for registration, booking confirmation and cancellation (logged when SMTP is not configured) |
| **Admin** | Manage movies, theaters, shows, all bookings (revenue / tickets sold, cancel) and users |

## Project structure

```
cinebook/
├── backend/   Spring Boot REST API (com.cinebook: config, controller, dto, entity, exception, repository, security, service)
└── frontend/  React app (pages: Home, MovieDetail, SeatSelection, Checkout, BookingDetail, MyBookings, Profile, admin/*)
```

## Run locally

```bash
# Backend (Java 17+, Maven) – http://localhost:8080 , Swagger at /swagger-ui.html
cd backend
mvn spring-boot:run
mvn test

# Frontend (Node 18+) – http://localhost:5173
cd frontend
npm install
npm run dev
```
On first start the app creates the admin account plus sample theaters, movies and a week of shows.

## Environment variables (backend)

| Variable | Purpose |
|---|---|
| `DATABASE_URL` or `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | database |
| `DB_SCHEMA` | schema for the tables (default `PUBLIC`) |
| `JWT_SECRET` | at least 32 characters |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | first admin account |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Razorpay **test** keys (optional – simulated payment without them) |
| `BOOKING_HOLD_MINUTES`, `CANCEL_BEFORE_MINUTES` | booking rules (default 10 and 60) |
| `CORS_ORIGINS`, `FRONTEND_URL` | frontend URL(s) |
| `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | SMTP (e.g. Gmail App Password) |

## API overview

| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Public |
| GET | `/api/movies?date=&genre=&city=&theaterId=&q=`, `/api/movies/{id}`, `/api/movies/{id}/shows?date=&city=` | Public |
| GET | `/api/genres`, `/api/cities`, `/api/theaters`, `/api/shows/{id}`, `/api/shows/{id}/seats` | Public |
| POST | `/api/bookings` (hold seats) | User |
| POST | `/api/bookings/{id}/payment-order`, `/api/bookings/{id}/payment-confirm` | Owner |
| GET | `/api/bookings/my`, `/api/bookings/{id}` | Owner / Admin |
| POST | `/api/bookings/{id}/cancel` | Owner / Admin |
| GET / PUT | `/api/users/me`, PUT `/api/users/me/password` | User |
| CRUD | `/api/admin/movies`, `/api/admin/theaters`, `/api/admin/shows` | Admin |
| GET / POST | `/api/admin/bookings?showId=`, `/api/admin/bookings/{id}/cancel` | Admin |
| GET / PUT / DELETE | `/api/admin/users` | Admin |

## Deployment
- **Backend → Render** (Docker, `render.yaml`) – https://cinebook-api.onrender.com
- **Frontend → Vercel** (root `frontend`, env `VITE_API_URL`) – https://guvi-cinebook-movie-booking.vercel.app

### Demo logins
| Role | Email | Password |
|---|---|---|
| Admin | admin@cinebook.com | Admin@123 |
| User | user@cinebook.com | User@1234 |

> Payments run in **test mode** (simulated – no money is charged) unless Razorpay test keys are added. The backend is on Render's free plan, so the first request after inactivity can take ~50 seconds.
