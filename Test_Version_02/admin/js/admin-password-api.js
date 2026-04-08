/**
 * Password reset queue — local storage (localStorage), no backend API.
 * Login is not required to open the page.
 */
(function (global) {
  var STORAGE_USER = 'tr_user';
  var STORAGE_RESETS = 'tr_admin_pw_resets_demo_v1';
  var STORAGE_AUDIT = 'tr_admin_pw_reset_audit_demo_v1';

  function getUser() {
    try {
      var s = localStorage.getItem(STORAGE_USER);
      return s ? JSON.parse(s) : null;
    } catch (e) {
      return null;
    }
  }

  function seedIfEmpty() {
    if (localStorage.getItem(STORAGE_RESETS)) return;
    var now = Date.now();
    var demo = [
      {
        id: 'pr-demo-1',
        username: 'ta001',
        role: 'TA',
        identityId: '2021001001',
        fullName: 'Zhang Wei',
        email: 'zhangwei@example.edu',
        phone: '138****0001',
        department: 'Computer Science',
        status: 'PENDING',
        rejectReason: null,
        createdAt: now - 86400000 * 2,
      },
      {
        id: 'pr-demo-2',
        username: 'mo002',
        role: 'MO',
        identityId: 'T80012',
        fullName: 'Li Na',
        email: 'lina@example.edu',
        phone: '',
        department: 'School Office',
        status: 'PENDING',
        rejectReason: null,
        createdAt: now - 3600000,
      },
      {
        id: 'pr-demo-3',
        username: 'ta099',
        role: 'TA',
        identityId: '2020000555',
        fullName: 'Wang Lei',
        email: 'wanglei@example.edu',
        phone: '139****7788',
        department: 'Software Engineering',
        status: 'REJECTED',
        rejectReason: 'ID could not be verified',
        createdAt: now - 86400000 * 5,
        processedAt: now - 86400000 * 4,
      },
    ];
    localStorage.setItem(STORAGE_RESETS, JSON.stringify(demo));
  }

  function getResets() {
    seedIfEmpty();
    try {
      var r = localStorage.getItem(STORAGE_RESETS);
      return r ? JSON.parse(r) : [];
    } catch (e) {
      return [];
    }
  }

  function saveResets(list) {
    localStorage.setItem(STORAGE_RESETS, JSON.stringify(list));
  }

  function appendAudit(entry) {
    try {
      var a = JSON.parse(localStorage.getItem(STORAGE_AUDIT) || '[]');
      a.push(entry);
      localStorage.setItem(STORAGE_AUDIT, JSON.stringify(a));
    } catch (e) {}
  }

  global.AdminPasswordApi = {
    getUser: getUser,
    /** 不校验登录 */
    requireAdmin: function () {
      return true;
    },
    /** 读取本地模拟队列 */
    listPasswordResets: function () {
      var list = getResets();
      list.sort(function (a, b) {
        return (b.createdAt || 0) - (a.createdAt || 0);
      });
      return Promise.resolve(list);
    },
    /** 本地更新状态并写入简易审计记录（仍无服务器） */
    putPasswordResetAction: function (id, body) {
      return new Promise(function (resolve, reject) {
        var list = getResets();
        var idx = list.findIndex(function (x) {
          return x.id === id;
        });
        if (idx === -1) {
          reject(new Error('Request not found'));
          return;
        }
        var pr = list[idx];
        if (pr.status !== 'PENDING') {
          reject(new Error('Already processed'));
          return;
        }
        var operator = getUser();
        var opName = operator ? operator.fullName || operator.username : 'Administrator';
        var action = body && body.action;
        var channel = (body && body.channel) || 'online';

        if (action === 'APPROVE') {
          pr.status = 'APPROVED';
          pr.processedAt = Date.now();
          pr.lastChannel = channel;
          list[idx] = pr;
          saveResets(list);
          appendAudit({
            time: Date.now(),
            operator: opName,
            applicant: pr.username,
            channel: channel,
            outcome: 'APPROVED',
            detail: 'Initial password set to 123456',
          });
          resolve({
            message: 'Approved. Password reset to initial value.',
            temporaryPassword: '123456',
          });
        } else if (action === 'REJECT') {
          pr.status = 'REJECTED';
          pr.rejectReason = (body && body.rejectReason) || '';
          pr.processedAt = Date.now();
          pr.lastChannel = channel;
          list[idx] = pr;
          saveResets(list);
          appendAudit({
            time: Date.now(),
            operator: opName,
            applicant: pr.username,
            channel: channel,
            outcome: 'REJECTED',
            detail: pr.rejectReason,
          });
          resolve({ message: 'Request rejected.' });
        } else {
          reject(new Error('action must be APPROVE or REJECT'));
        }
      });
    },
    /** 恢复初始示例数据 */
    resetDemoData: function () {
      localStorage.removeItem(STORAGE_RESETS);
      seedIfEmpty();
      return Promise.resolve();
    },
  };
})(typeof window !== 'undefined' ? window : globalThis);
