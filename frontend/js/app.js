/**
 * app.js — Core application logic: auth state, navigation, theme, toast notifications
 * Faculty Leave Management System
 */

/* ========== Global State ========== */
let currentUser = null;

/* ========== Toast Notifications ========== */
function showToast(type, title, message, duration = 5000) {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const icons = {
        success: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--success-500)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>',
        error: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--danger-500)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
        warning: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--warning-500)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>',
        info: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="var(--primary-400)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>'
    };

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${icons[type] || icons.info}</span>
        <div class="toast-content">
            <div class="toast-title">${Utils.escapeHtml(title)}</div>
            <div class="toast-message">${Utils.escapeHtml(message)}</div>
        </div>
        <button class="toast-close" onclick="this.parentElement.remove()">✕</button>
    `;

    container.appendChild(toast);

    // Auto-remove
    setTimeout(() => {
        if (toast.parentElement) {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(20px)';
            setTimeout(() => toast.remove(), 300);
        }
    }, duration);
}

/* ========== Theme Toggle ========== */
function initTheme() {
    const saved = localStorage.getItem('theme') || 'dark';
    applyTheme(saved);
}

function toggleTheme() {
    const current = document.body.classList.contains('theme-light') ? 'light' : 'dark';
    const next = current === 'dark' ? 'light' : 'dark';
    applyTheme(next);
    localStorage.setItem('theme', next);
}

function applyTheme(theme) {
    if (theme === 'light') {
        document.body.classList.add('theme-light');
    } else {
        document.body.classList.remove('theme-light');
    }

    // Update toggle button icon
    const toggleBtn = document.querySelector('.theme-toggle');
    if (toggleBtn) {
        toggleBtn.innerHTML = theme === 'dark' 
            ? '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>'
            : '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>';
    }
}

/* ========== Auth Check (for dashboard pages) ========== */
async function requireAuth(allowedRoles = []) {
    try {
        const user = await API.get('/api/auth/me');
        if (!user || !user.id) {
            window.location.href = 'index.html';
            return null;
        }

        if (!user.is_approved) {
            window.location.href = 'index.html';
            return null;
        }

        if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
            window.location.href = 'index.html';
            return null;
        }

        currentUser = user;
        updateUserUI(user);
        return user;
    } catch (e) {
        window.location.href = 'index.html';
        return null;
    }
}

/* ========== Update UI with User Info ========== */
function updateUserUI(user) {
    // User name in sidebar
    const userName = document.querySelector('.user-name');
    if (userName) userName.textContent = user.name || user.email;

    // User role in sidebar
    const userRole = document.querySelector('.user-role');
    if (userRole) userRole.textContent = user.role?.toLowerCase() || 'faculty';

    // User avatar
    const avatar = document.querySelector('.user-avatar');
    const avatarPlaceholder = document.querySelector('.user-avatar-placeholder');
    if (user.profile_picture && avatar) {
        avatar.src = user.profile_picture;
        avatar.classList.remove('hidden');
        if (avatarPlaceholder) avatarPlaceholder.classList.add('hidden');
    } else if (avatarPlaceholder) {
        avatarPlaceholder.textContent = Utils.getInitials(user.name);
        avatarPlaceholder.classList.remove('hidden');
        if (avatar) avatar.classList.add('hidden');
    }
}

/* ========== Sidebar Navigation ========== */
function initSidebar() {
    // Active nav item
    const currentPage = window.location.pathname.split('/').pop() || 'index.html';
    document.querySelectorAll('.nav-item').forEach(item => {
        const href = item.getAttribute('href') || item.dataset.page;
        if (href === currentPage) {
            item.classList.add('active');
        }
    });

    // Mobile menu toggle
    const menuBtn = document.querySelector('.mobile-menu-btn');
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.querySelector('.sidebar-overlay');

    if (menuBtn && sidebar) {
        menuBtn.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }

    if (overlay && sidebar) {
        overlay.addEventListener('click', () => {
            sidebar.classList.remove('open');
        });
    }
}

/* ========== Logout ========== */
async function logout() {
    const confirmed = await Utils.confirm(
        'Sign Out',
        'Are you sure you want to sign out?',
        'Sign Out',
        'warning'
    );
    if (!confirmed) return;

    try {
        await API.post('/api/auth/logout');
    } catch (e) {
        // Ignore
    }
    window.location.href = 'index.html';
}

/* ========== Notification Panel ========== */
async function toggleNotifications() {
    const panel = document.getElementById('notifPanel');
    if (!panel) return;

    panel.classList.toggle('open');
    if (panel.classList.contains('open')) {
        await loadNotifications();
    }
}

async function loadNotifications() {
    const list = document.getElementById('notifList');
    if (!list) return;

    try {
        const notifications = await API.get('/api/notifications');
        if (notifications.length === 0) {
            list.innerHTML = `
                <div class="empty-state" style="padding:32px 16px;">
                    <div class="empty-state-icon">
                        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="var(--border-strong)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
                    </div>
                    <p class="empty-state-text">No notifications yet</p>
                </div>
            `;
        } else {
            list.innerHTML = notifications.map(n => `
                <div class="notif-item ${n.is_read ? '' : 'unread'}" 
                     onclick="markNotificationRead(${n.id})">
                    <div>
                        <div style="font-size:0.88rem; font-weight:500; color:var(--text-primary);">
                            ${Utils.escapeHtml(n.title)}
                        </div>
                        <div style="font-size:0.8rem; color:var(--text-muted); margin-top:4px;">
                            ${Utils.escapeHtml(n.message)}
                        </div>
                        <div style="font-size:0.72rem; color:var(--text-muted); margin-top:6px;">
                            ${Utils.timeAgo(n.created_at)}
                        </div>
                    </div>
                </div>
            `).join('');
        }

        // Update badge count
        const unread = notifications.filter(n => !n.is_read).length;
        updateNotifBadge(unread);
    } catch (e) {
        list.innerHTML = '<p class="text-muted text-sm" style="padding:16px;">Unable to load notifications.</p>';
    }
}

async function markNotificationRead(id) {
    try {
        await API.put(`/api/notifications/${id}/read`);
        await loadNotifications();
    } catch (e) {
        // Ignore
    }
}

function updateNotifBadge(count) {
    const dot = document.querySelector('.notif-dot');
    if (dot) {
        dot.style.display = count > 0 ? 'block' : 'none';
    }
    const badge = document.querySelector('.notif-count-badge');
    if (badge) {
        badge.textContent = count > 0 ? count : '';
        badge.style.display = count > 0 ? 'inline' : 'none';
    }
}

/* ========== Page Initialization ========== */
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initSidebar();
});
