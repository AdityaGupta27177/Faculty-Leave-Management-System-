-- ============================================================
-- Faculty Leave Management System — Database Schema
-- PostgreSQL
-- ============================================================

-- 1. Departments
CREATE TABLE IF NOT EXISTS departments (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Users (faculty, HOD, admin)
CREATE TABLE IF NOT EXISTS users (
    id              SERIAL PRIMARY KEY,
    google_id       VARCHAR(255) UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    profile_picture TEXT,
    department_id   INTEGER      REFERENCES departments(id),
    designation     VARCHAR(100),
    role            VARCHAR(20)  NOT NULL DEFAULT 'FACULTY'
                        CHECK (role IN ('FACULTY', 'HOD', 'ADMIN')),
    approver_id     INTEGER      REFERENCES users(id),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    is_approved     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Sessions
CREATE TABLE IF NOT EXISTS sessions (
    id              VARCHAR(128) PRIMARY KEY,
    user_id         INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sessions_user    ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires ON sessions(expires_at);

-- 4. Leave types
CREATE TABLE IF NOT EXISTS leave_types (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(80)  NOT NULL UNIQUE,
    code            VARCHAR(20)  NOT NULL UNIQUE,
    description     TEXT,
    requires_document BOOLEAN    NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Leave policies (entitlements per designation per year)
CREATE TABLE IF NOT EXISTS leave_policies (
    id              SERIAL PRIMARY KEY,
    leave_type_id   INTEGER      NOT NULL REFERENCES leave_types(id) ON DELETE CASCADE,
    designation     VARCHAR(100) NOT NULL,
    days_entitled   NUMERIC(5,1) NOT NULL DEFAULT 0,
    carry_forward   BOOLEAN      NOT NULL DEFAULT FALSE,
    max_accumulation NUMERIC(5,1),
    academic_year   VARCHAR(9)   NOT NULL,  -- e.g. '2026-2027'
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(leave_type_id, designation, academic_year)
);

-- 6. Leave applications
CREATE TABLE IF NOT EXISTS leave_applications (
    id              SERIAL PRIMARY KEY,
    application_no  VARCHAR(30)  NOT NULL UNIQUE,
    user_id         INTEGER      NOT NULL REFERENCES users(id),
    leave_type_id   INTEGER      NOT NULL REFERENCES leave_types(id),
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    half_day_start  BOOLEAN      NOT NULL DEFAULT FALSE,
    half_day_end    BOOLEAN      NOT NULL DEFAULT FALSE,
    total_days      NUMERIC(5,1) NOT NULL,
    reason          TEXT         NOT NULL,
    supporting_doc  TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT','SUBMITTED','PENDING','APPROVED',
                                          'REJECTED','CANCELLED','RETURNED')),
    approver_id     INTEGER      REFERENCES users(id),
    submitted_at    TIMESTAMP,
    decided_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dates CHECK (end_date >= start_date)
);
CREATE INDEX IF NOT EXISTS idx_leave_app_user     ON leave_applications(user_id);
CREATE INDEX IF NOT EXISTS idx_leave_app_status   ON leave_applications(status);
CREATE INDEX IF NOT EXISTS idx_leave_app_approver ON leave_applications(approver_id);
CREATE INDEX IF NOT EXISTS idx_leave_app_dates    ON leave_applications(start_date, end_date);

-- 7. Approval history (audit trail for each decision)
CREATE TABLE IF NOT EXISTS approval_history (
    id              SERIAL PRIMARY KEY,
    application_id  INTEGER      NOT NULL REFERENCES leave_applications(id) ON DELETE CASCADE,
    action          VARCHAR(20)  NOT NULL
                        CHECK (action IN ('SUBMITTED','APPROVED','REJECTED',
                                          'RETURNED','CANCELLED','WITHDRAWN')),
    actor_id        INTEGER      NOT NULL REFERENCES users(id),
    comments        TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_approval_hist_app ON approval_history(application_id);

-- 8. Leave balances (per user, leave type, academic year)
CREATE TABLE IF NOT EXISTS leave_balances (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    leave_type_id   INTEGER      NOT NULL REFERENCES leave_types(id) ON DELETE CASCADE,
    academic_year   VARCHAR(9)   NOT NULL,
    total_entitled  NUMERIC(5,1) NOT NULL DEFAULT 0,
    used            NUMERIC(5,1) NOT NULL DEFAULT 0,
    pending         NUMERIC(5,1) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, leave_type_id, academic_year)
);
CREATE INDEX IF NOT EXISTS idx_leave_bal_user ON leave_balances(user_id);

-- 9. Balance transactions (every change to a balance is recorded)
CREATE TABLE IF NOT EXISTS balance_transactions (
    id                  SERIAL PRIMARY KEY,
    balance_id          INTEGER      NOT NULL REFERENCES leave_balances(id) ON DELETE CASCADE,
    application_id      INTEGER      REFERENCES leave_applications(id),
    transaction_type    VARCHAR(20)  NOT NULL
                            CHECK (transaction_type IN ('ENTITLE','DEDUCT','RESTORE',
                                                        'ADJUST','PENDING_ADD','PENDING_REMOVE')),
    days                NUMERIC(5,1) NOT NULL,
    reason              TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. Holidays
CREATE TABLE IF NOT EXISTS holidays (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    holiday_date    DATE         NOT NULL,
    holiday_type    VARCHAR(20)  NOT NULL DEFAULT 'INSTITUTIONAL'
                        CHECK (holiday_type IN ('NATIONAL','STATE','INSTITUTIONAL')),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(holiday_date, name)
);

-- 11. Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    message         TEXT         NOT NULL,
    notif_type      VARCHAR(30)  NOT NULL DEFAULT 'INFO'
                        CHECK (notif_type IN ('INFO','LEAVE_SUBMITTED','LEAVE_APPROVED',
                                               'LEAVE_REJECTED','LEAVE_RETURNED',
                                               'LEAVE_CANCELLED','SYSTEM')),
    reference_id    INTEGER,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notif_user ON notifications(user_id);

-- 12. Audit logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id              SERIAL PRIMARY KEY,
    user_id         INTEGER      REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       INTEGER,
    details         TEXT,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(created_at);

-- 13. Application sequence (for generating application numbers)
CREATE SEQUENCE IF NOT EXISTS app_number_seq START WITH 1001;
