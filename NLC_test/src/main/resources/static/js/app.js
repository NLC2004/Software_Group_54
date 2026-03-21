/* ============================================================
   BUPT TA Recruitment System - Frontend Application
   ============================================================ */

// ==================== API ====================

const API = {
    token: localStorage.getItem('token'),
    async req(method, path, body) {
        const opts = { method, headers: {} };
        if (this.token) opts.headers['Authorization'] = 'Bearer ' + this.token;
        if (body) { opts.headers['Content-Type'] = 'application/json'; opts.body = JSON.stringify(body); }
        const r = await fetch(path, opts);
        const data = await r.json();
        if (r.status === 401) { localStorage.clear(); location.href = '/login.html'; return; }
        if (!r.ok) throw new Error(data.error || 'Request failed');
        return data;
    },
    get(p)    { return this.req('GET', p); },
    post(p,b) { return this.req('POST', p, b); },
    put(p,b)  { return this.req('PUT', p, b); },
    del(p)    { return this.req('DELETE', p); },
};

// ==================== Icons (Feather-style SVGs) ====================

const I = {
    dashboard: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>',
    user: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
    briefcase: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v2"/></svg>',
    file: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>',
    plus: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>',
    users: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>',
    bar: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>',
    settings: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 11-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 110-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 112.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 114 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 112.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 110 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>',
    logout: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>',
    check: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>',
    x: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>',
    list: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>',
    clock: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
    download: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>',
};

// ==================== State ====================

const S = {
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    view: null,
    viewData: null,
};

if (!S.user || !API.token) { localStorage.clear(); location.href = '/login.html'; }

// ==================== Toast ====================

function toast(msg, type = 'info') {
    const c = document.getElementById('toastContainer');
    const el = document.createElement('div');
    el.className = 'toast toast-' + type;
    el.textContent = msg;
    c.appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; setTimeout(() => el.remove(), 300); }, 3000);
}

// ==================== Modal ====================

function openModal(title, bodyHtml, footerHtml = '') {
    const o = document.getElementById('modalOverlay');
    const m = document.getElementById('modal');
    m.innerHTML = `
        <div class="modal-header"><h2>${title}</h2><button class="modal-close" onclick="closeModal()">${I.x}</button></div>
        <div class="modal-body">${bodyHtml}</div>
        ${footerHtml ? '<div class="modal-footer">' + footerHtml + '</div>' : ''}`;
    o.style.display = 'flex';
}
function closeModal() { document.getElementById('modalOverlay').style.display = 'none'; }
document.getElementById('modalOverlay').addEventListener('click', e => { if (e.target === e.currentTarget) closeModal(); });

// ==================== Helpers ====================

function fmtDate(ts) {
    if (!ts) return '-';
    return new Date(ts).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
}
function statusBadge(s) { return `<span class="badge badge-${(s||'').toLowerCase()}">${s}</span>`; }
function typeBadge(t) { return `<span class="badge badge-${(t||'').toLowerCase()}">${t === 'COURSE' ? 'Course TA' : 'Activity'}</span>`; }
function roleBadge(r) { return `<span class="badge badge-${(r||'').toLowerCase()}">${r}</span>`; }

function statCard(icon, color, value, label) {
    return `<div class="stat-card"><div class="stat-icon ${color}">${icon}</div><div><div class="stat-value">${value}</div><div class="stat-label">${label}</div></div></div>`;
}

function emptyState(title, sub) {
    return `<div class="empty-state"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v2"/></svg><h3>${title}</h3><p>${sub}</p></div>`;
}

function initials(name) { return (name || '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2); }

// ==================== Navigation ====================

const NAV = {
    TA: [
        { id: 'ta-dashboard', label: 'Dashboard', icon: I.dashboard },
        { id: 'ta-profile', label: 'My Profile', icon: I.user },
        { id: 'ta-jobs', label: 'Browse Jobs', icon: I.briefcase },
        { id: 'ta-applications', label: 'My Applications', icon: I.file },
    ],
    MO: [
        { id: 'mo-dashboard', label: 'Dashboard', icon: I.dashboard },
        { id: 'mo-post', label: 'Post Job', icon: I.plus },
        { id: 'mo-postings', label: 'My Postings', icon: I.list },
        { id: 'mo-applicants', label: 'Review Applicants', icon: I.users },
    ],
    ADMIN: [
        { id: 'admin-dashboard', label: 'Dashboard', icon: I.dashboard },
        { id: 'admin-users', label: 'Users', icon: I.users },
        { id: 'admin-jobs', label: 'All Jobs', icon: I.briefcase },
        { id: 'admin-workload', label: 'Workload', icon: I.bar },
        { id: 'admin-settings', label: 'Settings', icon: I.settings },
    ],
};

function navigate(view, data = null) {
    S.view = view;
    S.viewData = data;
    renderSidebar();
    renderContent();
}

function renderSidebar() {
    const nav = document.getElementById('sidebarNav');
    const items = NAV[S.user.role] || [];
    nav.innerHTML = items.map(it =>
        `<div class="nav-item ${S.view === it.id ? 'active' : ''}" onclick="navigate('${it.id}')">${it.icon}<span>${it.label}</span></div>`
    ).join('') +
    `<div class="nav-section">Account</div>
     <div class="nav-item" onclick="doLogout()">${I.logout}<span>Sign Out</span></div>`;

    const footer = document.getElementById('sidebarFooter');
    const displayName = S.user.fullName || S.user.username;
    footer.innerHTML = `<div class="user-info"><div class="user-avatar">${initials(displayName)}</div><div class="user-meta"><div class="name">${displayName}</div><div class="role">${S.user.role}</div></div></div>`;
}

async function doLogout() {
    try { await API.post('/api/auth/logout'); } catch(e) {}
    localStorage.clear();
    location.href = '/login.html';
}

// ==================== Content Router ====================

const VIEWS = {
    'ta-dashboard': taDashboard, 'ta-profile': taProfile, 'ta-jobs': taJobs, 'ta-applications': taApplications,
    'mo-dashboard': moDashboard, 'mo-post': moPostJob, 'mo-postings': moPostings, 'mo-applicants': moApplicants,
    'admin-dashboard': adminDashboard, 'admin-users': adminUsers, 'admin-jobs': adminJobs, 'admin-workload': adminWorkload, 'admin-settings': adminSettings,
};

function renderContent() {
    const el = document.getElementById('content');
    const fn = VIEWS[S.view];
    const titles = { 'ta-dashboard':'Dashboard','ta-profile':'My Profile','ta-jobs':'Browse Jobs','ta-applications':'My Applications',
        'mo-dashboard':'Dashboard','mo-post':'Post a Job','mo-postings':'My Job Postings','mo-applicants':'Review Applicants',
        'admin-dashboard':'Dashboard','admin-users':'User Management','admin-jobs':'All Job Postings','admin-workload':'TA Workload','admin-settings':'System Settings' };
    document.getElementById('topbar').innerHTML = `<h1>${titles[S.view] || 'Dashboard'}</h1><div class="topbar-actions"><span class="text-sm">${fmtDate(Date.now())}</span></div>`;
    if (fn) fn(el); else el.innerHTML = '<p>View not found.</p>';
}

// ==================== TA Views ====================

async function taDashboard(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const [jobs, apps] = await Promise.all([API.get('/api/jobs?status=OPEN'), API.get('/api/applications')]);
        const pending = apps.filter(a => a.status === 'PENDING').length;
        const approved = apps.filter(a => a.status === 'APPROVED').length;
        el.innerHTML = `
            <div class="stats-grid">
                ${statCard(I.briefcase, 'blue', jobs.length, 'Open Positions')}
                ${statCard(I.clock, 'amber', pending, 'Pending')}
                ${statCard(I.check, 'green', approved, 'Approved')}
                ${statCard(I.file, 'purple', apps.length, 'Total Applications')}
            </div>
            <div class="card"><div class="card-header"><h3>Recent Applications</h3></div>
            <div class="card-body no-pad">${apps.length ? appTable(apps.slice(0, 5), true) : emptyState('No applications yet', 'Browse open positions to get started')}</div></div>`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

async function taProfile(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const u = await API.get('/api/auth/me');
        S.user = u; localStorage.setItem('user', JSON.stringify(u));
        el.innerHTML = `
        <div class="card"><div class="card-header"><h3>Personal Information</h3></div><div class="card-body">
            <form id="profileForm">
                <div class="form-row">
                    <div class="form-group"><label>Full Name</label><input name="fullName" value="${u.fullName||''}" placeholder="Your full name"></div>
                    <div class="form-group"><label>Email</label><input name="email" type="email" value="${u.email||''}" placeholder="Email address"></div>
                </div>
                <div class="form-row">
                    <div class="form-group"><label>Phone</label><input name="phone" value="${u.phone||''}" placeholder="Phone number"></div>
                    <div class="form-group"><label>Gender</label><select name="gender"><option value="">-- Select --</option><option ${u.gender==='Male'?'selected':''}>Male</option><option ${u.gender==='Female'?'selected':''}>Female</option><option ${u.gender==='Other'?'selected':''}>Other</option></select></div>
                </div>
                <button type="submit" class="btn btn-primary">Save Changes</button>
            </form>
        </div></div>
        <div class="card"><div class="card-header"><h3>Change Password</h3></div><div class="card-body">
            <form id="pwdForm">
                <div class="form-row">
                    <div class="form-group"><label>Current Password</label><input name="oldPassword" type="password" required></div>
                    <div class="form-group"><label>New Password</label><input name="newPassword" type="password" required minlength="4"></div>
                </div>
                <button type="submit" class="btn btn-outline">Update Password</button>
            </form>
        </div></div>`;
        document.getElementById('profileForm').onsubmit = async e => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                const updated = await API.put('/api/auth/profile', Object.fromEntries(fd));
                S.user = updated; localStorage.setItem('user', JSON.stringify(updated));
                renderSidebar(); toast('Profile updated', 'success');
            } catch(err) { toast(err.message, 'error'); }
        };
        document.getElementById('pwdForm').onsubmit = async e => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try {
                await API.put('/api/auth/password', Object.fromEntries(fd));
                toast('Password updated', 'success'); e.target.reset();
            } catch(err) { toast(err.message, 'error'); }
        };
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

async function taJobs(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const jobs = await API.get('/api/jobs?status=OPEN');
        let filter = 'ALL';
        const render = () => {
            const filtered = filter === 'ALL' ? jobs : jobs.filter(j => j.type === filter);
            el.innerHTML = `
                <div class="filter-bar">
                    <button class="filter-btn ${filter==='ALL'?'active':''}" onclick="window._jf('ALL')">All</button>
                    <button class="filter-btn ${filter==='COURSE'?'active':''}" onclick="window._jf('COURSE')">Course TA</button>
                    <button class="filter-btn ${filter==='ACTIVITY'?'active':''}" onclick="window._jf('ACTIVITY')">Activity</button>
                    <span class="text-muted" style="margin-left:auto">${filtered.length} position${filtered.length!==1?'s':''}</span>
                </div>
                ${filtered.length ? '<div class="jobs-grid">' + filtered.map(j => jobCard(j, true)).join('') + '</div>'
                    : emptyState('No positions found', 'Check back later for new openings')}`;
        };
        window._jf = f => { filter = f; render(); };
        render();
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

function jobCard(j, showApply = false) {
    const reqs = (j.requirements||[]).map(r => `<span class="tag">${r}</span>`).join('');
    return `<div class="job-card">
        <div class="job-card-header">${typeBadge(j.type)} ${statusBadge(j.status)}</div>
        <h3>${esc(j.title)}</h3>
        ${j.courseName ? `<div class="job-course">${esc(j.courseName)}</div>` : ''}
        <div class="job-desc">${esc(j.description || 'No description provided.')}</div>
        <div class="job-meta">
            <span>${I.clock} ${j.weeklyHours || 0} hrs/week</span>
            <span>${I.users} ${j.applicationCount ?? '?'} applied</span>
            <span>Quota: ${j.approvedCount ?? 0}/${j.quota}</span>
        </div>
        ${reqs ? '<div>' + reqs + '</div>' : ''}
        <div class="job-card-footer">
            <span class="text-muted">by ${esc(j.posterName||'Unknown')}</span>
            ${showApply ? `<button class="btn btn-primary btn-sm" onclick="openApplyModal('${j.id}','${esc(j.title)}')">Apply</button>` : ''}
        </div>
    </div>`;
}

function openApplyModal(jobId, title) {
    openModal('Apply for ' + title, `
        <form id="applyForm">
            <div class="form-group"><label>Cover Letter</label><textarea name="coverLetter" placeholder="Why are you a good fit for this position?" rows="4"></textarea></div>
            <div class="form-group"><label>CV / Resume (optional)</label><input type="file" id="cvFile" accept=".pdf,.doc,.docx,.txt"></div>
            <div class="form-group"><label>Priority (1 = highest)</label><select name="priority"><option value="1">1 - Top choice</option><option value="2">2 - Second choice</option><option value="3">3 - Third choice</option></select></div>
        </form>`,
        `<button class="btn btn-outline" onclick="closeModal()">Cancel</button><button class="btn btn-primary" onclick="submitApplication('${jobId}')">Submit Application</button>`
    );
}

async function submitApplication(jobId) {
    const form = document.getElementById('applyForm');
    const fd = new FormData(form);
    const fileInput = document.getElementById('cvFile');
    let cvFileName = '';
    try {
        if (fileInput.files.length > 0) {
            const file = fileInput.files[0];
            const base64 = await readFileBase64(file);
            const up = await API.post('/api/upload', { fileName: file.name, data: base64 });
            cvFileName = up.fileName;
        }
        await API.post('/api/jobs/' + jobId + '/apply', {
            coverLetter: fd.get('coverLetter') || '',
            cvFileName: cvFileName,
            priority: parseInt(fd.get('priority')) || 1,
        });
        closeModal();
        toast('Application submitted!', 'success');
        navigate('ta-applications');
    } catch(e) { toast(e.message, 'error'); }
}

function readFileBase64(file) {
    return new Promise((resolve, reject) => {
        const r = new FileReader();
        r.onload = () => resolve(r.result.split(',')[1]);
        r.onerror = reject;
        r.readAsDataURL(file);
    });
}

async function taApplications(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const apps = await API.get('/api/applications');
        el.innerHTML = `<div class="card"><div class="card-header"><h3>My Applications</h3><span class="text-muted">${apps.length} total</span></div>
            <div class="card-body no-pad">${apps.length ? appTable(apps, true) : emptyState('No applications', 'Browse jobs and apply')}</div></div>`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

function appTable(apps, showWithdraw = false) {
    return `<table class="data-table"><thead><tr><th>Position</th><th>Type</th><th>Status</th><th>Priority</th><th>Applied</th>${showWithdraw?'<th>Actions</th>':''}</tr></thead><tbody>
    ${apps.map(a => `<tr>
        <td><strong>${esc(a.jobTitle)}</strong></td>
        <td>${typeBadge(a.jobType)}</td>
        <td>${statusBadge(a.status)}</td>
        <td>${a.priority || '-'}</td>
        <td>${fmtDate(a.createdAt)}</td>
        ${showWithdraw ? `<td>${a.status==='PENDING'?`<button class="btn btn-sm btn-outline-danger" onclick="withdrawApp('${a.id}')">Withdraw</button>`:'-'}</td>` : ''}
    </tr>`).join('')}</tbody></table>`;
}

async function withdrawApp(id) {
    if (!confirm('Withdraw this application?')) return;
    try {
        await API.put('/api/applications/' + id + '/status', { status: 'WITHDRAWN' });
        toast('Application withdrawn', 'success');
        navigate(S.view);
    } catch(e) { toast(e.message, 'error'); }
}

// ==================== MO Views ====================

async function moDashboard(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const [jobs, apps] = await Promise.all([
            API.get('/api/jobs?postedBy=' + S.user.id),
            API.get('/api/applications')
        ]);
        const pending = apps.filter(a => a.status === 'PENDING').length;
        const approved = apps.filter(a => a.status === 'APPROVED').length;
        el.innerHTML = `
            <div class="stats-grid">
                ${statCard(I.briefcase, 'blue', jobs.length, 'My Postings')}
                ${statCard(I.users, 'purple', apps.length, 'Total Applicants')}
                ${statCard(I.clock, 'amber', pending, 'Pending Review')}
                ${statCard(I.check, 'green', approved, 'Approved')}
            </div>
            <div class="card"><div class="card-header"><h3>My Job Postings</h3></div>
            <div class="card-body no-pad">${jobs.length ? jobsTable(jobs) : emptyState('No postings yet', 'Create your first job posting')}</div></div>`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

function jobsTable(jobs) {
    return `<table class="data-table"><thead><tr><th>Title</th><th>Type</th><th>Quota</th><th>Applicants</th><th>Status</th><th>Actions</th></tr></thead><tbody>
    ${jobs.map(j => `<tr>
        <td><strong>${esc(j.title)}</strong>${j.courseName ? '<br><span class="text-muted text-sm">'+esc(j.courseName)+'</span>' : ''}</td>
        <td>${typeBadge(j.type)}</td>
        <td>${j.approvedCount ?? 0} / ${j.quota}</td>
        <td>${j.applicationCount ?? 0}</td>
        <td>${statusBadge(j.status)}</td>
        <td>
            <button class="btn btn-sm btn-primary" onclick="navigate('mo-applicants','${j.id}')">Review</button>
            <button class="btn btn-sm btn-outline" onclick="toggleJobStatus('${j.id}','${j.status}')">${j.status==='OPEN'?'Close':'Open'}</button>
        </td>
    </tr>`).join('')}</tbody></table>`;
}

async function toggleJobStatus(id, current) {
    try {
        await API.put('/api/jobs/' + id, { status: current === 'OPEN' ? 'CLOSED' : 'OPEN' });
        toast('Job status updated', 'success');
        navigate(S.view);
    } catch(e) { toast(e.message, 'error'); }
}

async function moPostJob(el) {
    el.innerHTML = `<div class="card"><div class="card-header"><h3>Create New Position</h3></div><div class="card-body">
        <form id="postJobForm">
            <div class="form-row">
                <div class="form-group"><label>Job Title *</label><input name="title" required placeholder="e.g. Teaching Assistant - Data Structures"></div>
                <div class="form-group"><label>Type *</label><select name="type"><option value="COURSE">Course TA</option><option value="ACTIVITY">Activity (Invigilation etc.)</option></select></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>Course / Activity Name</label><input name="courseName" placeholder="e.g. EBU4201 Data Structures"></div>
                <div class="form-group"><label>Quota *</label><input name="quota" type="number" min="1" value="1" required></div>
            </div>
            <div class="form-row">
                <div class="form-group"><label>Weekly Hours</label><input name="weeklyHours" type="number" step="0.5" min="0" value="4" placeholder="Hours per week"></div>
                <div class="form-group"><label>Schedule / Dates</label><input name="schedule" placeholder="e.g. Mon/Wed 14:00-16:00"></div>
            </div>
            <div class="form-group"><label>Description</label><textarea name="description" rows="3" placeholder="Describe the responsibilities..."></textarea></div>
            <div class="form-group"><label>Required Skills (comma-separated)</label><input name="requirements" placeholder="e.g. Java, Python, Data Structures"></div>
            <button type="submit" class="btn btn-primary">Publish Position</button>
        </form>
    </div></div>`;
    document.getElementById('postJobForm').onsubmit = async e => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const reqs = (fd.get('requirements') || '').split(',').map(s => s.trim()).filter(Boolean);
        try {
            await API.post('/api/jobs', {
                title: fd.get('title'), type: fd.get('type'), courseName: fd.get('courseName'),
                quota: parseInt(fd.get('quota')), weeklyHours: parseFloat(fd.get('weeklyHours')) || 0,
                schedule: fd.get('schedule'), description: fd.get('description'), requirements: reqs,
            });
            toast('Job posted successfully!', 'success');
            navigate('mo-postings');
        } catch(err) { toast(err.message, 'error'); }
    };
}

async function moPostings(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const jobs = await API.get('/api/jobs?postedBy=' + S.user.id);
        el.innerHTML = `<div class="flex-between mb-6"><span class="text-muted">${jobs.length} posting${jobs.length!==1?'s':''}</span>
            <button class="btn btn-primary btn-sm" onclick="navigate('mo-post')">${I.plus} New Position</button></div>
            ${jobs.length ? '<div class="jobs-grid">' + jobs.map(j => moJobCard(j)).join('') + '</div>'
                : emptyState('No postings yet', 'Create your first job posting')}`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

function moJobCard(j) {
    return `<div class="job-card">
        <div class="job-card-header">${typeBadge(j.type)} ${statusBadge(j.status)}</div>
        <h3>${esc(j.title)}</h3>
        ${j.courseName ? `<div class="job-course">${esc(j.courseName)}</div>` : ''}
        <div class="job-meta">
            <span>${I.users} ${j.applicationCount??0} applicants</span>
            <span>Filled: ${j.approvedCount??0}/${j.quota}</span>
            <span>${I.clock} ${j.weeklyHours||0} hrs/week</span>
        </div>
        <div class="job-card-footer">
            <button class="btn btn-sm btn-primary" onclick="navigate('mo-applicants','${j.id}')">Review Applicants</button>
            <button class="btn btn-sm btn-outline" onclick="deleteJob('${j.id}')">Delete</button>
        </div>
    </div>`;
}

async function deleteJob(id) {
    if (!confirm('Delete this job posting?')) return;
    try { await API.del('/api/jobs/' + id); toast('Job deleted', 'success'); navigate(S.view); }
    catch(e) { toast(e.message, 'error'); }
}

async function moApplicants(el) {
    const jobId = S.viewData;
    if (!jobId) {
        // show a list of jobs to pick from
        el.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const jobs = await API.get('/api/jobs?postedBy=' + S.user.id);
            el.innerHTML = `<div class="card"><div class="card-header"><h3>Select a Position to Review</h3></div>
            <div class="card-body no-pad">${jobs.length ?
                `<table class="data-table"><thead><tr><th>Position</th><th>Applicants</th><th>Status</th><th></th></tr></thead><tbody>
                ${jobs.map(j => `<tr><td><strong>${esc(j.title)}</strong></td><td>${j.applicationCount??0}</td><td>${statusBadge(j.status)}</td>
                    <td><button class="btn btn-sm btn-primary" onclick="navigate('mo-applicants','${j.id}')">Review</button></td></tr>`).join('')}
                </tbody></table>` : emptyState('No postings', 'Post a job first')}</div></div>`;
        } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
        return;
    }
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const [job, apps] = await Promise.all([API.get('/api/jobs/' + jobId), API.get('/api/applications')]);
        const jobApps = apps.filter(a => a.jobId === jobId);
        el.innerHTML = `
            <div class="card mb-4"><div class="card-body">
                <div class="flex-between"><div><h3>${esc(job.title)}</h3><span class="text-muted">${job.courseName||''}</span></div>
                <div>${typeBadge(job.type)} ${statusBadge(job.status)}</div></div>
            </div></div>
            <div class="card"><div class="card-header"><h3>Applicants (${jobApps.length})</h3></div>
            <div class="card-body no-pad">${jobApps.length ?
                `<table class="data-table"><thead><tr><th>Name</th><th>Email</th><th>CV</th><th>Priority</th><th>Status</th><th>Applied</th><th>Actions</th></tr></thead><tbody>
                ${jobApps.map(a => `<tr>
                    <td><strong>${esc(a.applicantName)}</strong></td>
                    <td>${esc(a.applicantEmail||'-')}</td>
                    <td>${a.cvFileName ? `<button class="btn btn-sm btn-outline" onclick="viewCV('${a.cvFileName}')">${I.download} CV</button>` : '<span class="text-muted">-</span>'}</td>
                    <td>${a.priority||'-'}</td>
                    <td>${statusBadge(a.status)}</td>
                    <td>${fmtDate(a.createdAt)}</td>
                    <td>${a.status==='PENDING'?`
                        <button class="btn btn-sm btn-success" onclick="updateAppStatus('${a.id}','APPROVED')">Approve</button>
                        <button class="btn btn-sm btn-danger" onclick="updateAppStatus('${a.id}','REJECTED')">Reject</button>
                    `:'<span class="text-muted">Done</span>'}</td>
                </tr>`).join('')}</tbody></table>`
                : emptyState('No applicants yet', 'Applicants will appear here once they apply')}</div></div>`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

async function updateAppStatus(id, status) {
    try {
        await API.put('/api/applications/' + id + '/status', { status });
        toast('Applicant ' + status.toLowerCase(), 'success');
        navigate(S.view, S.viewData);
    } catch(e) { toast(e.message, 'error'); }
}

async function viewCV(fileName) {
    try {
        const r = await fetch('/api/upload/' + fileName, { headers: { 'Authorization': 'Bearer ' + API.token } });
        if (!r.ok) throw new Error('File not found');
        const blob = await r.blob();
        window.open(URL.createObjectURL(blob), '_blank');
    } catch(e) { toast('Could not open file: ' + e.message, 'error'); }
}

// ==================== Admin Views ====================

async function adminDashboard(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const stats = await API.get('/api/admin/stats');
        el.innerHTML = `
            <div class="stats-grid">
                ${statCard(I.users, 'blue', stats.totalUsers, 'Total Users')}
                ${statCard(I.briefcase, 'purple', stats.openJobs + ' / ' + stats.totalJobs, 'Open / Total Jobs')}
                ${statCard(I.file, 'amber', stats.pendingApplications, 'Pending Applications')}
                ${statCard(I.bar, 'red', stats.overloadedTAs || 0, 'Overloaded TAs')}
            </div>
            <div class="stats-grid">
                ${statCard(I.user, 'blue', stats.totalTAs, 'Teaching Assistants')}
                ${statCard(I.user, 'purple', stats.totalMOs, 'Module Organisers')}
                ${statCard(I.check, 'green', stats.approvedApplications, 'Approved Applications')}
                ${statCard(I.x, 'red', stats.rejectedApplications, 'Rejected')}
            </div>`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

async function adminUsers(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        let users = await API.get('/api/admin/users');
        const render = (list) => {
            el.innerHTML = `
                <div class="filter-bar mb-4">
                    <input class="search-input" id="userSearch" placeholder="Search users..." value="">
                </div>
                <div class="card"><div class="card-body no-pad">
                <table class="data-table"><thead><tr><th>Username</th><th>Full Name</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead><tbody>
                ${list.map(u => `<tr>
                    <td><strong>${esc(u.username)}</strong></td>
                    <td>${esc(u.fullName||'-')}</td>
                    <td>${esc(u.email||'-')}</td>
                    <td>${roleBadge(u.role)}</td>
                    <td>${u.active ? '<span class="badge badge-approved">Active</span>' : '<span class="badge badge-rejected">Inactive</span>'}</td>
                    <td>
                        <button class="btn btn-sm ${u.active?'btn-outline-danger':'btn-success'}" onclick="toggleUserActive('${u.id}',${!u.active})">${u.active?'Deactivate':'Activate'}</button>
                        <button class="btn btn-sm btn-outline" onclick="resetUserPwd('${u.id}','${esc(u.username)}')">Reset Pwd</button>
                    </td>
                </tr>`).join('')}</tbody></table></div></div>`;
            document.getElementById('userSearch').addEventListener('input', e => {
                const q = e.target.value.toLowerCase();
                const filtered = users.filter(u => (u.username||'').toLowerCase().includes(q) || (u.fullName||'').toLowerCase().includes(q) || (u.email||'').toLowerCase().includes(q));
                render(filtered);
                document.getElementById('userSearch').value = q;
                document.getElementById('userSearch').focus();
            });
        };
        render(users);
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

async function toggleUserActive(id, active) {
    try { await API.put('/api/admin/users/' + id, { active }); toast('User updated', 'success'); navigate(S.view); }
    catch(e) { toast(e.message, 'error'); }
}

async function resetUserPwd(id, username) {
    const newPwd = 'pass' + Math.random().toString(36).slice(2, 6);
    if (!confirm(`Reset password for "${username}" to: ${newPwd}?`)) return;
    try { await API.put('/api/admin/users/' + id, { password: newPwd }); toast('Password reset to: ' + newPwd, 'success'); }
    catch(e) { toast(e.message, 'error'); }
}

async function adminJobs(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const jobs = await API.get('/api/jobs');
        el.innerHTML = `<div class="card"><div class="card-header"><h3>All Job Postings (${jobs.length})</h3></div>
        <div class="card-body no-pad">
        <table class="data-table"><thead><tr><th>Title</th><th>Type</th><th>Posted By</th><th>Applicants</th><th>Status</th><th>Actions</th></tr></thead><tbody>
        ${jobs.map(j => `<tr>
            <td><strong>${esc(j.title)}</strong>${j.courseName?'<br><span class="text-muted text-sm">'+esc(j.courseName)+'</span>':''}</td>
            <td>${typeBadge(j.type)}</td>
            <td>${esc(j.posterName||'Unknown')}</td>
            <td>${j.applicationCount??0}</td>
            <td>${statusBadge(j.status)}</td>
            <td><button class="btn btn-sm btn-outline-danger" onclick="adminDeleteJob('${j.id}')">Remove</button></td>
        </tr>`).join('')}</tbody></table></div></div>`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

async function adminDeleteJob(id) {
    if (!confirm('Remove this job posting?')) return;
    try { await API.del('/api/jobs/' + id); toast('Job removed', 'success'); navigate(S.view); }
    catch(e) { toast(e.message, 'error'); }
}

async function adminWorkload(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const data = await API.get('/api/admin/workload');
        el.innerHTML = `<div class="card"><div class="card-header"><h3>TA Workload Overview</h3></div>
        <div class="card-body no-pad">
        ${data.length ? `<table class="data-table"><thead><tr><th>TA</th><th>Approved Positions</th><th>Weekly Hours</th><th>Max Hours</th><th>Status</th><th>Assigned Jobs</th></tr></thead><tbody>
        ${data.map(d => `<tr>
            <td><strong>${esc(d.fullName)}</strong></td>
            <td>${d.approvedPositions}</td>
            <td><strong>${d.totalWeeklyHours}</strong></td>
            <td>${d.maxHours}</td>
            <td>${d.overloaded ? '<span class="badge badge-overload">OVERLOADED</span>' : '<span class="badge badge-approved">OK</span>'}</td>
            <td>${(d.jobTitles||[]).map(t => '<span class="tag">'+esc(t)+'</span>').join(' ') || '-'}</td>
        </tr>`).join('')}</tbody></table>` : emptyState('No TAs in the system', 'TA workload will appear here once TAs are approved for positions')}</div></div>`;
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

async function adminSettings(el) {
    el.innerHTML = '<div class="loading-spinner"></div>';
    try {
        const settings = await API.get('/api/admin/settings');
        el.innerHTML = `<div class="card"><div class="card-header"><h3>Recruitment Settings</h3></div><div class="card-body">
            <form id="settingsForm">
                <div class="form-row">
                    <div class="form-group"><label>Recruitment Start Date</label><input name="recruitmentStart" type="date" value="${settings.recruitmentStart||''}"></div>
                    <div class="form-group"><label>Recruitment End Date</label><input name="recruitmentEnd" type="date" value="${settings.recruitmentEnd||''}"></div>
                </div>
                <div class="form-group" style="max-width:240px"><label>Max Weekly Hours per TA</label><input name="maxWeeklyHours" type="number" step="0.5" min="1" value="${settings.maxWeeklyHours||'20'}"></div>
                <button type="submit" class="btn btn-primary">Save Settings</button>
            </form>
        </div></div>`;
        document.getElementById('settingsForm').onsubmit = async e => {
            e.preventDefault();
            const fd = new FormData(e.target);
            try { await API.put('/api/admin/settings', Object.fromEntries(fd)); toast('Settings saved', 'success'); }
            catch(err) { toast(err.message, 'error'); }
        };
    } catch(e) { el.innerHTML = `<div class="alert alert-error">${e.message}</div>`; }
}

// ==================== XSS prevention ====================
function esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }

// ==================== Init ====================
(async function init() {
    try {
        const me = await API.get('/api/auth/me');
        S.user = me;
        localStorage.setItem('user', JSON.stringify(me));
    } catch (e) {
        localStorage.clear();
        location.href = '/login.html';
        return;
    }
    const defaultView = { TA: 'ta-dashboard', MO: 'mo-dashboard', ADMIN: 'admin-dashboard' };
    navigate(defaultView[S.user.role] || 'ta-dashboard');
})();
