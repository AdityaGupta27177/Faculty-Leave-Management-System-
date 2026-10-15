/**
 * hod.js — HOD dashboard logic: pending approvals, review, approve/reject
 * Faculty Leave Management System
 */

let pendingApps = [];
let historyApps = [];

document.addEventListener('DOMContentLoaded', async () => {
    const user = await requireAuth(['HOD', 'ADMIN']);
    if (!user) return;

    await loadHodData();
});

async function loadHodData() {
    await Promise.all([
        loadHodStats(),
        loadPendingApprovals(),
    ]);
}

async function loadHodStats() {
    const container = document.getElementById('hodStats');
    try {
        const stats = await API.get('/api/approval/stats');
        container.innerHTML = `
            <div class="stat-card">
                <div class="stat-icon warning">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.pending || 0}</div>
                    <div class="stat-label">Pending Review</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon success">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.approved || 0}</div>
                    <div class="stat-label">Approved (This Year)</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon danger">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.rejected || 0}</div>
                    <div class="stat-label">Rejected (This Year)</div>
                </div>
            </div>
            <div class="stat-card">
                <div class="stat-icon info">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                </div>
                <div class="stat-content">
                    <div class="stat-value">${stats.on_leave_today || 0}</div>
                    <div class="stat-label">On Leave Today</div>
                </div>
            </div>
        `;

        // Update badge
        const badge = document.getElementById('pendingCount');
        if (stats.pending > 0) {
            badge.textContent = stats.pending;
            badge.style.display = 'inline';
        }
    } catch (e) {
        container.innerHTML = '';
    }
}

async function loadPendingApprovals() {
    const container = document.getElementById('pendingList');
    try {
        pendingApps = await API.get('/api/approval/pending');

        if (!pendingApps || pendingApps.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">✅</div>
                    <p class="empty-state-title">All caught up!</p>
                    <p class="empty-state-text">No pending leave applications to review.</p>
                </div>
            `;
            return;
        }

        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Application</th>
                            <th>Faculty</th>
                            <th>Type</th>
                            <th>Dates</th>
                            <th>Days</th>
                            <th>Submitted</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${pendingApps.map(app => `
                            <tr>
                                <td style="font-weight:500;">${Utils.escapeHtml(app.application_no)}</td>
                                <td>
                                    <div style="font-weight:500;">${Utils.escapeHtml(app.faculty_name)}</div>
                                    <div class="text-xs text-muted">${Utils.escapeHtml(app.department_name || '')}</div>
                                </td>
                                <td>${Utils.escapeHtml(app.leave_type_name)}</td>
                                <td>
                                    <div class="text-sm">${Utils.formatDate(app.start_date)}</div>
                                    <div class="text-xs text-muted">to ${Utils.formatDate(app.end_date)}</div>
                                </td>
                                <td>${app.total_days}</td>
                                <td class="text-sm text-muted">${Utils.timeAgo(app.submitted_at)}</td>
                                <td>
                                    <button class="btn btn-sm btn-primary" onclick="openReview(${app.id})">
                                        Review
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) {
        container.innerHTML = '<p class="text-muted" style="padding:16px;">Unable to load pending approvals.</p>';
    }
}

async function openReview(id) {
    const modal = document.getElementById('reviewModal');
    modal.classList.add('active');
    const body = document.getElementById('reviewBody');
    const footer = document.getElementById('reviewFooter');

    try {
        const app = await API.get(`/api/leave/${id}`);
        document.getElementById('reviewTitle').textContent = `Review: ${app.application_no}`;

        // Check for overlapping leave in the department
        let overlapWarning = '';
        try {
            const overlaps = await API.get('/api/approval/overlaps', {
                start_date: app.start_date,
                end_date: app.end_date,
                exclude_id: app.id
            });
            if (overlaps && overlaps.length > 0) {
                overlapWarning = `
                    <div style="padding:12px; background:rgba(245,158,11,0.1); border:1px solid rgba(245,158,11,0.2); border-radius:var(--radius-md); margin-bottom:16px;">
                        <strong style="color:var(--warning-500);">⚠ Overlapping Leave:</strong>
                        <ul style="margin-top:6px; padding-left:20px; font-size:0.85rem; color:var(--text-secondary);">
                            ${overlaps.map(o => `<li>${Utils.escapeHtml(o.faculty_name)} — ${Utils.formatDate(o.start_date)} to ${Utils.formatDate(o.end_date)} (${o.status})</li>`).join('')}
                        </ul>
                    </div>
                `;
            }
        } catch (e) { /* ignore */ }

        body.innerHTML = `
            ${overlapWarning}
            <div class="detail-grid">
                <div class="detail-item">
                    <div class="detail-label">Faculty</div>
                    <div class="detail-value">${Utils.escapeHtml(app.faculty_name)}</div>
                </div>
                <div class="detail-item">
                    <div class="detail-label">Department</div>
                    <div class="detail-value">${Utils.escapeHtml(app.department_name || '—')}</div>
                </div>
                <div class="detail-item">
                    <div class="detail-label">Leave Type</div>
                    <div class="detail-value">${Utils.escapeHtml(app.leave_type_name)}</div>
                </div>
                <div class="detail-item">
                    <div class="detail-label">Duration</div>
                    <div class="detail-value">${app.total_days} day${app.total_days !== 1 ? 's' : ''}</div>
                </div>
                <div class="detail-item">
                    <div class="detail-label">From</div>
                    <div class="detail-value">${Utils.formatDate(app.start_date, 'long')}${app.half_day_start ? ' (Half)' : ''}</div>
                </div>
                <div class="detail-item">
                    <div class="detail-label">To</div>
                    <div class="detail-value">${Utils.formatDate(app.end_date, 'long')}${app.half_day_end ? ' (Half)' : ''}</div>
                </div>
                <div class="detail-item" style="grid-column:1/-1;">
                    <div class="detail-label">Reason</div>
                    <div class="detail-value">${Utils.escapeHtml(app.reason)}</div>
                </div>
            </div>

            <div class="form-group" style="margin-top:20px;">
                <label class="form-label" for="reviewComments">Comments</label>
                <textarea class="form-textarea" id="reviewComments" placeholder="Add your comments (required for rejection)…" rows="3"></textarea>
            </div>
        `;

        footer.innerHTML = `
            <button class="btn btn-secondary" onclick="closeReview()">Cancel</button>
            <button class="btn btn-warning" onclick="submitDecision(${app.id}, 'RETURNED')" style="display:inline-flex; align-items:center; gap:6px;">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 14 4 9 9 4"/><path d="M20 20v-7a4 4 0 0 0-4-4H4"/></svg>
                Return
            </button>
            <button class="btn btn-danger" onclick="submitDecision(${app.id}, 'REJECTED')" style="display:inline-flex; align-items:center; gap:6px;">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
                Reject
            </button>
            <button class="btn btn-success" onclick="submitDecision(${app.id}, 'APPROVED')" style="display:inline-flex; align-items:center; gap:6px;">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                Approve
            </button>
        `;
    } catch (e) {
        body.innerHTML = '<p class="text-muted">Unable to load application details.</p>';
        footer.innerHTML = '<button class="btn btn-secondary" onclick="closeReview()">Close</button>';
    }
}

async function submitDecision(id, action) {
    const comments = document.getElementById('reviewComments')?.value?.trim() || '';

    // Validate
    const validation = Validator.validateApproval({ action, comments });
    if (!validation.valid) {
        showToast('warning', 'Validation', Object.values(validation.errors)[0]);
        return;
    }

    const actionLabels = { APPROVED: 'approve', REJECTED: 'reject', RETURNED: 'return' };
    const confirmed = await Utils.confirm(
        `${actionLabels[action]?.charAt(0).toUpperCase() + actionLabels[action]?.slice(1)} Application?`,
        `Are you sure you want to ${actionLabels[action]} this leave application?`,
        actionLabels[action]?.charAt(0).toUpperCase() + actionLabels[action]?.slice(1),
        action === 'APPROVED' ? 'success' : 'danger'
    );
    if (!confirmed) return;

    try {
        await API.post(`/api/approval/${id}/decide`, { action, comments });
        showToast('success', 'Decision Recorded',
            `Application has been ${action.toLowerCase()}.`);
        closeReview();
        await loadHodData();
    } catch (e) {
        showToast('error', 'Error', e.message || 'Unable to record decision.');
    }
}

function closeReview() {
    document.getElementById('reviewModal').classList.remove('active');
}

function showTab(tab) {
    // Update tab buttons
    document.querySelectorAll('#hodTabs .tab').forEach((t, i) => {
        t.classList.toggle('active',
            (tab === 'pending' && i === 0) ||
            (tab === 'history' && i === 1) ||
            (tab === 'calendar' && i === 2)
        );
    });

    // Show/hide content
    document.getElementById('pendingTab').classList.toggle('hidden', tab !== 'pending');
    document.getElementById('historyTab').classList.toggle('hidden', tab !== 'history');
    document.getElementById('calendarTab').classList.toggle('hidden', tab !== 'calendar');

    if (tab === 'history') loadHistory();
    if (tab === 'calendar') loadDeptCalendar();
}

async function loadHistory() {
    const container = document.getElementById('historyList');
    try {
        historyApps = await API.get('/api/approval/history');

        if (!historyApps || historyApps.length === 0) {
            container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No decisions recorded yet.</p></div>';
            return;
        }

        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Application</th>
                            <th>Faculty</th>
                            <th>Type</th>
                            <th>Days</th>
                            <th>Decision</th>
                            <th>Date</th>
                            <th>Comments</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${historyApps.map(app => {
                            const badge = Utils.statusBadge(app.status);
                            return `
                                <tr>
                                    <td style="font-weight:500;">${Utils.escapeHtml(app.application_no)}</td>
                                    <td>${Utils.escapeHtml(app.faculty_name)}</td>
                                    <td>${Utils.escapeHtml(app.leave_type_name)}</td>
                                    <td>${app.total_days}</td>
                                    <td><span class="badge ${badge.className}">${badge.label}</span></td>
                                    <td class="text-sm">${Utils.formatDate(app.decided_at, 'short')}</td>
                                    <td class="text-sm truncate" style="max-width:200px;">${Utils.escapeHtml(app.approval_comments || '—')}</td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (e) {
        container.innerHTML = '<p class="text-muted" style="padding:16px;">Unable to load history.</p>';
    }
}

async function loadDeptCalendar() {
    const container = document.getElementById('deptCalendar');
    try {
        const data = await API.get('/api/leave/calendar', { dept: true });

        if (!data || data.length === 0) {
            container.innerHTML = '<div class="empty-state"><p class="empty-state-text">No upcoming leave in your department.</p></div>';
            return;
        }

        container.innerHTML = `
            <div class="table-container">
                <table class="data-table">
                    <thead>
                        <tr><th>Faculty</th><th>Type</th><th>From</th><th>To</th><th>Days</th><th>Status</th></tr>
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
        container.innerHTML = '<p class="text-muted" style="padding:16px;">Unable to load calendar.</p>';
    }
}

function exportHistory() {
    if (!historyApps || historyApps.length === 0) {
        showToast('info', 'No Data', 'No history data to export.');
        return;
    }

    const rows = historyApps.map(a => ({
        'Application No': a.application_no,
        'Faculty': a.faculty_name,
        'Leave Type': a.leave_type_name,
        'Start Date': a.start_date,
        'End Date': a.end_date,
        'Days': a.total_days,
        'Decision': a.status,
        'Decision Date': a.decided_at,
        'Comments': a.approval_comments || ''
    }));

    Utils.exportCSV(rows, 'approval_history.csv');
    showToast('success', 'Exported', 'History exported to CSV.');
}
