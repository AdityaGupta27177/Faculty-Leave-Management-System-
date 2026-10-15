-- ============================================================
-- Seed data — run AFTER schema.sql
-- Provides default departments, leave types, and one admin user.
-- Does NOT delete existing data.
-- ============================================================

-- Default departments
INSERT INTO departments (name, code) VALUES
    ('Computer Science', 'CS'),
    ('Information Technology', 'IT'),
    ('Electronics', 'ECE'),
    ('Mechanical Engineering', 'ME'),
    ('Civil Engineering', 'CE'),
    ('Mathematics', 'MATH'),
    ('Physics', 'PHY')
ON CONFLICT (code) DO NOTHING;

-- Default leave types
INSERT INTO leave_types (name, code, description, requires_document) VALUES
    ('Casual Leave',       'CL',  'Short-duration leave for personal reasons',               FALSE),
    ('Sick Leave',         'SL',  'Leave due to illness or medical reasons',                  TRUE),
    ('Earned Leave',       'EL',  'Pre-planned leave earned through service',                 FALSE),
    ('Academic Duty Leave','ADL', 'Leave for academic conferences, workshops, or seminars',   FALSE),
    ('Maternity Leave',    'ML',  'Leave for maternity purposes',                             TRUE),
    ('Unpaid Leave',       'UPL', 'Leave without pay when other leave types are exhausted',   FALSE)
ON CONFLICT (code) DO NOTHING;

-- Default leave policies for 'Professor' designation, 2026-2027
INSERT INTO leave_policies (leave_type_id, designation, days_entitled, carry_forward, academic_year)
SELECT lt.id, 'Professor', 
    CASE lt.code
        WHEN 'CL'  THEN 12
        WHEN 'SL'  THEN 10
        WHEN 'EL'  THEN 15
        WHEN 'ADL' THEN 10
        WHEN 'ML'  THEN 180
        WHEN 'UPL' THEN 30
    END,
    CASE lt.code WHEN 'EL' THEN TRUE ELSE FALSE END,
    '2026-2027'
FROM leave_types lt
WHERE lt.code IN ('CL','SL','EL','ADL','ML','UPL')
ON CONFLICT (leave_type_id, designation, academic_year) DO NOTHING;

-- Default leave policies for 'Associate Professor' designation, 2026-2027
INSERT INTO leave_policies (leave_type_id, designation, days_entitled, carry_forward, academic_year)
SELECT lt.id, 'Associate Professor',
    CASE lt.code
        WHEN 'CL'  THEN 12
        WHEN 'SL'  THEN 10
        WHEN 'EL'  THEN 12
        WHEN 'ADL' THEN 8
        WHEN 'ML'  THEN 180
        WHEN 'UPL' THEN 20
    END,
    CASE lt.code WHEN 'EL' THEN TRUE ELSE FALSE END,
    '2026-2027'
FROM leave_types lt
WHERE lt.code IN ('CL','SL','EL','ADL','ML','UPL')
ON CONFLICT (leave_type_id, designation, academic_year) DO NOTHING;

-- Default leave policies for 'Assistant Professor' designation, 2026-2027
INSERT INTO leave_policies (leave_type_id, designation, days_entitled, carry_forward, academic_year)
SELECT lt.id, 'Assistant Professor',
    CASE lt.code
        WHEN 'CL'  THEN 10
        WHEN 'SL'  THEN 8
        WHEN 'EL'  THEN 10
        WHEN 'ADL' THEN 5
        WHEN 'ML'  THEN 180
        WHEN 'UPL' THEN 15
    END,
    CASE lt.code WHEN 'EL' THEN TRUE ELSE FALSE END,
    '2026-2027'
FROM leave_types lt
WHERE lt.code IN ('CL','SL','EL','ADL','ML','UPL')
ON CONFLICT (leave_type_id, designation, academic_year) DO NOTHING;

-- Default holidays for 2026-2027
INSERT INTO holidays (name, holiday_date, holiday_type) VALUES
    ('Republic Day',          '2027-01-26', 'NATIONAL'),
    ('Holi',                  '2027-03-14', 'NATIONAL'),
    ('Good Friday',           '2027-03-26', 'NATIONAL'),
    ('Independence Day',      '2026-08-15', 'NATIONAL'),
    ('Gandhi Jayanti',        '2026-10-02', 'NATIONAL'),
    ('Dussehra',              '2026-10-12', 'NATIONAL'),
    ('Diwali',                '2026-11-01', 'NATIONAL'),
    ('Christmas',             '2026-12-25', 'NATIONAL'),
    ('New Year',              '2027-01-01', 'INSTITUTIONAL'),
    ('Foundation Day',        '2027-02-15', 'INSTITUTIONAL')
ON CONFLICT (holiday_date, name) DO NOTHING;

-- Default sample users for evaluation and testing
-- Passwordless / Google-ready / Dev-login compatible
INSERT INTO users (id, email, name, department_id, designation, role, is_active, is_approved) VALUES
    (1, 'admin@institution.edu', 'Prof. Albus Dumbledore', 1, 'Professor', 'ADMIN', TRUE, TRUE),
    (2, 'hod.cs@institution.edu', 'Dr. Alan Turing', 1, 'Professor', 'HOD', TRUE, TRUE),
    (3, 'faculty.cs@institution.edu', 'Dr. Ada Lovelace', 1, 'Associate Professor', 'FACULTY', TRUE, TRUE),
    (4, 'faculty.it@institution.edu', 'Prof. Grace Hopper', 2, 'Assistant Professor', 'FACULTY', TRUE, TRUE)
ON CONFLICT (id) DO UPDATE SET is_active = TRUE, is_approved = TRUE;

-- Update approvers (Faculty reports to HOD)
UPDATE users SET approver_id = 2 WHERE id IN (3, 4);

-- Ensure user ID sequence is ahead of inserted IDs
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));

-- Initialize leave balances for sample users for 2026-2027
INSERT INTO leave_balances (user_id, leave_type_id, academic_year, total_entitled, used, pending)
SELECT u.id, lp.leave_type_id, '2026-2027', lp.days_entitled, 0, 0
FROM users u
JOIN leave_policies lp ON u.designation = lp.designation AND lp.academic_year = '2026-2027'
WHERE u.id IN (1, 2, 3, 4)
ON CONFLICT (user_id, leave_type_id, academic_year) DO NOTHING;

