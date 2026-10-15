/**
 * admin.js — Admin dashboard logic
 * Faculty Leave Management System
 */

let allUsers = [];
let allDepartments = [];
let allLeaveTypes = [];
let allPolicies = [];
let allHolidays = [];
let reportData = [];

document.addEventListener('DOMContentLoaded', async () => {
    const user = await requireAuth(['ADMIN']);
    if (!user) return;

    await loadOverview();

    // Search listeners
    document.getElementById('userSearch')?.addEventListener('input',
        Utils.debounce(() => filterUsers(), 300));
    document.getElementById('userRoleFilter')?.addEventListener('change', filterUsers);
});

/* ========== Section Navigation ========== */
function showSection(section) {
    const sections = ['overview', 'users', 'departments', 'leaveTypes', 'policies', 'holidays', 'reports', 'audit'];
    const titles = {
        overview: 'Admin Overview', users: 'User Management', departments: 'Departments',
        leaveTypes: 'Leave Types', policies: 'Leave Policies', holidays: 'Holidays',
        reports: 'Reports', audit: 'Audit Logs'
    };

    sections.forEach(s => {
        const el = document.getElementById(`${s}Section`);
        if (el) el.classList.toggle('hidden', s !== section);
    });

    document.getElementById('sectionTitle').textContent = titles[section] || section;

    // Update sidebar active
    document.querySelectorAll('.nav-item').forEach(item => item.classList.remove('active'));
    event?.target?.closest('.nav-item')?.classList.add('active');

    // Load data for section
    switch (section) {
        case 'overview': loadOverview(); break;
        case 'users': loadUsers(); break;
        case 'departments': loadDepartments(); break;
        case 'leaveTypes': loadLeaveTypes(); break;
        case 'policies': loadPolicies(); break;
        case 'holidays': loadHolidaysList(); break;
        case 'reports': loadReportDepts(); break;
        case 'audit': loadAuditLogs(); break;
    }
}

/* ========== Overview ========== */
async function loadOverview() {
    try {
        const stats = await API.get('/api/admin/stats');
        document.getElementById('adminStats').innerHTML = `
            <div class="stat-card">
                <div class="stat-icon primary">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.total_users || 0}</div>
                    <div class="stat-label">Total Users</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon warning">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.pending_approvals || 0}</div>
                    <div class="stat-label">Pending Approvals</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon success">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.approved_this_month || 0}</div>
                    <div class="stat-label">Approved (This Month)</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon info">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="2" width="16" height="20" rx="2" ry="2"/><line x1="9" y1="22" x2="9" y2="22.01"/><line x1="15" y1="22" x2="15" y2="22.01"/><line x1="8" y1="6" x2="8.01" y2="6"/><line x1="16" y1="6" x2="16.01" y2="6"/><line x1="8" y1="10" x2="8.01" y2="10"/><line x1="16" y1="10" x2="16.01" y2="10"/><line x1="8" y1="14" x2="8.01" y2="14"/><line x1="16" y1="14" x2="16.01" y2="14"/><line x1="8" y1="18" x2="8.01" y2="18"/><line x1="16" y1="18" x2="16.01" y2="18"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.total_departments || 0}</div>
                    <div class="stat-label">Departments</div>
                </div>
            </div>
        `;

        // Recent applications
        const apps = await API.get('/api/admin/applications', { limit: 10 });
        renderAllApplications(apps);
    } catch (e) {
        document.getElementById('adminStats').innerHTML = '';
    }
}

function renderAllApplications(apps) {
    const container = document.getElementById('allApplicationsList');
    if (!apps || apps.length === 0) {
        container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No applications found.</p></div>';
        return;
    }
    container.innerHTML = `
        <div class="table-container">
            <table class="data-table">
                <thead>
                    <tr><th>ID</th><th>Faculty</th><th>Dept</th><th>Type</th><th>Dates</th><th>Days</th><th>Status</th></tr>
                </thead>
                <tbody>
                    ${apps.map(a => {
                        const badge = Utils.statusBadge(a.status);
                        return `<tr>
                            <td style="font-weight:500;">${Utils.escapeHtml(a.application_no)}</td>
                            <td>${Utils.escapeHtml(a.faculty_name)}</td>
                            <td class="text-sm">${Utils.escapeHtml(a.department_name || '—')}</td>
                            <td>${Utils.escapeHtml(a.leave_type_name)}</td>
                            <td class="text-sm">${Utils.formatDate(a.start_date)} – ${Utils.formatDate(a.end_date)}</td>
                            <td>${a.total_days}</td>
                            <td><span class="badge ${badge.className}">${badge.label}</span></td>
                        </tr>`;
                    }).join('')}
                </tbody>
            </table>
        </div>
    `;
}

async function exportAllApplications() {
    try {
        const apps = await API.get('/api/admin/applications', { limit: 1000 });
        if (!apps.length) { showToast('info', 'No Data', 'No applications to export.'); return; }
        Utils.exportCSV(apps.map(a => ({
            'App No': a.application_no, 'Faculty': a.faculty_name, 'Department': a.department_name,
            'Type': a.leave_type_name, 'From': a.start_date, 'To': a.end_date,
            'Days': a.total_days, 'Status': a.status
        })), 'all_applications.csv');
        showToast('success', 'Exported', 'Applications exported to CSV.');
    } catch (e) { showToast('error', 'Error', e.message); }
}

/* ========== Users ========== */
async function loadUsers() {
    try {
        allUsers = await API.get('/api/admin/users');
        renderUsers(allUsers);
    } catch (e) {
        document.getElementById('usersList').innerHTML = '<p class="text-muted" style="padding:16px;">Unable to load users.</p>';
    }
}

function filterUsers() {
    const search = (document.getElementById('userSearch')?.value || '').toLowerCase();
    const role = document.getElementById('userRoleFilter')?.value || '';
    let filtered = allUsers;
    if (search) filtered = filtered.filter(u => u.name.toLowerCase().includes(search) || u.email.toLowerCase().includes(search));
    if (role) filtered = filtered.filter(u => u.role === role);
    renderUsers(filtered);
}

function renderUsers(users) {
    const container = document.getElementById('usersList');
    if (!users.length) { container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No users found.</p></div>'; return; }
    container.innerHTML = `
        <div class="table-container">
            <table class="data-table">
                <thead><tr><th>Name</th><th>Email</th><th>Department</th><th>Designation</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>
                    ${users.map(u => {
                        const rb = Utils.roleBadge(u.role);
                        return `<tr>
                            <td style="font-weight:500;">${Utils.escapeHtml(u.name)}</td>
                            <td class="text-sm">${Utils.escapeHtml(u.email)}</td>
                            <td>${Utils.escapeHtml(u.department_name || '—')}</td>
                            <td>${Utils.escapeHtml(u.designation || '—')}</td>
                            <td><span class="role-badge ${rb.className}">${rb.label}</span></td>
                            <td>${u.is_approved ? '<span class="badge badge-approved">Active</span>' : '<span class="badge badge-pending">Pending</span>'}</td>
                            <td>
                                <button class="btn btn-sm btn-ghost" onclick="openUserModal(${u.id})" title="Edit User">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                                </button>
                                ${!u.is_approved ? `
                                <button class="btn btn-sm btn-success" onclick="approveUser(${u.id})" title="Approve User" style="display:inline-flex; align-items:center; gap:4px; padding:4px 8px;">
                                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                                    Approve
                                </button>` : ''}
                            </td>
                        </tr>`;
                    }).join('')}
                </tbody>
            </table>
        </div>
    `;
}

async function openUserModal(userId) {
    const modal = document.getElementById('adminModal');
    const title = document.getElementById('adminModalTitle');
    const body = document.getElementById('adminModalBody');
    const footer = document.getElementById('adminModalFooter');

    let user = userId ? allUsers.find(u => u.id === userId) : null;
    if (userId && !user) {
        try { user = await API.get(`/api/admin/users/${userId}`); } catch (e) {}
    }

    // Load departments for dropdown
    let depts = allDepartments.length ? allDepartments : [];
    if (!depts.length) { try { depts = await API.get('/api/departments'); } catch (e) {} }

    // Load users for approver dropdown
    let approvers = [];
    try { approvers = await API.get('/api/admin/users', { role: 'HOD' }); } catch (e) {}

    title.textContent = user ? 'Edit User' : 'Add User';
    body.innerHTML = `
        <form id="userForm">
            <div class="form-group">
                <label class="form-label">Name <span class="required">*</span></label>
                <input class="form-input" name="name" value="${Utils.escapeHtml(user?.name || '')}" required>
            </div>
            <div class="form-group">
                <label class="form-label">Email <span class="required">*</span></label>
                <input class="form-input" name="email" type="email" value="${Utils.escapeHtml(user?.email || '')}" ${user ? 'readonly' : ''} required>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label">Department <span class="required">*</span></label>
                    <select class="form-select" name="department_id" required>
                        <option value="">Select…</option>
                        ${depts.map(d => `<option value="${d.id}" ${user?.department_id == d.id ? 'selected' : ''}>${Utils.escapeHtml(d.name)}</option>`).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Designation <span class="required">*</span></label>
                    <input class="form-input" name="designation" value="${Utils.escapeHtml(user?.designation || '')}" placeholder="e.g. Assistant Professor">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label">Role <span class="required">*</span></label>
                    <select class="form-select" name="role" required>
                        <option value="FACULTY" ${user?.role === 'FACULTY' ? 'selected' : ''}>Faculty</option>
                        <option value="HOD" ${user?.role === 'HOD' ? 'selected' : ''}>HOD</option>
                        <option value="ADMIN" ${user?.role === 'ADMIN' ? 'selected' : ''}>Admin</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Approver</label>
                    <select class="form-select" name="approver_id">
                        <option value="">None</option>
                        ${approvers.map(a => `<option value="${a.id}" ${user?.approver_id == a.id ? 'selected' : ''}>${Utils.escapeHtml(a.name)}</option>`).join('')}
                    </select>
                </div>
            </div>
            <div class="form-group">
                <label class="form-checkbox">
                    <input type="checkbox" name="is_approved" ${user?.is_approved !== false ? 'checked' : ''}>
                    Account approved (active)
                </label>
            </div>
        </form>
    `;
    footer.innerHTML = `
        <button class="btn btn-secondary" onclick="closeAdminModal()">Cancel</button>
        <button class="btn btn-primary" onclick="saveUser(${userId || 'null'})" style="display:inline-flex; align-items:center; gap:6px;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            Save User
        </button>
    `;
    modal.classList.add('active');
}

async function saveUser(userId) {
    const form = document.getElementById('userForm');
    const data = {
        name: form.querySelector('[name=name]').value.trim(),
        email: form.querySelector('[name=email]').value.trim(),
        department_id: parseInt(form.querySelector('[name=department_id]').value) || null,
        designation: form.querySelector('[name=designation]').value.trim(),
        role: form.querySelector('[name=role]').value,
        approver_id: parseInt(form.querySelector('[name=approver_id]').value) || null,
        is_approved: form.querySelector('[name=is_approved]').checked
    };

    const validation = Validator.validateUser(data);
    if (!validation.valid) { Validator.showErrors(validation.errors, form); return; }

    try {
        if (userId) {
            await API.put(`/api/admin/users/${userId}`, data);
            showToast('success', 'Updated', 'User updated successfully.');
        } else {
            await API.post('/api/admin/users', data);
            showToast('success', 'Created', 'User created successfully.');
        }
        closeAdminModal();
        loadUsers();
    } catch (e) {
        showToast('error', 'Error', e.message);
    }
}

async function approveUser(id) {
    try {
        await API.put(`/api/admin/users/${id}`, { is_approved: true });
        showToast('success', 'Approved', 'User account has been approved.');
        loadUsers();
    } catch (e) { showToast('error', 'Error', e.message); }
}

/* ========== Departments ========== */
async function loadDepartments() {
    try {
        allDepartments = await API.get('/api/departments');
        const container = document.getElementById('deptList');
        if (!allDepartments.length) { container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No departments.</p></div>'; return; }
        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead><tr><th>Name</th><th>Code</th><th>Status</th><th>Actions</th></tr></thead>
                    <tbody>
                        ${allDepartments.map(d => `<tr>
                            <td style="font-weight:500;">${Utils.escapeHtml(d.name)}</td>
                            <td>${Utils.escapeHtml(d.code)}</td>
                            <td>${d.is_active !== false ? '<span class="badge badge-approved">Active</span>' : '<span class="badge badge-rejected">Inactive</span>'}</td>
                            <td>
                                <button class="btn btn-sm btn-ghost" onclick="openDeptModal(${d.id})" title="Edit Department">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                                </button>
                            </td>
                        </tr>`).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) { document.getElementById('deptList').innerHTML = '<p class="text-muted" style="padding:16px;">Error loading.</p>'; }
}

function openDeptModal(deptId) {
    const dept = deptId ? allDepartments.find(d => d.id === deptId) : null;
    const body = document.getElementById('adminModalBody');
    document.getElementById('adminModalTitle').textContent = dept ? 'Edit Department' : 'Add Department';
    body.innerHTML = `
        <form id="deptForm">
            <div class="form-group">
                <label class="form-label">Name <span class="required">*</span></label>
                <input class="form-input" name="name" value="${Utils.escapeHtml(dept?.name || '')}">
            </div>
            <div class="form-group">
                <label class="form-label">Code <span class="required">*</span></label>
                <input class="form-input" name="code" value="${Utils.escapeHtml(dept?.code || '')}" style="text-transform:uppercase;">
            </div>
        </form>
    `;
    document.getElementById('adminModalFooter').innerHTML = `
        <button class="btn btn-secondary" onclick="closeAdminModal()">Cancel</button>
        <button class="btn btn-primary" onclick="saveDept(${deptId || 'null'})" style="display:inline-flex; align-items:center; gap:6px;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            Save Department
        </button>
    `;
    document.getElementById('adminModal').classList.add('active');
}

async function saveDept(id) {
    const form = document.getElementById('deptForm');
    const data = { name: form.querySelector('[name=name]').value.trim(), code: form.querySelector('[name=code]').value.trim().toUpperCase() };
    const v = Validator.validateDepartment(data);
    if (!v.valid) { Validator.showErrors(v.errors, form); return; }
    try {
        if (id) await API.put(`/api/admin/departments/${id}`, data);
        else await API.post('/api/admin/departments', data);
        showToast('success', 'Saved', 'Department saved.'); closeAdminModal(); loadDepartments();
    } catch (e) { showToast('error', 'Error', e.message); }
}

/* ========== Leave Types ========== */
async function loadLeaveTypes() {
    try {
        allLeaveTypes = await API.get('/api/leave-types');
        const container = document.getElementById('leaveTypesList');
        if (!allLeaveTypes.length) { container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No leave types.</p></div>'; return; }
        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead><tr><th>Name</th><th>Code</th><th>Requires Doc</th><th>Status</th><th>Actions</th></tr></thead>
                    <tbody>
                        ${allLeaveTypes.map(t => `<tr>
                            <td style="font-weight:500;">${Utils.escapeHtml(t.name)}</td>
                            <td>${Utils.escapeHtml(t.code)}</td>
                            <td>${t.requires_document ? 'Yes' : 'No'}</td>
                            <td>${t.is_active !== false ? '<span class="badge badge-approved">Active</span>' : '<span class="badge badge-rejected">Inactive</span>'}</td>
                            <td>
                                <button class="btn btn-sm btn-ghost" onclick="openLeaveTypeModal(${t.id})" title="Edit Leave Type">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                                </button>
                            </td>
                        </tr>`).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) { document.getElementById('leaveTypesList').innerHTML = '<p class="text-muted" style="padding:16px;">Error.</p>'; }
}

function openLeaveTypeModal(id) {
    const lt = id ? allLeaveTypes.find(t => t.id === id) : null;
    document.getElementById('adminModalTitle').textContent = lt ? 'Edit Leave Type' : 'Add Leave Type';
    document.getElementById('adminModalBody').innerHTML = `
        <form id="leaveTypeForm">
            <div class="form-group"><label class="form-label">Name <span class="required">*</span></label>
                <input class="form-input" name="name" value="${Utils.escapeHtml(lt?.name || '')}"></div>
            <div class="form-group"><label class="form-label">Code <span class="required">*</span></label>
                <input class="form-input" name="code" value="${Utils.escapeHtml(lt?.code || '')}"></div>
            <div class="form-group"><label class="form-label">Description</label>
                <textarea class="form-textarea" name="description" rows="2">${Utils.escapeHtml(lt?.description || '')}</textarea></div>
            <div class="form-group"><label class="form-checkbox">
                <input type="checkbox" name="requires_document" ${lt?.requires_document ? 'checked' : ''}> Requires supporting document
            </label></div>
        </form>
    `;
    document.getElementById('adminModalFooter').innerHTML = `
        <button class="btn btn-secondary" onclick="closeAdminModal()">Cancel</button>
        <button class="btn btn-primary" onclick="saveLeaveType(${id || 'null'})" style="display:inline-flex; align-items:center; gap:6px;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            Save Leave Type
        </button>
    `;
    document.getElementById('adminModal').classList.add('active');
}

async function saveLeaveType(id) {
    const form = document.getElementById('leaveTypeForm');
    const data = { name: form.querySelector('[name=name]').value.trim(), code: form.querySelector('[name=code]').value.trim().toUpperCase(), description: form.querySelector('[name=description]').value.trim(), requires_document: form.querySelector('[name=requires_document]').checked };
    const v = Validator.validateLeaveType(data);
    if (!v.valid) { Validator.showErrors(v.errors, form); return; }
    try {
        if (id) await API.put(`/api/admin/leave-types/${id}`, data);
        else await API.post('/api/admin/leave-types', data);
        showToast('success', 'Saved', 'Leave type saved.'); closeAdminModal(); loadLeaveTypes();
    } catch (e) { showToast('error', 'Error', e.message); }
}

/* ========== Policies ========== */
async function loadPolicies() {
    try {
        allPolicies = await API.get('/api/admin/policies');
        const container = document.getElementById('policiesList');
        if (!allPolicies.length) { container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No policies configured.</p></div>'; return; }
        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead><tr><th>Leave Type</th><th>Designation</th><th>Days Entitled</th><th>Carry Forward</th><th>Year</th><th>Actions</th></tr></thead>
                    <tbody>
                        ${allPolicies.map(p => `<tr>
                            <td style="font-weight:500;">${Utils.escapeHtml(p.leave_type_name || '')}</td>
                            <td>${Utils.escapeHtml(p.designation)}</td>
                            <td>${p.days_entitled}</td>
                            <td>${p.carry_forward ? 'Yes' : '—'}</td>
                            <td>${Utils.escapeHtml(p.academic_year)}</td>
                            <td>
                                <button class="btn btn-sm btn-ghost" onclick="openPolicyModal(${p.id})" title="Edit Policy">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                                </button>
                            </td>
                        </tr>`).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) { document.getElementById('policiesList').innerHTML = '<p class="text-muted" style="padding:16px;">Error.</p>'; }
}

function openPolicyModal(id) {
    const p = id ? allPolicies.find(x => x.id === id) : null;
    const ltOptions = allLeaveTypes.map(t => `<option value="${t.id}" ${p?.leave_type_id == t.id ? 'selected' : ''}>${t.name}</option>`).join('');
    document.getElementById('adminModalTitle').textContent = p ? 'Edit Policy' : 'Add Policy';
    document.getElementById('adminModalBody').innerHTML = `
        <form id="policyForm">
            <div class="form-group"><label class="form-label">Leave Type</label><select class="form-select" name="leave_type_id"><option value="">Select…</option>${ltOptions}</select></div>
            <div class="form-group"><label class="form-label">Designation</label><input class="form-input" name="designation" value="${Utils.escapeHtml(p?.designation || '')}" placeholder="e.g. Assistant Professor"></div>
            <div class="form-row">
                <div class="form-group"><label class="form-label">Days Entitled</label><input class="form-input" name="days_entitled" type="number" step="0.5" value="${p?.days_entitled || 0}"></div>
                <div class="form-group"><label class="form-label">Academic Year</label><input class="form-input" name="academic_year" value="${Utils.escapeHtml(p?.academic_year || Utils.currentAcademicYear())}" placeholder="2026-2027"></div>
            </div>
            <div class="form-group"><label class="form-checkbox"><input type="checkbox" name="carry_forward" ${p?.carry_forward ? 'checked' : ''}> Allow carry forward</label></div>
        </form>
    `;
    document.getElementById('adminModalFooter').innerHTML = `
        <button class="btn btn-secondary" onclick="closeAdminModal()">Cancel</button>
        <button class="btn btn-primary" onclick="savePolicy(${id || 'null'})" style="display:inline-flex; align-items:center; gap:6px;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            Save Policy
        </button>
    `;
    document.getElementById('adminModal').classList.add('active');
}

async function savePolicy(id) {
    const form = document.getElementById('policyForm');
    const data = { leave_type_id: parseInt(form.querySelector('[name=leave_type_id]').value), designation: form.querySelector('[name=designation]').value.trim(), days_entitled: parseFloat(form.querySelector('[name=days_entitled]').value), academic_year: form.querySelector('[name=academic_year]').value.trim(), carry_forward: form.querySelector('[name=carry_forward]').checked };
    try {
        if (id) await API.put(`/api/admin/policies/${id}`, data);
        else await API.post('/api/admin/policies', data);
        showToast('success', 'Saved', 'Policy saved.'); closeAdminModal(); loadPolicies();
    } catch (e) { showToast('error', 'Error', e.message); }
}

/* ========== Holidays ========== */
async function loadHolidaysList() {
    try {
        allHolidays = await API.get('/api/holidays');
        const container = document.getElementById('holidaysList');
        if (!allHolidays.length) { container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No holidays.</p></div>'; return; }
        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead><tr><th>Date</th><th>Name</th><th>Type</th><th>Actions</th></tr></thead>
                    <tbody>
                        ${allHolidays.map(h => `<tr>
                            <td>${Utils.formatDate(h.holiday_date, 'long')}</td>
                            <td style="font-weight:500;">${Utils.escapeHtml(h.name)}</td>
                            <td><span class="badge badge-submitted">${h.holiday_type}</span></td>
                            <td>
                                <button class="btn btn-sm btn-ghost" onclick="deleteHoliday(${h.id})" title="Delete Holiday" style="color:var(--danger-500);">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                                </button>
                            </td>
                        </tr>`).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) { document.getElementById('holidaysList').innerHTML = '<p class="text-muted" style="padding:16px;">Error.</p>'; }
}

function openHolidayModal() {
    document.getElementById('adminModalTitle').textContent = 'Add Holiday';
    document.getElementById('adminModalBody').innerHTML = `
        <form id="holidayForm">
            <div class="form-group"><label class="form-label">Name</label><input class="form-input" name="name" placeholder="e.g. Republic Day"></div>
            <div class="form-group"><label class="form-label">Date</label><input class="form-input" name="holiday_date" type="date"></div>
            <div class="form-group"><label class="form-label">Type</label>
                <select class="form-select" name="holiday_type">
                    <option value="NATIONAL">National</option><option value="STATE">State</option><option value="INSTITUTIONAL">Institutional</option>
                </select></div>
        </form>
    `;
    document.getElementById('adminModalFooter').innerHTML = `
        <button class="btn btn-secondary" onclick="closeAdminModal()">Cancel</button>
        <button class="btn btn-primary" onclick="saveHoliday()" style="display:inline-flex; align-items:center; gap:6px;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/><polyline points="17 21 17 13 7 13 7 21"/><polyline points="7 3 7 8 15 8"/></svg>
            Save Holiday
        </button>
    `;
    document.getElementById('adminModal').classList.add('active');
}

async function saveHoliday() {
    const form = document.getElementById('holidayForm');
    const data = { name: form.querySelector('[name=name]').value.trim(), holiday_date: form.querySelector('[name=holiday_date]').value, holiday_type: form.querySelector('[name=holiday_type]').value };
    if (!data.name || !data.holiday_date) { showToast('warning', 'Validation', 'Name and date are required.'); return; }
    try {
        await API.post('/api/admin/holidays', data);
        showToast('success', 'Added', 'Holiday added.'); closeAdminModal(); loadHolidaysList();
    } catch (e) { showToast('error', 'Error', e.message); }
}

async function deleteHoliday(id) {
    if (!await Utils.confirm('Delete Holiday?', 'Are you sure?', 'Delete', 'danger')) return;
    try { await API.delete(`/api/admin/holidays/${id}`); showToast('success', 'Deleted', 'Holiday removed.'); loadHolidaysList(); }
    catch (e) { showToast('error', 'Error', e.message); }
}

/* ========== Reports ========== */
async function loadReportDepts() {
    try {
        const depts = await API.get('/api/departments');
        const select = document.getElementById('reportDept');
        if (select.options.length <= 1) {
            depts.forEach(d => { const o = document.createElement('option'); o.value = d.id; o.textContent = d.name; select.appendChild(o); });
        }
    } catch (e) {}
}

async function generateReport() {
    const type = document.getElementById('reportType').value;
    const dept = document.getElementById('reportDept').value;
    const container = document.getElementById('reportContent');
    container.innerHTML = '<div class="empty-state"><div class="spinner"></div></div>';

    try {
        reportData = await API.get('/api/admin/reports', { type, department_id: dept });
        if (!reportData || !reportData.length) { container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No data for the selected report.</p></div>'; return; }

        const headers = Object.keys(reportData[0]);
        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead><tr>${headers.map(h => `<th>${Utils.escapeHtml(h)}</th>`).join('')}</tr></thead>
                    <tbody>${reportData.map(row => `<tr>${headers.map(h => `<td>${Utils.escapeHtml(String(row[h] ?? ''))}</td>`).join('')}</tr>`).join('')}</tbody>
                </table>
            </div>
        `;
    } catch (e) { container.innerHTML = `<p class="text-muted" style="padding:16px;">${e.message}</p>`; }
}

function exportReport() {
    if (!reportData?.length) { showToast('info', 'No Data', 'Generate a report first.'); return; }
    Utils.exportCSV(reportData, 'leave_report.csv');
    showToast('success', 'Exported', 'Report exported.');
}

/* ========== Audit Logs ========== */
async function loadAuditLogs() {
    const container = document.getElementById('auditList');
    try {
        const logs = await API.get('/api/admin/audit-logs', { limit: 50 });
        if (!logs.length) { container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No audit entries.</p></div>'; return; }
        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead><tr><th>Time</th><th>User</th><th>Action</th><th>Entity</th><th>Details</th></tr></thead>
                    <tbody>${logs.map(l => `<tr>
                        <td class="text-sm">${Utils.formatDate(l.created_at, 'time')}</td>
                        <td>${Utils.escapeHtml(l.user_name || '—')}</td>
                        <td style="font-weight:500;">${Utils.escapeHtml(l.action)}</td>
                        <td class="text-sm">${Utils.escapeHtml(l.entity_type || '')} ${l.entity_id || ''}</td>
                        <td class="text-sm truncate" style="max-width:250px;">${Utils.escapeHtml(l.details || '')}</td>
                    </tr>`).join('')}</tbody>
                </table>
            </div>
        `;
    } catch (e) { container.innerHTML = '<p class="text-muted" style="padding:16px;">Error loading audit logs.</p>'; }
}

function closeAdminModal() { document.getElementById('adminModal').classList.remove('active'); }
