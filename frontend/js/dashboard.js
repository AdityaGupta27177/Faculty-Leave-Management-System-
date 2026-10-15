/**
 * dashboard.js — Faculty dashboard logic
 * Faculty Leave Management System
 */

document.addEventListener('DOMContentLoaded', async () => {
    const user = await requireAuth(['FACULTY', 'HOD', 'ADMIN']);
    if (!user) return;

    // Update welcome message
    let displayName = user.name || 'Faculty';
    const parts = displayName.trim().split(/\s+/);
    if (parts.length > 1 && (parts[0].toLowerCase() === 'dr.' || parts[0].toLowerCase() === 'prof.')) {
        displayName = `${parts[0]} ${parts[1]}`;
    } else if (parts.length > 0) {
        displayName = parts[0];
    }
    document.getElementById('welcomeTitle').textContent = `Welcome back, ${displayName}!`;
    document.getElementById('academicYear').textContent = Utils.currentAcademicYear();

    // Load dashboard data
    await Promise.all([
        loadLeaveBalances(),
        loadStatusSummary(),
        loadRecentApplications()
    ]);
});

async function loadLeaveBalances() {
    const grid = document.getElementById('balanceGrid');
    try {
        const balances = await API.get('/api/leave/balances');

        if (!balances || balances.length === 0) {
            grid.innerHTML = `
                <div class="balance-card" style="grid-column: 1/-1;">
                    <div class="empty-state" style="padding: 24px;">
                        <p class="empty-state-text">No leave balances configured. Contact administrator.</p>
                    </div>
                </div>
            `;
            return;
        }

        grid.innerHTML = balances.map(b => {
            const remaining = b.total_entitled - b.used - b.pending;
            const usagePercent = b.total_entitled > 0
                ? Math.round((b.used / b.total_entitled) * 100) : 0;
            const progressClass = usagePercent > 80 ? 'danger' : usagePercent > 50 ? 'warning' : '';

            return `
                <div class="balance-card">
                    <div class="balance-card-head">
                        <div class="balance-type">${Utils.escapeHtml(b.leave_type_name || b.code)}</div>
                        <span class="balance-code-badge">${Utils.escapeHtml(b.code || '')}</span>
                    </div>
                    <div class="balance-figure-cluster">
                        <span class="balance-number">${remaining}</span>
                        <div class="balance-unit-meta">
                            <span>Available Days</span>
                            <span class="balance-allocated">Allocated: ${b.total_entitled}d</span>
                        </div>
                    </div>
                    <div class="balance-gauge-track">
                        <div class="balance-gauge-fill ${progressClass}" style="width: ${usagePercent}%;"></div>
                    </div>
                    <div class="balance-sub-metrics">
                        <span class="metric-item">
                            <span class="metric-dot"></span>
                            <span>Utilized: <strong>${b.used}</strong>d</span>
                        </span>
                        <span class="metric-item">
                            <span class="metric-dot ${b.pending > 0 ? 'in-review' : ''}"></span>
                            <span>In Review: <strong>${b.pending}</strong>d</span>
                        </span>
                    </div>
                </div>
            `;
        }).join('');
    } catch (e) {
        grid.innerHTML = `<p class="text-muted text-sm" style="padding:16px;">Unable to load leave balances.</p>`;
    }
}

async function loadStatusSummary() {
    const container = document.getElementById('statusSummary');
    try {
        const summary = await API.get('/api/leave/summary');
        const pendingCount = summary.pending || 0;

        container.innerHTML = `
            <div class="stat-card">
                <div class="stat-icon info">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${summary.total || 0}</div>
                    <div class="stat-label">Total Applications Filed</div>
                </div>
            </div>
            <div class="stat-card" style="${pendingCount > 0 ? 'background:#FFFDF7; border-color:#EFE2C2;' : ''}">
                <div class="stat-icon warning">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value" style="${pendingCount > 0 ? 'color:var(--warning-text);' : ''}">${pendingCount}</div>
                    <div class="stat-label">Pending Institutional Review</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon success">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${summary.approved || 0}</div>
                    <div class="stat-label">Approved & Sanctioned</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon danger">
                    <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${summary.rejected || 0}</div>
                    <div class="stat-label">Declined / Returned</div>
                </div>
            </div>
        `;
    } catch (e) {
        container.innerHTML = '';
    }
}

async function loadRecentApplications() {
    const container = document.getElementById('recentApplications');
    try {
        const apps = await API.get('/api/leave/my', { limit: 5 });

        if (!apps || apps.length === 0) {
            container.innerHTML = `
                <div class="journal-empty">
                    <div class="empty-state-icon" style="color:var(--text-muted); opacity:0.6;">
                        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
                    </div>
                    <div class="empty-state-content">
                        <div style="font-weight:600; font-size:0.9rem; color:var(--text-primary); margin-bottom:2px;">No active applications on record</div>
                        <div style="font-size:0.78rem; color:var(--text-secondary);">Your submitted leave applications and sanctioning history will be logged here.</div>
                    </div>
                    <a href="apply-leave.html" class="btn btn-sm btn-outline">Apply for Leave</a>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="journal-table-wrap">
                ${apps.map(app => {
                    const badge = Utils.statusBadge(app.status);
                    return `
                        <a href="my-leaves.html?id=${app.id}" class="journal-row" title="View application details">
                            <span class="journal-ref">${app.application_no}</span>
                            <span class="journal-type">${Utils.escapeHtml(app.leave_type_name)}</span>
                            <span class="journal-period">${Utils.formatDate(app.start_date)} – ${Utils.formatDate(app.end_date)}</span>
                            <span class="journal-days">${app.total_days} day${app.total_days !== 1 ? 's' : ''}</span>
                            <div><span class="badge ${badge.className}">${badge.label}</span></div>
                            <span class="journal-arrow">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
                            </span>
                        </a>
                    `;
                }).join('')}
            </div>
        `;
    } catch (e) {
        container.innerHTML = `<p class="text-muted text-sm" style="padding:16px;">Unable to load applications.</p>`;
    }
}

function showCalendar() {
    const modal = document.getElementById('calendarModal');
    modal.classList.add('active');
    loadCalendarData();
}

async function loadCalendarData() {
    const body = document.getElementById('calendarBody');
    try {
        const data = await API.get('/api/leave/calendar');
        if (!data || data.length === 0) {
            body.innerHTML = '<p class="text-muted">No upcoming leave entries.</p>';
            return;
        }

        body.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Faculty</th>
                            <th>Type</th>
                            <th>From</th>
                            <th>To</th>
                            <th>Days</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${data.map(item => {
                            const badge = Utils.statusBadge(item.status);
                            return `
                                <tr>
                                    <td>${Utils.escapeHtml(item.faculty_name)}</td>
                                    <td>${Utils.escapeHtml(item.leave_type_name)}</td>
                                    <td>${Utils.formatDate(item.start_date)}</td>
                                    <td>${Utils.formatDate(item.end_date)}</td>
                                    <td>${item.total_days}</td>
                                    <td><span class="badge ${badge.className}">${badge.label}</span></td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) {
        body.innerHTML = '<p class="text-muted">Unable to load calendar data.</p>';
    }
}

function showHolidays() {
    const modal = document.getElementById('holidaysModal');
    modal.classList.add('active');
    loadHolidaysData();
}

async function loadHolidaysData() {
    const body = document.getElementById('holidaysBody');
    try {
        const holidays = await API.get('/api/holidays');
        if (!holidays || holidays.length === 0) {
            body.innerHTML = '<p class="text-muted">No holidays configured.</p>';
            return;
        }

        body.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr><th>Date</th><th>Holiday</th><th>Type</th></tr>
                    </thead>
                    <tbody>
                        ${holidays.map(h => `
                            <tr>
                                <td>${Utils.formatDate(h.holiday_date, 'long')}</td>
                                <td style="font-weight:500;">${Utils.escapeHtml(h.name)}</td>
                                <td><span class="badge badge-submitted">${h.holiday_type}</span></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) {
        body.innerHTML = '<p class="text-muted">Unable to load holidays.</p>';
    }
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}
