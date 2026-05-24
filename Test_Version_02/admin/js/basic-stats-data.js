/**
 * AD_04: Data structure of basic statistical indicators (aggregated from records such as applications).
 * The JSON returned by /api/admin/stats after page loading will overwrite this content; a default placeholder is provided here for offline preview and comparison purposes, which needs to be "recorded into a JavaScript file".
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
