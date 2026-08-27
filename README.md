# e-SpazaSethu 🛒

> **"Our digital spaza"** — A mobile-first POS and inventory management system built for South African spaza shop owners.

e-SpazaSethu replaces paper notebooks and memory-based stock tracking with a simple, affordable digital system. Shop owners can process sales, track stock automatically, monitor daily profits, and get alerted when products are running low — all from their Android phone browser.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [Environment Variables](#environment-variables)
- [Running the App](#running-the-app)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Features](#features)
- [Team](#team)
- [Branch Strategy](#branch-strategy)
- [Contributing](#contributing)

---

## Overview

Spaza shop owners across South Africa currently manage inventory through memory or paper notebooks — leading to stock losses, inaccurate profit tracking, and poor restocking decisions. e-SpazaSethu solves this by providing:

- **POS selling screen** — search or scan products, build a cart, complete checkout
- **Automatic stock deduction** — stock updates the moment a sale is completed
- **Low-stock alerts** — be notified when products fall below a set threshold
- **Daily dashboard** — see today's sales total and profit at a glance
- **Stocktaking** — count physical stock and reconcile against system records
- **Role-based access** — separate admin and cashier permissions

**MVP scope:** Online-first web app optimised for Android mobile browsers. Full offline capability is planned for Phase 2.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 + TypeScript (Vite) |
| Styling | Tailwind CSS |
| Backend | Spring Boot 3 (Java 17) |
| Database | MySQL 8 |
| Auth | Spring Security + JWT |
| API | REST / JSON |
| Version Control | Git + GitHub |

---

## Project Structure

```
e-spazasethu/                        # Monorepo root
├── frontend/                        # React + TypeScript app
│   ├── public/
│   ├── src/
│   │   ├── components/              # Reusable UI components
│   │   │   ├── pos/                 # POS-specific components (cart, scanner, checkout)
│   │   │   ├── shared/              # General-purpose components (buttons, modals, etc.)
│   │   │   └── layout/              # Layout components (BottomNav, PageWrapper, etc.)
│   │   ├── pages/                   # One file per screen
│   │   │   ├── LoginPage.tsx
│   │   │   ├── DashboardPage.tsx
│   │   │   ├── POSPage.tsx
│   │   │   ├── ProductsPage.tsx
│   │   │   ├── ProductFormPage.tsx
│   │   │   ├── SalesHistoryPage.tsx
│   │   │   ├── SaleDetailPage.tsx
│   │   │   ├── StocktakePage.tsx
│   │   │   ├── StocktakeCountPage.tsx
│   │   │   ├── StocktakeReviewPage.tsx
│   │   │   ├── StockMovementsPage.tsx
│   │   │   ├── ReportsPage.tsx
│   │   │   ├── UsersPage.tsx
│   │   │   ├── UserFormPage.tsx
│   │   │   └── SettingsPage.tsx
│   │   ├── services/                # API calls
│   │   │   └── api.ts               # Axios instance with JWT interceptor
│   │   ├── context/                 # React Context providers
│   │   │   └── AuthContext.tsx      # Auth state (user, token, login, logout)
│   │   ├── hooks/                   # Custom React hooks
│   │   ├── types/                   # TypeScript interfaces and types
│   │   ├── utils/                   # Helper functions (formatting, dates, etc.)
│   │   ├── routes/
│   │   │   └── AppRouter.tsx        # Route definitions + protected route wrappers
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── index.css
│   ├── .env.example
│   ├── index.html
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── package.json
│
├── backend/                         # Spring Boot app
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/za/co/espaza/espazaapi/
│   │   │   │   ├── config/          # Security config, CORS, beans
│   │   │   │   ├── controller/      # REST controllers
│   │   │   │   ├── dto/             # Request and response DTOs
│   │   │   │   │   ├── request/
│   │   │   │   │   └── response/
│   │   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── enums/           # Java enums (Role, SaleStatus, etc.)
│   │   │   │   ├── exception/       # Custom exceptions + global handler
│   │   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   │   ├── security/        # JWT filter, UserDetailsService
│   │   │   │   └── service/         # Business logic
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       └── schema.sql       # Database schema
│   │   └── test/
│   │       └── java/za/co/espaza/espazaapi/
│   │           └── service/         # Unit tests (JUnit 5 + Mockito)
│   ├── .env.example
│   └── pom.xml
│
└── README.md                        # This file
```

---

## Getting Started

### Prerequisites

Make sure you have the following installed before you begin:

| Tool | Version | Download |
|---|---|---|
| Node.js | 18+ | https://nodejs.org |
| npm | 9+ | Included with Node |
| Java JDK | 17 | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| MySQL | 8.0+ | https://dev.mysql.com/downloads |
| Git | Any | https://git-scm.com |

---

### Backend Setup

**1. Clone the repository**

```bash
git clone https://github.com/YOUR_ORG/e-spazasethu.git
cd e-spazasethu/backend
```

**2. Create the MySQL database**

Open your MySQL client (MySQL Workbench, DBeaver, or terminal) and run:

```sql
CREATE DATABASE espaza_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**3. Run the schema script**

```bash
mysql -u root -p espaza_db < src/main/resources/schema.sql
```

This creates all 8 tables and seeds a default admin user.

> **Default admin credentials (local dev only):**
> Username: `admin` | Password: `admin123`
> Change this immediately in any environment beyond your local machine.

**4. Configure environment variables**

```bash
cp .env.example .env
```

Open `.env` and fill in your values:

```env
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=a-long-random-secret-string-at-least-64-characters
ALLOWED_ORIGIN=http://localhost:5173
```

**5. Install dependencies and build**

```bash
mvn clean install
```

---

### Frontend Setup

**1. Navigate to the frontend directory**

```bash
cd ../frontend
```

**2. Install dependencies**

```bash
npm install
```

**3. Configure environment variables**

```bash
cp .env.example .env
```

Open `.env` and set:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

---

## Running the App

### Start the backend

```bash
cd backend
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Start the frontend

Open a new terminal:

```bash
cd frontend
npm run dev
```

The app will be available at `http://localhost:5173`.

### Running backend tests

```bash
cd backend
mvn test
```

### Building for production

**Frontend:**
```bash
cd frontend
npm run build
```
Output goes to `frontend/dist/`.

**Backend:**
```bash
cd backend
mvn clean package -DskipTests
```
Output JAR is at `backend/target/espaza-api-*.jar`.

---

## Environment Variables

### Backend (`backend/.env`)

| Variable | Required | Description |
|---|---|---|
| `DB_USERNAME` | Yes | MySQL username |
| `DB_PASSWORD` | Yes | MySQL password |
| `JWT_SECRET` | Yes | Secret key for signing JWT tokens — must be at least 64 characters |
| `ALLOWED_ORIGIN` | Yes | Frontend URL allowed by CORS (e.g. `http://localhost:5173`) |
| `DB_URL` | No | Full JDBC URL — defaults to `jdbc:mysql://localhost:3306/espaza_db` |

### Frontend (`frontend/.env`)

| Variable | Required | Description |
|---|---|---|
| `VITE_API_BASE_URL` | Yes | Backend API base URL (e.g. `http://localhost:8080/api/v1`) |

> **Never commit `.env` files.** Both are listed in `.gitignore`. Only `.env.example` files are committed.

---

## API Documentation

Once the backend is running, Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

All endpoints are documented with request/response schemas and require a Bearer token (except `POST /api/v1/auth/login`).

### Quick endpoint reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Public | Log in and receive JWT |
| GET | `/api/v1/products` | All | List / search products |
| POST | `/api/v1/products` | Admin | Create product |
| PUT | `/api/v1/products/:id` | Admin | Update product |
| GET | `/api/v1/products/low-stock` | All | Products at or below threshold |
| GET | `/api/v1/categories` | All | List categories |
| POST | `/api/v1/sales` | All | Process a sale (deducts stock) |
| GET | `/api/v1/sales` | All | Sales history with date filter |
| GET | `/api/v1/sales/:id` | All | Sale detail with line items |
| POST | `/api/v1/stocktakes` | Admin | Start a stocktake |
| POST | `/api/v1/stocktakes/:id/apply` | Admin | Apply stocktake adjustments |
| GET | `/api/v1/movements` | Admin | Stock movement audit log |
| POST | `/api/v1/movements/adjustment` | Admin | Manual stock adjustment |
| GET | `/api/v1/reports/dashboard` | All | Today's sales and profit |
| GET | `/api/v1/users` | Admin | List all users |
| POST | `/api/v1/users` | Admin | Create user |

---

## Database Schema

The system uses 8 tables:

```
user              → core user accounts and roles
category          → optional product groupings
product           → product catalogue with pricing and stock
sale              → sale header (one per transaction)
sale_item         → line items within a sale
stock_movement    → immutable audit trail of every stock change
stocktake         → physical inventory count sessions
stocktake_item    → per-product count entries within a stocktake
```

Foreign key relationships:
- `product` → `category`
- `sale` → `user`
- `sale_item` → `sale`, `product`
- `stock_movement` → `product`, `user`
- `stocktake` → `user`
- `stocktake_item` → `stocktake`, `product`

See `backend/src/main/resources/schema.sql` for the full DDL.

---

## Features

### MVP (Current)

- [x] JWT-based authentication with role support (Admin / Cashier)
- [x] Product catalogue — add, edit, deactivate
- [x] Category management
- [x] POS selling screen — search, barcode scan, cart, checkout
- [x] Automatic stock deduction on sale completion
- [x] Low-stock alerts (configurable per product)
- [x] Daily sales and profit dashboard
- [x] Sales history with date filtering
- [x] Stock movement audit log (immutable)
- [x] Stocktaking — count, review discrepancies, apply corrections
- [x] Manual stock adjustments (admin only, requires reason)
- [x] User management (admin only)

### Phase 2 (Planned)

- [ ] Full offline capability with sync
- [ ] Supplier management and goods receiving
- [ ] Stock reversal on sale cancellation
- [ ] Receipt printing / WhatsApp receipt sharing
- [ ] Multi-device support

---

## Team

| Name | Role | Responsibilities |
|---|---|---|
| **Samukelo Ndlela** | Tech Lead | Backend architecture, Spring Security, Sale transaction service, Reports |
| **Avuyile** | Frontend | Project setup, Login, Dashboard, Sales History |
| **Lesego** | Frontend | POS screen, Barcode scanner, Products management |
| **Lisakhanya** | Backend | Users, Products, Categories, Stock Movements API |
| **Amanda** | Backend | Sale CRUD, Stocktake, Input validation, Unit tests |
| **Phumelela** | Full Stack | Stocktake frontend, Reports, Settings, User management frontend |

---

## Branch Strategy

We use a feature branch workflow. No one pushes directly to `main` or `develop`.

```
main        →  production-ready only — merged from develop after team review
develop     →  integration branch — all feature branches merge here via PR
feature/*   →  one branch per GitHub issue
fix/*       →  bug fixes
```

**Branch naming convention:**

```bash
feature/FE-01-project-setup
feature/BE-05-sale-service
fix/FE-11-checkout-validation-bug
```

**Pull Request rules:**
- Branch must be up to date with `develop` before opening a PR
- At least 1 approval required (tech lead reviews all backend PRs)
- All PR descriptions must reference the issue number (e.g. `Closes #12`)
- No direct pushes to `main` or `develop`

---

## Contributing

1. Pick your assigned issue from the GitHub Issues board
2. Create a branch from `develop`:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/FE-01-your-issue-name
   ```
3. Do your work, commit with descriptive messages:
   ```bash
   git commit -m "FE-01: Add Vite project setup with Tailwind and React Router"
   ```
4. Push and open a Pull Request to `develop`:
   ```bash
   git push origin feature/FE-01-your-issue-name
   ```
5. Reference the issue in your PR description: `Closes #1`
6. Request a review from the tech lead

---

## Commit Message Format

```
[ISSUE-ID]: Short description of what was done

FE-01: Initialise Vite project with Tailwind CSS and React Router
BE-05: Implement atomic sale transaction with stock deduction
fix: Correct cart total calculation for quantity > 1
```

---

*Built with purpose for South African small businesses — e-SpazaSethu, ours.*