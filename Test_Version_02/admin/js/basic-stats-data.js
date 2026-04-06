/**
 * AD_04：基础统计指标的数据结构（由申请 applications 等记录聚合）。
 * 页面加载后由 /api/admin/stats 返回的 JSON 覆盖；此处提供默认占位，便于离线预览与对照需求「记录成 js 文件」。
 */
window.BASIC_STATS_DATA = {
  source: 'aggregated_from_application_records',
  totalUsers: 0,
  totalApplications: 0,
  openJobs: 0,
  closedJobs: 0,
  pendingApplications: 0,
  approvedApplications: 0,
  rejectedApplications: 0,
  withdrawnApplications: 0,
  pendingPasswordResets: 0
};
