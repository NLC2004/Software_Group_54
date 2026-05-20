/**
 * Shared API utilities for TA Recruitment System v2.
 * Include this script in every HTML page before page-specific scripts.
 */
const LEGACY_TOKEN_KEY = 'ta_recruit_token';
const LEGACY_USER_KEY = 'ta_recruit_user';

function getPortalScope() {
    const path = window.location.pathname || '';
    if (path.startsWith('/admin/')) return 'admin';
    if (path.startsWith('/MO/')) return 'mo';
    if (path.startsWith('/TA/')) return 'ta';
    return 'default';
}

function scopedTokenKey(scope) {
    return 'ta_recruit_token_' + scope;
}

function scopedUserKey(scope) {
    return 'ta_recruit_user_' + scope;
}

function roleForScope(scope) {
    if (scope === 'admin') return 'ADMIN';
    if (scope === 'mo') return 'MO';
    if (scope === 'ta') return 'TA';
    return null;
}

function migrateLegacyAuth(scope) {
    if (localStorage.getItem(scopedTokenKey(scope))) return;
    const legacyToken = localStorage.getItem(LEGACY_TOKEN_KEY);
    const legacyUserRaw = localStorage.getItem(LEGACY_USER_KEY);
    if (!legacyToken || !legacyUserRaw) return;
    try {
        const user = JSON.parse(legacyUserRaw);
        const expectedRole = roleForScope(scope);
        if (expectedRole && user && user.role === expectedRole) {
            localStorage.setItem(scopedTokenKey(scope), legacyToken);
            localStorage.setItem(scopedUserKey(scope), legacyUserRaw);
        }
    } catch (e) { /* ignore */ }
}

/** Admin opened MO/TA page with ?userId= to view another account (from User Management). */
function isAdminProxyView() {
    const path = window.location.pathname || '';
    if (!path.startsWith('/MO/') && !path.startsWith('/TA/')) return false;
    const params = new URLSearchParams(window.location.search);
    return !!(params.get('userId') || params.get('id'));
}

function readScopedAuth(scope) {
    migrateLegacyAuth(scope);
    let user = null;
    try {
        const raw = localStorage.getItem(scopedUserKey(scope));
        user = raw ? JSON.parse(raw) : null;
    } catch (e) { /* ignore */ }
    return {
        token: localStorage.getItem(scopedTokenKey(scope)),
        user: user
    };
}

function resolveAuthContext() {
    const scope = getPortalScope();
    const local = readScopedAuth(scope);
    if (local.token && local.user) {
        return { scope: scope, token: local.token, user: local.user, proxy: false };
    }
    if (isAdminProxyView()) {
        const admin = readScopedAuth('admin');
        if (admin.token && admin.user && admin.user.role === 'ADMIN') {
            return { scope: 'admin', token: admin.token, user: admin.user, proxy: true };
        }
    }
    return { scope: scope, token: local.token, user: local.user, proxy: false };
}

const API = {
    getPortalScope() { return getPortalScope(); },

    isAdminProxyView() { return isAdminProxyView(); },

    getToken() {
        return resolveAuthContext().token;
    },

    getUser() {
        return resolveAuthContext().user;
    },

    setAuth(token, user) {
        const scope = getPortalScope();
        localStorage.setItem(scopedTokenKey(scope), token);
        localStorage.setItem(scopedUserKey(scope), JSON.stringify(user));
    },

    clearAuth() {
        const scope = getPortalScope();
        localStorage.removeItem(scopedTokenKey(scope));
        localStorage.removeItem(scopedUserKey(scope));
    },

    /** Clear all portal sessions (e.g. full sign-out). */
    clearAllAuth() {
        ['admin', 'mo', 'ta', 'default'].forEach(function(scope) {
            localStorage.removeItem(scopedTokenKey(scope));
            localStorage.removeItem(scopedUserKey(scope));
        });
        localStorage.removeItem(LEGACY_TOKEN_KEY);
        localStorage.removeItem(LEGACY_USER_KEY);
    },

    async request(path, options = {}) {
        const token = this.getToken();
        const headers = { ...(options.headers || {}) };
        if (token) headers['Authorization'] = 'Bearer ' + token;
        if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
            headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(options.body);
        }
        try {
            const res = await fetch(path, { ...options, headers });
            if (res.status === 401) {
                const normalizedPath = typeof path === 'string' ? path.split('?')[0] : '';
                const isLoginRequest = normalizedPath.endsWith('/api/auth/login');
                if (!isLoginRequest) {
                    const ctx = resolveAuthContext();
                    if (!ctx.proxy) {
                        this.clearAuth();
                    }
                    redirectToLogin();
                    return null;
                }
            }
            const contentType = res.headers.get('Content-Type') || '';
            if (contentType.includes('text/csv') || contentType.includes('application/zip') || contentType.includes('application/octet-stream')) {
                return { blob: await res.blob(), ok: res.ok, contentType };
            }
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Request failed');
            return data;
        } catch (e) {
            if (e.message === 'Failed to fetch') {
                showToast('Server connection failed. Is the server running?', 'error');
            }
            throw e;
        }
    },

    get(path) { return this.request(path); },
    post(path, body) { return this.request(path, { method: 'POST', body }); },
    put(path, body) { return this.request(path, { method: 'PUT', body }); },
    del(path) { return this.request(path, { method: 'DELETE' }); },

    async uploadFile(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = async () => {
                try {
                    const base64 = reader.result.split(',')[1];
                    const result = await API.post('/api/upload', { fileName: file.name, data: base64 });
                    resolve(result);
                } catch (e) { reject(e); }
            };
            reader.onerror = () => reject(new Error('File read failed'));
            reader.readAsDataURL(file);
        });
    }
};

function redirectToLogin() {
    const path = window.location.pathname;
    if (path.startsWith('/admin/')) {
        window.location.href = '/admin/index.html';
        return;
    }
    if (path.startsWith('/MO/')) {
        window.location.href = '/MO/index.html';
        return;
    }
    window.location.href = '/TA/index.html';
}

function requireAuth(allowedRoles) {
    const user = API.getUser();
    const token = API.getToken();
    if (!user || !token) { redirectToLogin(); return null; }
    if (allowedRoles && !allowedRoles.includes(user.role)) { redirectToLogin(); return null; }
    return user;
}

function logout() {
    API.post('/api/auth/logout', {}).catch(() => {});
    API.clearAuth();
    redirectToLogin();
}

function esc(str) {
    if (str == null) return '';
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

/** For onclick="fn('...')" — escape backslashes and single quotes in IDs / filenames */
function safeJsStr(s) {
    return String(s ?? '').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

function formatDate(ts) {
    if (!ts) return '-';
    const d = new Date(ts);
    return d.toLocaleDateString('en-GB', { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatDateTime(ts) {
    if (!ts) return '-';
    const d = new Date(ts);
    return d.toLocaleDateString('en-GB', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function timeAgo(ts) {
    if (!ts) return '';
    const diff = Date.now() - ts;
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'Just now';
    if (mins < 60) return mins + 'm ago';
    const hours = Math.floor(mins / 60);
    if (hours < 24) return hours + 'h ago';
    const days = Math.floor(hours / 24);
    if (days < 30) return days + 'd ago';
    return formatDate(ts);
}

function showToast(msg, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = 'position:fixed;top:20px;right:20px;z-index:99999;display:flex;flex-direction:column;gap:8px;';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    const colors = { info: '#3b82f6', success: '#22c55e', error: '#ef4444', warning: '#f59e0b' };
    toast.style.cssText = `background:${colors[type]||colors.info};color:white;padding:12px 20px;border-radius:8px;font-size:14px;box-shadow:0 4px 12px rgba(0,0,0,0.15);max-width:400px;opacity:0;transition:opacity 0.3s;`;
    toast.textContent = msg;
    container.appendChild(toast);
    requestAnimationFrame(() => toast.style.opacity = '1');
    setTimeout(() => { toast.style.opacity = '0'; setTimeout(() => toast.remove(), 300); }, 3000);
}

function getUrlParam(key) {
    return new URLSearchParams(window.location.search).get(key);
}

function statusBadge(status) {
    const map = {
        PENDING: { bg: '#fef3c7', color: '#92400e', label: 'Pending' },
        APPROVED: { bg: '#d1fae5', color: '#065f46', label: 'Approved' },
        REJECTED: { bg: '#fee2e2', color: '#991b1b', label: 'Rejected' },
        WITHDRAWN: { bg: '#e5e7eb', color: '#374151', label: 'Withdrawn' },
        OPEN: { bg: '#d1fae5', color: '#065f46', label: 'Open' },
        CLOSED: { bg: '#fee2e2', color: '#991b1b', label: 'Closed' },
    };
    const s = map[status] || { bg: '#e5e7eb', color: '#374151', label: status };
    return `<span style="display:inline-block;padding:2px 10px;border-radius:9999px;font-size:12px;font-weight:600;background:${s.bg};color:${s.color}">${s.label}</span>`;
}

function updateSidebarUser(user) {
    if (!user) return;
    document.querySelectorAll('[data-user-name]').forEach(el => el.textContent = user.fullName || user.username);
    document.querySelectorAll('[data-user-role]').forEach(el => el.textContent = user.role);
    document.querySelectorAll('[data-user-email]').forEach(el => el.textContent = user.email || '');
}
