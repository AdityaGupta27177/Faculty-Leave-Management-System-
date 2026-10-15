/**
 * utils.js — Utility functions for date calculations, formatting, and DOM helpers
 * Faculty Leave Management System
 */

const Utils = {
    /**
     * Format a date to a readable string
     * @param {string|Date} date
     * @param {string} format - 'short', 'long', 'iso'
     * @returns {string}
     */
    formatDate(date, format = 'short') {
        if (!date) return '—';
        const d = new Date(date);
        if (isNaN(d.getTime())) return '—';

        switch (format) {
            case 'long':
                return d.toLocaleDateString('en-IN', {
                    weekday: 'short', year: 'numeric', month: 'long', day: 'numeric'
                });
            case 'short':
                return d.toLocaleDateString('en-IN', {
                    year: 'numeric', month: 'short', day: 'numeric'
                });
            case 'iso':
                return d.toISOString().split('T')[0];
            case 'time':
                return d.toLocaleString('en-IN', {
                    year: 'numeric', month: 'short', day: 'numeric',
                    hour: '2-digit', minute: '2-digit'
                });
            default:
                return d.toLocaleDateString('en-IN');
        }
    },

    /**
     * Calculate working days between two dates, excluding weekends and holidays
     * @param {string} startDate
     * @param {string} endDate
     * @param {boolean} halfDayStart
     * @param {boolean} halfDayEnd
     * @param {Array} holidays - array of date strings (YYYY-MM-DD)
     * @returns {number}
     */
    calculateWorkingDays(startDate, endDate, halfDayStart = false, halfDayEnd = false, holidays = []) {
        const start = new Date(startDate);
        const end = new Date(endDate);

        if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) return 0;

        const holidaySet = new Set(holidays.map(h => typeof h === 'string' ? h : h.holiday_date));
        let count = 0;
        const current = new Date(start);

        while (current <= end) {
            const day = current.getDay();
            const dateStr = current.toISOString().split('T')[0];

            // Skip weekends (0 = Sunday, 6 = Saturday)
            if (day !== 0 && day !== 6 && !holidaySet.has(dateStr)) {
                count++;
            }
            current.setDate(current.getDate() + 1);
        }

        // Adjust for half days
        if (halfDayStart && count > 0) count -= 0.5;
        if (halfDayEnd && count > 0) count -= 0.5;

        return Math.max(0, count);
    },

    /**
     * Get today's date as YYYY-MM-DD
     * @returns {string}
     */
    today() {
        return new Date().toISOString().split('T')[0];
    },

    /**
     * Get current academic year (e.g., '2026-2027')
     * Academic year starts in June
     * @returns {string}
     */
    currentAcademicYear() {
        const now = new Date();
        const year = now.getFullYear();
        const month = now.getMonth(); // 0-indexed
        if (month >= 5) { // June onwards
            return `${year}-${year + 1}`;
        }
        return `${year - 1}-${year}`;
    },

    /**
     * Generate a unique ID
     * @returns {string}
     */
    generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substring(2, 8);
    },

    /**
     * Truncate text to a max length
     * @param {string} text
     * @param {number} maxLen
     * @returns {string}
     */
    truncate(text, maxLen = 50) {
        if (!text || text.length <= maxLen) return text || '';
        return text.substring(0, maxLen) + '…';
    },

    /**
     * Escape HTML to prevent XSS in dynamic content
     * @param {string} str
     * @returns {string}
     */
    escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    },

    /**
     * Debounce a function
     * @param {Function} fn
     * @param {number} delay
     * @returns {Function}
     */
    debounce(fn, delay = 300) {
        let timer;
        return function (...args) {
            clearTimeout(timer);
            timer = setTimeout(() => fn.apply(this, args), delay);
        };
    },

    /**
     * Get initials from a name
     * @param {string} name
     * @returns {string}
     */
    getInitials(name) {
        if (!name) return '?';
        return name.split(' ')
            .filter(Boolean)
            .map(w => w[0])
            .join('')
            .toUpperCase()
            .substring(0, 2);
    },

    /**
     * Format a status to a badge class and label
     * @param {string} status
     * @returns {{ className: string, label: string }}
     */
    statusBadge(status) {
        const map = {
            'DRAFT':     { className: 'badge-draft',     label: 'Draft' },
            'SUBMITTED': { className: 'badge-submitted', label: 'Submitted' },
            'PENDING':   { className: 'badge-pending',   label: 'Pending' },
            'APPROVED':  { className: 'badge-approved',  label: 'Approved' },
            'REJECTED':  { className: 'badge-rejected',  label: 'Rejected' },
            'CANCELLED': { className: 'badge-cancelled', label: 'Cancelled' },
            'RETURNED':  { className: 'badge-returned',  label: 'Returned' },
        };
        return map[status] || { className: 'badge-draft', label: status || 'Unknown' };
    },

    /**
     * Format a role to a badge class
     * @param {string} role
     * @returns {{ className: string, label: string }}
     */
    roleBadge(role) {
        const map = {
            'FACULTY': { className: 'role-faculty', label: 'Faculty' },
            'HOD':     { className: 'role-hod',     label: 'HOD' },
            'ADMIN':   { className: 'role-admin',   label: 'Admin' },
        };
        return map[role] || { className: 'role-faculty', label: role || 'Unknown' };
    },

    /**
     * Show a confirmation dialog
     * @param {string} title
     * @param {string} message
     * @param {string} confirmText
     * @param {string} type - 'warning', 'danger', 'success'
     * @returns {Promise<boolean>}
     */
    confirm(title, message, confirmText = 'Confirm', type = 'warning') {
        return new Promise((resolve) => {
            const overlay = document.createElement('div');
            overlay.className = 'modal-overlay active confirm-dialog';
            overlay.innerHTML = `
                <div class="modal">
                    <div class="modal-body" style="text-align:center; padding-top:32px;">
                        <div class="confirm-icon ${type}">
                            ${type === 'danger' 
                                ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--danger-500)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>' 
                                : type === 'success' 
                                ? '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--success-500)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>' 
                                : '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--warning-500)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>'}
                        </div>
                        <h3 style="margin-bottom:8px;">${Utils.escapeHtml(title)}</h3>
                        <p style="color:var(--text-secondary); font-size:0.9rem;">${Utils.escapeHtml(message)}</p>
                    </div>
                    <div class="modal-footer" style="justify-content:center;">
                        <button class="btn btn-secondary" id="confirmCancel">Cancel</button>
                        <button class="btn ${type === 'danger' ? 'btn-danger' : 'btn-primary'}" id="confirmOk">${Utils.escapeHtml(confirmText)}</button>
                    </div>
                </div>
            `;
            document.body.appendChild(overlay);

            overlay.querySelector('#confirmCancel').onclick = () => {
                document.body.removeChild(overlay);
                resolve(false);
            };
            overlay.querySelector('#confirmOk').onclick = () => {
                document.body.removeChild(overlay);
                resolve(true);
            };
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) {
                    document.body.removeChild(overlay);
                    resolve(false);
                }
            });
        });
    },

    /**
     * Format file size
     * @param {number} bytes
     * @returns {string}
     */
    formatFileSize(bytes) {
        if (!bytes) return '0 B';
        const units = ['B', 'KB', 'MB', 'GB'];
        let i = 0;
        while (bytes >= 1024 && i < units.length - 1) {
            bytes /= 1024;
            i++;
        }
        return `${bytes.toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
    },

    /**
     * Relative time (e.g., "2 hours ago")
     * @param {string|Date} date
     * @returns {string}
     */
    timeAgo(date) {
        if (!date) return '';
        const d = new Date(date);
        const now = new Date();
        const diff = Math.floor((now - d) / 1000);

        if (diff < 60) return 'Just now';
        if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
        if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
        if (diff < 604800) return `${Math.floor(diff / 86400)}d ago`;
        return Utils.formatDate(date, 'short');
    },

    /**
     * CSV export helper
     * @param {Array} rows - array of objects
     * @param {string} filename
     */
    exportCSV(rows, filename = 'export.csv') {
        if (!rows.length) return;
        const headers = Object.keys(rows[0]);
        const csv = [
            headers.join(','),
            ...rows.map(row =>
                headers.map(h => {
                    let val = row[h] ?? '';
                    val = String(val).replace(/"/g, '""');
                    return `"${val}"`;
                }).join(',')
            )
        ].join('\n');

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = filename;
        link.click();
        URL.revokeObjectURL(link.href);
    }
};
