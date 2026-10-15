# 🎓 Faculty Leave Management System (FLMS)

**Semester III Industry Practice-I | Java Full Stack Development Project**

A complete, production-grade web application built to streamline and automate faculty leave requests, approvals, balance tracking, and institutional reporting.

---

## 🌟 Key Highlights

- **Pure Java Backend**: Built using JDK's native `com.sun.net.httpserver.HttpServer` with clean MVC/Repository architecture. No black-box frameworks like Spring Boot — every line is transparent, easy to explain during viva, and demonstrates core OOP fundamentals.
- **Modern Responsive Frontend**: Crafted with semantic HTML5, Vanilla CSS3 (custom CSS design system with glassmorphism, responsive data tables, modals, and toasts), and Vanilla JavaScript (Fetch API, form validation, DOM manipulation).
- **Relational Persistence**: PostgreSQL database accessed through JDBC with 100% parameterized `PreparedStatement`s to guarantee protection against SQL injection.
- **Dual Authentication**:
  - **Google OAuth 2.0**: ID Token verification for real-world institutional sign-in.
  - **Evaluation / Viva Quick Login**: One-click demo authentication for Admin, HOD, and Faculty to enable hassle-free offline evaluations and presentations.
- **Role-Based Access Control (RBAC)**:
  - **Faculty**: Apply for leaves, view balances, track application status, download calendar, receive in-app notifications.
  - **HOD (Head of Department)**: Department approval queue, overlap detection, one-click approve/reject/return with comments, department schedule calendar.
  - **Administrator**: Comprehensive institutional control — manage users, approvals, departments, leave types, leave entitlement policies, academic holidays, audit logs, and exportable reports.

---

## 📚 Syllabus Alignment (Semester III Java FSD)

This project is tailored specifically for Semester III Java Full Stack Development curricula:

| Syllabus Area | How FLMS Demonstrates It |
|---|---|
| **Language Basics** | Variables, primitive and reference types, operators, flow control (`if/else`, `switch/case`), loops (`for`, `enhanced for`, `while`). |
| **Object-Oriented Programming** | Encapsulation (`User`, `LeaveApplication`, `LeaveBalance` models with private fields and getters/setters), Inheritance (`HttpHandler` implementations, `AuthMiddleware`), Polymorphism, and Separation of Concerns. |
| **Exception Handling** | Robust `try-catch-finally` and try-with-resources blocks across JDBC database operations and HTTP request lifecycle. |
| **Collections Framework** | `List`, `ArrayList`, `Map`, `HashMap`, `LinkedHashMap`, and `Set` used throughout data repositories and JSON transformers. |
| **HTML5 & Semantic Web** | Clean semantic hierarchy (`<header>`, `<nav>`, `<main>`, `<section>`, `<article>`, `<form>`, `<aside>`). |
| **CSS3 Design System** | Custom CSS variables (tokens for palette, spacing, typography, radii), Flexbox, CSS Grid, animations, and dark/light contrast. |
| **Vanilla JavaScript** | Asynchronous programming (`async/await`, `Promise`), Fetch API, client-side validation regex, DOM event listeners. |
| **Extensions Beyond Sem III** | JDBC connection management, Google OAuth token verification, HTTP session cookie management, and Google Gson serialization. |

---

## 🏗️ Architecture & Project Structure

```
ip project yoo/
├── frontend/                     # Pure HTML5 / CSS3 / Vanilla JS Frontend
│   ├── index.html                # Sign-in portal (Google OAuth + Demo Sign-In)
│   ├── dashboard.html            # Faculty personal dashboard
│   ├── apply-leave.html          # Leave application form with live day calculations
│   ├── my-leaves.html            # Leave history, status badges, and cancel modal
│   ├── hod-dashboard.html        # HOD approval queue, overlap warning, and review
│   ├── admin-dashboard.html      # Administrator management panel (all-in-one)
│   ├── css/
│   │   └── style.css             # Unified modern CSS design system
│   └── js/
│       ├── api.js                # Centrally configured Fetch API wrapper
│       ├── app.js                # Authentication state, session check, navbar, notifications
│       ├── validation.js         # Client-side form validators
│       ├── dashboard.js          # Faculty dashboard data binding & metrics
│       ├── leave-form.js         # Leave submission & date calculator
│       ├── hod.js                # HOD approval workflows & overlap checking
│       ├── admin.js              # Admin operations, tables, and CSV exports
│       └── utils.js              # Date arithmetic, toast notifications, formatting
├── backend/                      # Pure Java Backend
│   ├── pom.xml                   # Maven build configuration
│   ├── src/
│   │   ├── Main.java             # Entry point, DB initializer, server launcher
│   │   ├── server/
│   │   │   ├── AppServer.java    # Route registry & ThreadPool executor
│   │   │   └── StaticFileHandler.java # Secure static asset server (MIME + path sanitizer)
│   │   ├── models/               # Domain Entity Models (OOP Encapsulation)
│   │   │   ├── User.java
│   │   │   ├── Department.java
│   │   │   ├── LeaveType.java
│   │   │   ├── LeaveApplication.java
│   │   │   └── LeaveBalance.java
│   │   ├── repositories/         # JDBC Database Layer (Parameterized SQL)
│   │   │   ├── DatabaseManager.java
│   │   │   ├── UserRepository.java
│   │   │   ├── LeaveRepository.java
│   │   │   └── GeneralRepository.java
│   │   ├── handlers/             # HTTP Handlers (Controllers)
│   │   │   ├── AuthHandler.java
│   │   │   ├── LeaveHandler.java
│   │   │   ├── ApprovalHandler.java
│   │   │   ├── AdminHandler.java
│   │   │   ├── NotificationHandler.java
│   │   │   └── CommonHandler.java
│   │   ├── middleware/
│   │   │   └── AuthMiddleware.java # Decorator for session validation & RBAC
│   │   └── utils/
│   │       ├── JsonUtil.java     # Gson serialization & response utilities
│   │       ├── SessionManager.java # In-memory secure UUID token manager
│   │       └── DateUtil.java     # Academic year calculations
│   └── lib/                      # Offline JAR libraries (Gson + PostgreSQL Driver)
├── database/
│   ├── schema.sql                # Complete relational schema (13 tables & sequences)
│   ├── seed.sql                  # Seed data (departments, leave policies, holidays, test users)
│   └── setup_db.bat              # One-click PostgreSQL DB setup script
├── run.bat                       # Windows 1-click build & run script
├── run.sh                        # Linux / macOS / WSL build & run script
└── README.md                     # Comprehensive project documentation
```

---

## ⚙️ Quick Start Guide

### Prerequisites
- **Java Development Kit (JDK 17 or higher)** installed and present in your system `PATH`.

---

### Step 1: Database Setup (Simple & Free via Supabase — No Local PostgreSQL Installation Needed!)

1. Go to **[supabase.com](https://supabase.com)** (free cloud PostgreSQL).
2. Create a new project (e.g. `faculty-leave`).
3. In your Supabase project, go to:
   **Project Settings** (gear icon) → **Database** → scroll down to **Connection String**.
4. Select the **URI** or **JDBC** tab and copy the connection string.
5. Open [`db.properties`](file:///c:/Users/Aditya/ip%20project%20yoo/db.properties) in this folder and paste it:
   ```properties
   SUPABASE_URL=postgresql://postgres.abcdefg:YOUR_PASSWORD@aws-0-ap-south-1.pooler.supabase.com:6543/postgres
   ```
*(Replace `YOUR_PASSWORD` with the database password you chose when creating your Supabase project).*

> **Note**: You do NOT need to run any SQL scripts! The backend automatically creates all 13 tables, sequences, policies, holidays, and demo users on its first startup.

*(If you prefer running a local PostgreSQL database instead, leave `SUPABASE_URL` blank and run `database\setup_db.bat`).*

---

### Step 2: Run the Application
Double-click or run from command prompt:
```bash
run.bat
```
*(On Linux/macOS: `chmod +x run.sh && ./run.sh`)*

**What `run.bat` automatically does:**
1. Checks and downloads `gson-2.10.1.jar` and `postgresql-42.7.2.jar` into `backend/lib/` if missing.
2. Compiles all `.java` source files with `javac`.
3. Auto-creates all tables and seeds demo data on Supabase or PostgreSQL.
4. Starts the server on `http://localhost:8080`.
5. Automatically opens your default web browser to the application!

---

## 🧪 Testing & Evaluation Walkthrough (Viva Demo)

On the login page (`http://localhost:8080`), you can immediately sign in with any of the pre-configured test profiles:

| Role | Test Account | Capabilities to Demonstrate |
|---|---|---|
| **Administrator** | `admin@institution.edu` *(Prof. Albus Dumbledore)* | Manage users, approve new registrations, configure leave entitlement policies, add holidays, view institution-wide audit trail, export CSV leave reports. |
| **Head of Department (HOD)** | `hod.cs@institution.edu` *(Dr. Alan Turing)* | Review pending applications from CS faculty, test overlap checking (detects if multiple faculty are absent on the same day), approve/reject with remarks. |
| **Faculty Member** | `faculty.cs@institution.edu` *(Dr. Ada Lovelace)* | Apply for leave with auto day calculation (excluding weekends & holidays), view balance breakdown (Entitled, Used, Pending, Remaining), track approval status. |

---

## 🔒 Security Best Practices Implemented

1. **SQL Injection Prevention**: All queries use JDBC `PreparedStatement` with parameterized placeholders (`?`). No raw SQL string concatenation.
2. **Role-Based Authorization**: Endpoints are wrapped with `AuthMiddleware.requireRole(...)` ensuring faculty cannot access HOD or Admin APIs.
3. **Session Management**: Session cookies use `HttpOnly; SameSite=Strict; Path=/`.
4. **Directory Traversal Protection**: `StaticFileHandler` normalizes paths and verifies that requested files strictly reside within the designated public frontend directory.
5. **Auditing**: Every sensitive action (login, leave application, approval, policy change) writes to an immutable `audit_logs` table with timestamp and IP address.

---

## 🎤 Common Viva Questions & Preparation

**Q1: Why did you use `com.sun.net.httpserver.HttpServer` instead of Spring Boot?**
> *Answer*: "Since this is a Semester III project focusing on core Java fundamentals, `HttpServer` is built directly into the standard JDK. It allows me to demonstrate object-oriented design patterns (like the Decorator pattern for middleware and the Repository pattern for database access) without the abstraction and 'magic' annotations of Spring Boot, making the code 100% transparent and understandable."

**Q2: How does the system prevent leave balance corruption if two leaves are submitted simultaneously?**
> *Answer*: "The system handles balance state transitions through discrete stages: when an application is submitted, the days are immediately placed into the `pending` balance. Upon HOD approval, they transition from `pending` to `used`. If rejected or cancelled, the `pending` days are restored. Every transaction is also recorded in the `balance_transactions` table."

**Q3: How do you calculate the actual days of leave?**
> *Answer*: "The leave calculator iterates through the start and end dates, checks against institutional holidays stored in the database, skips weekends (Saturdays and Sundays), and accommodates half-day increments (0.5 days) at the start or end of the requested leave span."

---

## 📄 License
Created for academic demonstration and Semester III Industry Practice evaluation.
