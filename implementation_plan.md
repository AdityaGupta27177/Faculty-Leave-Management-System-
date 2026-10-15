# Faculty Leave Management System — Implementation Plan

## Architecture Decision

Since this is a **Semester III Java FSD** project, I'll use:

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| **Frontend** | HTML5 + CSS3 + Vanilla JS | Syllabus-aligned, no frameworks |
| **Backend** | Java with `com.sun.net.httpserver.HttpServer` | Lightweight, no Spring Boot needed, pure Java |
| **Database** | PostgreSQL via JDBC | Persistent, relational, industry-standard |
| **Auth** | Google OAuth 2.0 (ID token flow) | Real authentication |
| **Build** | Javac + simple scripts (no Maven/Gradle initially) | Keeps it simple for a student |

### Why `HttpServer` over Spring Boot?
- Built into the JDK (no external dependencies for the HTTP layer)
- Students can understand every line — no magic annotations
- Demonstrates OOP naturally (handlers as classes)
- Perfectly adequate for this scale

### Extensions beyond Sem III (documented):
1. JDBC (database connectivity)
2. Google OAuth token verification
3. Session management with secure cookies
4. JSON parsing (using a small library like `org.json` or `Gson`)

---

## Phase Execution Order

### Phase 1: Project Structure & Database ✏️
- Create directory structure
- Write SQL schema
- Create database setup script
- Create `.gitignore` and `README.md`

### Phase 2: Frontend (HTML/CSS/JS) 🎨
- Login page with Google Sign-In
- Faculty Dashboard
- Leave Application Form
- HOD Dashboard (approval queue)
- Admin Dashboard (user/policy management)
- Responsive CSS design system
- JavaScript: validation, fetch API, DOM manipulation

### Phase 3: Java Backend 🔧
- HttpServer setup with routing
- Model classes (User, LeaveApplication, Department, etc.)
- Repository classes (JDBC database access)
- Service classes (business logic)
- Handler classes (HTTP request handling)
- JSON utilities
- Session management

### Phase 4: Database Integration 🗄️
- JDBC connection pooling
- All CRUD operations
- Prepared statements everywhere

### Phase 5: Google OAuth & Authorization 🔐
- Google Sign-In integration
- Token verification
- Session creation
- Role-based access control

### Phase 6: Business Logic ⚙️
- Leave application workflow (Draft→Submitted→Approved/Rejected)
- Leave balance calculations
- Holiday calendar
- Notifications (in-app)
- Search & Reports
- CSV export

### Phase 7: Testing & Bug Fixes 🧪
### Phase 8: Deployment Prep 🚀
### Phase 9: Documentation 📄

---

## Directory Structure

```
ip project yoo/
├── frontend/
│   ├── index.html              (Landing/Login)
│   ├── dashboard.html          (Faculty Dashboard)
│   ├── apply-leave.html        (Leave Application)
│   ├── my-leaves.html          (Leave History)
│   ├── hod-dashboard.html      (HOD View)
│   ├── admin-dashboard.html    (Admin View)
│   ├── css/
│   │   ├── style.css           (Main stylesheet)
│   │   └── components.css      (Reusable component styles)
│   ├── js/
│   │   ├── app.js              (Main app logic, routing, auth)
│   │   ├── api.js              (Fetch API wrapper)
│   │   ├── validation.js       (Form validation)
│   │   ├── dashboard.js        (Dashboard logic)
│   │   ├── leave-form.js       (Leave application form)
│   │   ├── hod.js              (HOD approval logic)
│   │   ├── admin.js            (Admin panel logic)
│   │   └── utils.js            (Date calculations, formatting)
│   └── assets/
│       └── (icons, images)
├── backend/
│   ├── src/
│   │   ├── Main.java
│   │   ├── server/
│   │   │   ├── AppServer.java
│   │   │   └── StaticFileHandler.java
│   │   ├── models/
│   │   │   ├── User.java
│   │   │   ├── Department.java
│   │   │   ├── LeaveType.java
│   │   │   ├── LeaveApplication.java
│   │   │   ├── LeaveBalance.java
│   │   │   ├── BalanceTransaction.java
│   │   │   ├── ApprovalHistory.java
│   │   │   ├── Holiday.java
│   │   │   ├── Notification.java
│   │   │   └── AuditLog.java
│   │   ├── repositories/
│   │   │   ├── DatabaseManager.java
│   │   │   ├── UserRepository.java
│   │   │   ├── DepartmentRepository.java
│   │   │   ├── LeaveRepository.java
│   │   │   ├── LeaveBalanceRepository.java
│   │   │   ├── HolidayRepository.java
│   │   │   ├── NotificationRepository.java
│   │   │   └── AuditLogRepository.java
│   │   ├── services/
│   │   │   ├── AuthService.java
│   │   │   ├── UserService.java
│   │   │   ├── LeaveService.java
│   │   │   ├── ApprovalService.java
│   │   │   ├── BalanceService.java
│   │   │   ├── ReportService.java
│   │   │   └── NotificationService.java
│   │   ├── handlers/
│   │   │   ├── AuthHandler.java
│   │   │   ├── UserHandler.java
│   │   │   ├── LeaveHandler.java
│   │   │   ├── ApprovalHandler.java
│   │   │   ├── AdminHandler.java
│   │   │   ├── ReportHandler.java
│   │   │   └── NotificationHandler.java
│   │   ├── middleware/
│   │   │   ├── AuthMiddleware.java
│   │   │   └── CorsHandler.java
│   │   └── utils/
│   │       ├── JsonUtil.java
│   │       ├── SessionManager.java
│   │       ├── PasswordUtil.java
│   │       └── DateUtil.java
│   └── lib/
│       ├── postgresql-*.jar
│       └── gson-*.jar
├── database/
│   ├── schema.sql
│   ├── seed.sql
│   └── migrations/
├── .gitignore
├── README.md
├── run.bat (Windows run script)
└── run.sh (Linux/Mac run script)
```
