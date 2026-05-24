# Unit Test Evidence Report

## 1. Document Control

| Item | Value |
|---|---|
| System | BUPT International School TA Recruitment System |
| Tested version | `Test_Version_02` |
| Test type | Backend unit and handler-level API testing |
| Framework | JUnit 5 |
| Last verified date | 2026-05-24 |
| Latest verified result | `103 tests successful, 0 tests failed` |

## 2. Executive Summary

This document records the automated test evidence for the implemented TA, Module
Organiser (MO), and Administrator functions. The test suite verifies application
workflow rules, persistence to JSON-backed storage, file upload/download
handling, session-based authentication, role authorisation, notifications,
workload calculations, AI matching integration, and administrative operations.

The current suite contains 18 JUnit test classes and 103 test methods. Every
test passed in the latest verified execution.

| Measurement | Result |
|---|---:|
| Test classes executed | 18 |
| Test methods found | 103 |
| Test methods started | 103 |
| Test methods successful | 103 |
| Test methods failed | 0 |
| Containers failed | 0 |

## 3. Test Strategy

The application uses lightweight Java HTTP handlers and JSON file persistence
rather than a relational database. The tests therefore operate at the handler
and service boundary:

- `TestHttpExchange` simulates HTTP requests and captures status codes,
  response bodies, headers, and binary downloads.
- `DataService` is created with JUnit `@TempDir`, giving each test isolated
  users, jobs, applications, drafts, notifications, uploads, settings, logs,
  and export task files.
- Authentication tests use actual session tokens created by the application's
  session service.
- Workflow tests invoke the same handlers registered by the running
  application, including `AuthHandler`, `JobHandler`, `ApplicationHandler`,
  `DraftHandler`, `UploadHandler`, `NotificationHandler`, and `AdminHandler`.
- AI matching tests use the supported mock configuration so that results are
  deterministic and do not depend on an external network API.

## 4. Test Environment

| Item | Evidence |
|---|---|
| Project folder | `D:\Documents\GitHub\Software_Group_54_new\Test_Version_02` |
| Source under test | `src/main/java/com/bupt/tarecruit` |
| Unit test source | `src/main/java/com/bupt/tarecruit/unit_testing` |
| Language/toolchain | Java / `javac` and `java` |
| Test framework jar | `lib/junit-platform-console-standalone-1.10.2.jar` |
| JSON dependency | `lib/gson-2.10.1.jar` |
| Persistent test data | Temporary JSON and upload folders provided by `@TempDir` |
| External database required | No |
| External mail server required | No |
| External AI service required | No for automated suite |

## 5. Reproducible Execution

Run these commands in PowerShell. The final command prints each test with a
pass mark in a tree, which is suitable for an evidence screenshot.

```powershell
cd D:\Documents\GitHub\Software_Group_54_new\Test_Version_02

$main = "out\junit-main"
$test = "out\junit-test"
$root = (Resolve-Path .).Path
foreach ($p in @($main, $test)) {
  $full = Join-Path $root $p
  if (Test-Path -LiteralPath $full) {
    $resolved = (Resolve-Path -LiteralPath $full).Path
    if (-not $resolved.StartsWith($root)) {
      throw "Refusing to remove outside workspace: $resolved"
    }
    Remove-Item -LiteralPath $resolved -Recurse -Force
  }
  New-Item -ItemType Directory -Path $full -Force | Out-Null
}

javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar" -d out\junit-main src\main\java\com\bupt\tarecruit\model\*.java src\main\java\com\bupt\tarecruit\service\*.java src\main\java\com\bupt\tarecruit\handler\*.java src\main\java\com\bupt\tarecruit\Main.java

javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar;lib\junit-platform-console-standalone-1.10.2.jar;out\junit-main" -d out\junit-test src\main\java\com\bupt\tarecruit\unit_testing\*.java

java -jar lib\junit-platform-console-standalone-1.10.2.jar execute --class-path "out\junit-test;out\junit-main;lib\gson-2.10.1.jar" --scan-class-path --details=tree
```

If tree symbols do not render correctly in a Windows terminal, run
`chcp 65001` before the final `java -jar` command.

## 6. User Story Coverage Matrix

### 6.1 Teaching Assistant Stories

| Story ID | Story | Automated Evidence | Coverage Assessment |
|---|---|---|---|
| TA_01 | Personal Profile Management | `TaDataServiceStoriesTest` | Profile update fields are persisted and returned. |
| TA_02 | Vacancy Browsing and Filtering | `MoJobManagementStoriesTest`, `TaJobBoundaryStoriesTest` | Vacancy filters, posting visibility, and application counts are verified. |
| TA_03 | Integrated Submission | `TaDataServiceStoriesTest`, `AuthUploadDraftCoverageTest`, `TaJobBoundaryStoriesTest` | PDF validation, byte storage/download, and uploaded CV filename linked to an application are verified. |
| TA_04 | Self-Service Password Modification | `TaAuthStoriesTest` | Correct current password succeeds; incorrect password is rejected. |
| TA_05 | Forgot Password Online Application | `TaDataServiceStoriesTest`, `ExtendedAuthenticationAndAccessTest`, `AdSystemOperationsStoriesTest` | Request storage, role-disambiguated processing, password reset, and notification are verified. |
| TA_06 | Forgot Password Direct Admin Contact | `AdminAdvancedOperationsStoriesTest` | Administrative escalation path and password-reset notifications are verified at API level. |
| TA_07 | Application Deadline Reminder | Notification infrastructure is covered by `TaApplicationAccessAndNotificationTest` and `DataServiceIntegrityCoverageTest` | Notification delivery mechanics are tested; timed 48-hour reminder scheduling and rendered banner remain integration/UI checks. |
| TA_08 | Document Preview Before Upload | `AuthUploadDraftCoverageTest`, `TaDataServiceStoriesTest` | Valid PDF upload/download data path is tested; browser preview rendering remains a UI check. |
| TA_09 | Save as Draft | `TaDataServiceStoriesTest`, `AuthUploadDraftCoverageTest`, `ExtendedAuthenticationAndAccessTest` | Save, replace, retrieve, delete, dashboard listing, and TA-only access are verified. |
| TA_10 | Application Status Tracking | `TaApplicationAccessAndNotificationTest`, `MoApplicantReviewStoriesTest`, `DataServiceIntegrityCoverageTest` | Role-scoped application access and status notification creation/read state are verified. |
| TA_11 | Application Withdrawal | `TaApplicationFlowTest` | Own pending withdrawal succeeds; other-user and non-pending withdrawal are rejected. |
| TA_12 | Preference Selection | `TaApplicationFlowTest` | Priority range, duplicate priority, maximum three active applications, reuse after inactive status, and reconciliation are verified. |

### 6.2 Module Organiser Stories

| Story ID | Story | Automated Evidence | Coverage Assessment |
|---|---|---|---|
| MO_01 | Manage TA Work | `MoJobManagementStoriesTest`, `MoApplicantReviewStoriesTest` | Job creation/update/delete, applicant handling, cascaded deletion, and status decisions are verified. |
| MO_02 | View Questionnaire Submissions | `MoApplicantReviewStoriesTest` | MO can view applicants and CV details only for owned postings. |
| MO_03 | User Registration and Role Selection | `MoAuthStoriesTest`, `ExtendedAuthenticationAndAccessTest` | MO account creation, portal login, and teacher ID persistence are verified. |
| MO_04 | Simplify Approval Process | `MoApplicantReviewStoriesTest` | Approve/reject flows, invalid changes, quota protection, and TA notification are verified. |
| MO_05 | Optimise UI Design | Not suitable for JUnit backend suite | Layout/navigation usability requires browser/manual or UI automation evidence. |
| MO_06 | Applicant CV Review | `MoApplicantReviewStoriesTest`, `TaJobBoundaryStoriesTest` | Stored CV filename appears in applicant review and is linked from application submission. |
| MO_07 | Applicant Selection and Approval | `MoApplicantReviewStoriesTest` | Approval/rejection and capacity limit enforcement are verified. |
| MO_08 | Self-Service Password Modification | `MoAuthStoriesTest` | MO password change success and failure paths are verified. |
| MO_09 | Forgot Password Online Application | `MoAuthStoriesTest`, `ExtendedAuthenticationAndAccessTest`, `AdminBoundaryCoverageTest` | MO request creation and correct role-specific administrative reset are verified. |
| MO_10 | Forgot Password Direct Admin Contact | `AdminAdvancedOperationsStoriesTest` | Escalation and administrator notification behavior are verified. |
| MO_11 | Support Different Course Types | `MoJobManagementStoriesTest`, `DataServiceIntegrityCoverageTest` | Different posting types and type-specific workload calculation are verified. |

### 6.3 Administrator Stories

| Story ID | Story | Automated Evidence | Coverage Assessment |
|---|---|---|---|
| AD_01 | Monitor Overall TA Workload | `AdWorkloadAndStatsStoriesTest`, `TaJobBoundaryStoriesTest`, `DataServiceIntegrityCoverageTest` | Workload summary, overload detection, apply-time blocking, calculation and setting fallback are verified. |
| AD_02 | Manage System Users | `AdUserManagementStoriesTest`, `AdminBoundaryCoverageTest` | Super-admin creation/update and standard-admin restrictions are verified. |
| AD_03 | Oversee Job Postings | `AdJobOversightStoriesTest` | Admin update, delete, and application access for any posting are verified. |
| AD_04 | View Basic Application Statistics | `AdWorkloadAndStatsStoriesTest` | Application/user totals, quota, priority, and open-job statistics are verified. |
| AD_05 | Export Core System Data | `AdSystemOperationsStoriesTest`, `AdminAdvancedOperationsStoriesTest` | CSV creation, export-task history, download, and retry are verified. |
| AD_06 | View System Key Event Logs | `AdSystemOperationsStoriesTest`, `TaJobBoundaryStoriesTest` | Audit-log search and workload-block audit generation are verified. |
| AD_07 | Send Bulk Notifications | `AdSystemOperationsStoriesTest`, `AdminBoundaryCoverageTest` | Target-role delivery, inactive-user exclusion, and generated evidence file are verified. |
| AD_08 | Configure Recruitment Cycle Dates | `AdSystemOperationsStoriesTest` | Setting update persistence is verified. |
| AD_09 | Assign Basic User Roles | `AdUserManagementStoriesTest`, `AdminAdvancedOperationsStoriesTest`, `AdminBoundaryCoverageTest` | Role assignment, role-template CRUD/assignment count, and invalid template rejection are verified. |
| AD_10 | Admin Password Reset Processing | `AdSystemOperationsStoriesTest`, `AdminAdvancedOperationsStoriesTest`, `AdminBoundaryCoverageTest`, `ExtendedAuthenticationAndAccessTest` | Approve, reject, escalate, configurable initial password, role collision, and standard-admin restriction are verified. |
| AD_11 | Search Users/Jobs | `AdUserManagementStoriesTest`, `MoJobManagementStoriesTest` | User and job search/filter behavior is verified. |
| AD_12 | View Job Application Status Summary | `AdWorkloadAndStatsStoriesTest` | Priority and quota summary calculations are verified. |
| AD_13 | View Individual User Details | `AdUserManagementStoriesTest` | User detail retrieval is verified. |

## 7. Additional Implemented Feature Coverage

The implemented system contains behavior beyond the original user-story tables.
These features are also represented in the automated suite.

| Feature | Evidence |
|---|---|
| Authentication session invalidation on logout | `AuthUploadDraftCoverageTest` |
| Public prevention of administrator self-registration | `AuthUploadDraftCoverageTest` |
| Deactivated-account login blocking | `ExtendedAuthenticationAndAccessTest` |
| Portal login privacy for an identifier belonging to another role | `TaAuthStoriesTest` |
| Shared TA/MO ID disambiguation for login and password reset | `TaAuthStoriesTest`, `ExtendedAuthenticationAndAccessTest` |
| Notification ownership protection | `ExtendedAuthenticationAndAccessTest` |
| Upload/download authentication protection | `ExtendedAuthenticationAndAccessTest` |
| Job deletion cascade to applications, drafts, and notifications | `MoJobManagementStoriesTest` |
| Duplicate schedule-week validation | `MoJobManagementStoriesTest` |
| Closed, expired, and full position application rejection | `TaApplicationFlowTest`, `TaJobBoundaryStoriesTest` |
| Weekly workload limit rejection with notification and audit log | `TaJobBoundaryStoriesTest` |
| Notification reconciliation idempotency | `DataServiceIntegrityCoverageTest` |
| Admin backup jobs and retry | `AdminAdvancedOperationsStoriesTest` |
| AI match explanation, saved result visibility, model validation, and usage limit | `AiMatchingStoriesTest` |

## 8. Complete Test Class Inventory

The following inventory accounts for every test method in the successful
103-test execution.

| Test Class | Tests | Primary Verified Area |
|---|---:|---|
| `TaDataServiceStoriesTest` | 9 | Profile, password-reset request, drafts, upload validation and file persistence |
| `TaAuthStoriesTest` | 7 | TA login, role-separated portal access, password change, registration |
| `TaApplicationFlowTest` | 11 | Priority rules, deadlines, quota, reapplication, reconciliation, withdrawal |
| `TaApplicationAccessAndNotificationTest` | 5 | Application access boundaries and notification read operations |
| `TaJobBoundaryStoriesTest` | 6 | CV/application link, vacancy filters/status, workload rejection, posting/apply roles |
| `MoAuthStoriesTest` | 5 | MO registration, login, password change, password-reset request |
| `MoJobManagementStoriesTest` | 8 | Posting CRUD, schedule validation, deletion cascade, filtering, posting types |
| `MoApplicantReviewStoriesTest` | 6 | Applicant/CV access, decision actions, quota and invalid transitions |
| `AdUserManagementStoriesTest` | 5 | User creation, permissions, search, role/status update, details |
| `AdJobOversightStoriesTest` | 3 | Cross-posting administration and application visibility |
| `AdWorkloadAndStatsStoriesTest` | 4 | Workload and statistical dashboard calculations |
| `AdSystemOperationsStoriesTest` | 5 | CSV export, audit log search, broadcasts, settings, reset approval |
| `AdminAdvancedOperationsStoriesTest` | 5 | Role templates, backup/export tasks, reset rejection/escalation, super-admin scope |
| `AdminBoundaryCoverageTest` | 6 | Admin access restrictions, templates, reset template password, inactive broadcast exclusion |
| `AuthUploadDraftCoverageTest` | 4 | Admin registration restriction, logout, PDF download, draft endpoint |
| `ExtendedAuthenticationAndAccessTest` | 6 | Deactivation, teacher ID, shared-ID reset, ownership and authentication boundaries |
| `DataServiceIntegrityCoverageTest` | 4 | Notification reconciliation and workload calculation integrity |
| `AiMatchingStoriesTest` | 4 | AI matching outputs, persistence, supported model and usage quota |
| **Total** | **103** | **All passed in latest execution** |

## 9. Complete Unit Test Design Catalogue

This section records the design of every JUnit test method currently executed
by the suite. Each test is isolated with temporary data storage and is designed
to verify one externally observable rule or persistence outcome.

### 9.1 TA Account, Profile, Upload And Draft Tests

| Test Class | Test Method | Test Design | Expected Result |
|---|---|---|---|
| `TaDataServiceStoriesTest` | `ta01_profileUpdateShouldPersistPersonalInformation` | Submit changed TA personal fields through the profile flow and read stored user data. | Updated personal fields are persisted. |
| `TaDataServiceStoriesTest` | `ta05_passwordResetRequestShouldBeStoredAsPending` | Submit a TA password-reset request. | A reset record is stored with `PENDING` status. |
| `TaDataServiceStoriesTest` | `ta09_saveDraftShouldOverwriteExistingDraftForSameUserAndJob` | Save two drafts for the same TA and posting. | Only the latest draft content remains for that TA/posting pair. |
| `TaDataServiceStoriesTest` | `ta09_deleteDraftShouldRespectNormalizedJobId` | Save a draft and delete it using a job ID requiring normalisation. | The corresponding draft is removed. |
| `TaDataServiceStoriesTest` | `ta09_dashboardShouldListOnlyCurrentUsersDraftsWithJobInfo` | Store drafts for different TAs and request one TA's draft dashboard. | Only that TA's draft is returned, with posting details. |
| `TaDataServiceStoriesTest` | `ta03_uploadStorageShouldSanitizeFileNameAndKeepBytes` | Store an uploaded CV whose original name contains unsafe characters. | Stored filename is sanitised and downloaded bytes equal uploaded bytes. |
| `TaDataServiceStoriesTest` | `ta03_uploadApiShouldRejectNonPdfFile` | Attempt upload with a non-PDF extension. | Request is rejected and no invalid CV is accepted. |
| `TaDataServiceStoriesTest` | `ta03_uploadApiShouldRejectInvalidBase64` | Attempt PDF upload with invalid encoded content. | Request is rejected as an invalid upload. |
| `TaDataServiceStoriesTest` | `ta03_uploadApiShouldRejectEmptyFile` | Attempt PDF upload without file content. | Request is rejected as an empty upload. |
| `TaAuthStoriesTest` | `ta02_loginShouldAllowTaUsingStudentId` | Log in through the TA portal using a valid student ID and password. | Login succeeds and a TA session token is returned. |
| `TaAuthStoriesTest` | `ta02_loginShouldAllowTaWhenMoSharesSameStudentId` | Create TA and MO accounts sharing one ID, then log in via their respective portals. | Each portal authenticates the intended role only. |
| `TaAuthStoriesTest` | `ta02_loginShouldNotRevealAccountExistsOnAnotherPortal` | Submit valid TA credentials to the MO portal. | Login returns the generic MO invalid-credential response without role disclosure. |
| `TaAuthStoriesTest` | `ta04_changePasswordShouldUpdateStoredPassword` | Submit the correct current password and a new TA password. | Password update succeeds and the new value is stored. |
| `TaAuthStoriesTest` | `ta04_changePasswordShouldRejectWrongOldPassword` | Submit an incorrect current password. | Update is rejected and the previous password remains. |
| `TaAuthStoriesTest` | `ta06_registerShouldCreateTaAccountAndReturnToken` | Register a complete new TA account. | User is stored with role `TA` and an authenticated session is returned. |
| `TaAuthStoriesTest` | `ta06_registerShouldRejectDuplicateUsername` | Attempt TA registration with an existing username. | Request is rejected with conflict status and no duplicate is created. |
| `AuthUploadDraftCoverageTest` | `uploadDownloadShouldReturnStoredPdfBytesForAuthenticatedUser` | Authenticated user uploads and downloads a PDF. | Response content type is PDF and downloaded bytes match the upload. |
| `AuthUploadDraftCoverageTest` | `draftHandlerShouldReturnEmptyDraftAndAllowUpsertThenGet` | Read a missing draft, then save and retrieve it. | Empty response is explicit; saved letter and priority are returned. |

### 9.2 TA Vacancy, Application And Notification Tests

| Test Class | Test Method | Test Design | Expected Result |
|---|---|---|---|
| `TaApplicationAccessAndNotificationTest` | `ta07_listApplicationsShouldReturnOnlyCurrentTasOwnApplications` | Store applications belonging to multiple TAs and list as one TA. | Only the authenticated TA's applications are visible. |
| `TaApplicationAccessAndNotificationTest` | `ta08_getApplicationDetailShouldAllowOwnApplication` | Request detail of the authenticated TA's application. | Application detail is returned. |
| `TaApplicationAccessAndNotificationTest` | `ta08_getApplicationDetailShouldRejectOtherTasApplication` | Request another TA's application detail. | Access is denied. |
| `TaApplicationAccessAndNotificationTest` | `ta10_notificationsShouldListOnlyUsersNotificationsAndMarkSingleAsRead` | Store notifications for different users, list and read one as the owner. | Only own notifications are listed and selected item becomes read. |
| `TaApplicationAccessAndNotificationTest` | `ta10_notificationsShouldMarkAllAsRead` | Store multiple unread notifications for a TA and invoke read-all. | All of that TA's notifications become read. |
| `TaApplicationFlowTest` | `ta12_applyShouldRejectPriorityOutsideRange` | Submit application with preference outside `1..3`. | Application is rejected. |
| `TaApplicationFlowTest` | `ta12_applyShouldRejectDuplicatePriorityAmongActiveApplications` | Apply using a priority already assigned to another active application. | Duplicate preference is rejected. |
| `TaApplicationFlowTest` | `ta12_applyShouldRejectWhenThreeActiveApplicationsAlreadyExist` | Submit a fourth active application. | Application is rejected because the active limit is three. |
| `TaApplicationFlowTest` | `ta12_applyShouldRejectExpiredJobDeadline` | Apply to a posting with a passed deadline. | Application is rejected and no record is added. |
| `TaApplicationFlowTest` | `ta12_applyShouldRejectJobThatReachedQuota` | Apply after an existing approved application fills the posting quota. | Application is rejected because the position is full. |
| `TaApplicationFlowTest` | `ta12_applyShouldAllowReusingPriorityFromWithdrawnOrRejectedApplications` | Reuse priority values belonging only to inactive applications. | New pending application is accepted. |
| `TaApplicationFlowTest` | `ta12_applyShouldAllowReapplyingSameJobAfterRejection` | Apply again for a posting following a prior rejection. | New pending application is accepted. |
| `TaApplicationFlowTest` | `ta12_reconcileShouldWithdrawDuplicateActivePriorityRecords` | Persist conflicting active priorities then run reconciliation. | One conflicting record is withdrawn and active priorities are valid. |
| `TaApplicationFlowTest` | `ta11_withdrawShouldAllowOwnApplication` | TA withdraws their own pending application. | Status changes to `WITHDRAWN`. |
| `TaApplicationFlowTest` | `ta11_withdrawShouldRejectNonPendingApplication` | TA attempts withdrawal after an application is no longer pending. | Request is rejected and status is unchanged. |
| `TaApplicationFlowTest` | `ta11_withdrawShouldRejectOtherUsersApplication` | TA attempts to withdraw another TA's application. | Access is denied and status is unchanged. |
| `TaJobBoundaryStoriesTest` | `applicationShouldLinkUploadedCvAndNotifyJobOwner` | Upload a PDF and submit an application carrying the stored filename. | Application references the CV and MO receives new-application notification. |
| `TaJobBoundaryStoriesTest` | `taShouldNotApplyForClosedPosition` | Attempt application for a closed posting. | Request is rejected and no application is stored. |
| `TaJobBoundaryStoriesTest` | `vacancyListShouldFilterTypeAndStatusAndExposeApplicationCounts` | Query vacancies by type and status after applications exist. | Only matching postings are returned with correct application count. |
| `TaJobBoundaryStoriesTest` | `applicationShouldBeBlockedWhenApprovedWorkloadWouldExceedWeeklyLimit` | Give TA approved workload at the limit, then apply for further hours. | Application is blocked, and workload notification and audit log are stored. |
| `TaJobBoundaryStoriesTest` | `taShouldNotCreateVacancyPosting` | Submit job-creation request authenticated as TA. | Posting is rejected because TA lacks posting permission. |
| `TaJobBoundaryStoriesTest` | `moShouldNotSubmitTaApplication` | Submit application request authenticated as MO. | Application is rejected because only TAs may apply. |

### 9.3 MO Account, Posting And Selection Tests

| Test Class | Test Method | Test Design | Expected Result |
|---|---|---|---|
| `MoAuthStoriesTest` | `mo03_registerShouldCreateMoAccount` | Register a new module organiser account. | MO user is stored successfully. |
| `MoAuthStoriesTest` | `mo03_loginShouldAllowMoPortalAccess` | Log in to the MO portal with valid MO credentials. | Login succeeds and MO access is returned. |
| `MoAuthStoriesTest` | `mo08_changePasswordShouldUpdateStoredPassword` | Authenticated MO submits correct current password and a new password. | Stored password changes. |
| `MoAuthStoriesTest` | `mo08_changePasswordShouldRejectWrongOldPassword` | Authenticated MO submits an incorrect current password. | Request is rejected and stored password remains unchanged. |
| `MoAuthStoriesTest` | `mo09_forgotPasswordShouldCreateResetRequest` | MO submits forgot-password information. | Pending password-reset request is stored. |
| `MoJobManagementStoriesTest` | `mo01_createJobShouldAllowMoToPostTask` | Authenticated MO creates a vacancy with requirements. | Open posting is stored under the MO owner. |
| `MoJobManagementStoriesTest` | `mo01_updateJobShouldAllowOwnerToEditPostedTask` | Owning MO edits posting fields and status. | Updated values are persisted. |
| `MoJobManagementStoriesTest` | `mo01_createJobShouldRejectDuplicateCourseScheduleWeeks` | Create course posting containing duplicate week entries. | Posting creation is rejected. |
| `MoJobManagementStoriesTest` | `mo01_updateJobShouldRejectDuplicateCourseScheduleWeeks` | Edit a posting to contain duplicate week entries. | Posting update is rejected. |
| `MoJobManagementStoriesTest` | `mo01_deleteJobShouldRejectNonOwner` | A different MO attempts to delete an owned posting. | Access is denied and posting remains. |
| `MoJobManagementStoriesTest` | `mo01_deleteJobShouldCascadeApplicationsDraftsAndApplicationNotifications` | Delete a posting with related application, draft and notification records. | Posting and related records are removed. |
| `MoJobManagementStoriesTest` | `mo01_listJobsShouldSupportPostedByAndSearchFiltering` | Query postings by owner and search text. | Only matching posting is returned. |
| `MoJobManagementStoriesTest` | `mo11_createJobShouldSupportDifferentCourseTypes` | Create a non-course/alternative vacancy type. | Posting stores the selected type and course context. |
| `MoApplicantReviewStoriesTest` | `mo06_viewApplicantsShouldReturnCvAndApplicantInfoForOwnJob` | Owning MO lists applications for a posting with stored CV. | Applicant identity and CV filename are returned. |
| `MoApplicantReviewStoriesTest` | `mo06_viewApplicantsShouldRejectOtherMosJob` | MO requests applicants for another MO's posting. | Access is denied. |
| `MoApplicantReviewStoriesTest` | `mo07_approveApplicantShouldUpdateStatusAndNotifyTa` | Owning MO approves a pending applicant. | Application becomes `APPROVED` and TA receives notification. |
| `MoApplicantReviewStoriesTest` | `mo07_approveApplicantShouldRejectWhenQuotaIsFull` | MO attempts approval when another approved applicant fills quota. | Request is rejected and pending status remains. |
| `MoApplicantReviewStoriesTest` | `mo07_rejectApplicantShouldUpdateStatusAndNotifyTa` | Owning MO rejects a pending applicant. | Application becomes `REJECTED` and TA receives notification. |
| `MoApplicantReviewStoriesTest` | `mo07_shouldRejectInvalidMoStatusChange` | MO tries an unsupported status transition. | Request is rejected and pending status remains. |

### 9.4 Administrator Tests

| Test Class | Test Method | Test Design | Expected Result |
|---|---|---|---|
| `AdUserManagementStoriesTest` | `ad02_superAdminShouldCreateUser` | Super administrator creates a managed user. | New user is persisted. |
| `AdUserManagementStoriesTest` | `ad02_nonSuperAdminShouldNotCreateUser` | Standard administrator attempts user creation. | Request is forbidden. |
| `AdUserManagementStoriesTest` | `ad09_superAdminShouldUpdateRoleAndActiveStatus` | Super administrator updates user role and activation state. | Role/status changes are persisted. |
| `AdUserManagementStoriesTest` | `ad11_userListShouldSupportSearchAndRoleFilter` | Search and filter admin user list. | Result contains only matching users. |
| `AdUserManagementStoriesTest` | `ad13_shouldViewIndividualUserDetails` | Administrator requests a specific user's detail. | User detail is returned. |
| `AdJobOversightStoriesTest` | `ad03_adminShouldUpdateAnyJobPosting` | Administrator edits a posting owned by an MO. | Posting update is persisted. |
| `AdJobOversightStoriesTest` | `ad03_adminShouldDeleteAnyJobPosting` | Administrator deletes an MO posting. | Posting is removed. |
| `AdJobOversightStoriesTest` | `ad03_adminShouldViewApplicationsForAnyJob` | Administrator requests applications for an MO posting. | Applications are visible to admin. |
| `AdWorkloadAndStatsStoriesTest` | `ad01_workloadShouldShowOverloadedTaAndSupportStatusFilter` | Build overloaded approved workload and query admin workload with filter. | Overloaded TA appears in matching result. |
| `AdWorkloadAndStatsStoriesTest` | `ad04_statsShouldReturnApplicationAndUserSummary` | Request statistics after storing users and applications. | Summary totals are returned. |
| `AdWorkloadAndStatsStoriesTest` | `ad12_statsShouldIncludePriorityAndQuotaSummary` | Request statistics for prioritised applications and posting quota. | Priority and capacity summary values are returned. |
| `AdWorkloadAndStatsStoriesTest` | `ad04_statsOpenJobsShouldReflectAllLiveJobsNotOnlyCreatedInPeriod` | Query period statistics while an older job remains open. | Live open-job count includes all currently open postings. |
| `AdSystemOperationsStoriesTest` | `ad05_exportShouldCreateExportTaskAndCsvFile` | Administrator requests core-data export. | Completed export task and CSV file are created. |
| `AdSystemOperationsStoriesTest` | `ad06_auditLogsShouldSupportSearch` | Store audit entries and search by detail text. | Only matching audit entry is returned. |
| `AdSystemOperationsStoriesTest` | `ad07_bulkNotificationShouldSendToTargetRoles` | Super administrator sends notification to selected roles. | Target TA/MO recipients receive the announcement. |
| `AdSystemOperationsStoriesTest` | `ad08_superAdminShouldUpdateSystemSettings` | Super administrator updates workload and cycle-date settings. | Settings are persisted. |
| `AdSystemOperationsStoriesTest` | `ad10_approvePasswordResetShouldResetPasswordAndNotifyUser` | Super administrator approves a pending reset request. | Initial password is assigned and user notification is created. |
| `AdminAdvancedOperationsStoriesTest` | `adminRoleTemplatesShouldSupportCreateListDetailUpdateDeleteAndAssignedCount` | Execute complete role-template lifecycle with assigned admin. | CRUD works and detail reports assignment count. |
| `AdminAdvancedOperationsStoriesTest` | `backupTasksShouldCreateListDownloadAndRetryBackupZip` | Create backup, list/download it, mark failed and retry. | ZIP is created/downloaded and retry completes. |
| `AdminAdvancedOperationsStoriesTest` | `exportTasksShouldListDownloadAndRetryFailedExports` | Store failed export, filter it, retry, and download output. | Retry produces downloadable completed CSV task. |
| `AdminAdvancedOperationsStoriesTest` | `passwordResetRejectAndEscalateShouldPersistReasonAndNotifySuperAdmin` | Reject reset with reason and escalate a pending request. | Reason is stored and escalation notification is sent. |
| `AdminAdvancedOperationsStoriesTest` | `standardAdminShouldBeBlockedFromSuperAdminOperations` | Standard admin attempts settings change and bulk broadcast. | Both super-admin-only operations are forbidden. |
| `AdminBoundaryCoverageTest` | `nonAdminShouldBeDeniedAdminStatistics` | TA calls administrator statistics endpoint. | Access is forbidden. |
| `AdminBoundaryCoverageTest` | `standardAdminShouldReadUsersButNotDeleteAccounts` | Standard admin reads user list then attempts deletion. | Read succeeds; destructive write is forbidden. |
| `AdminBoundaryCoverageTest` | `superAdminShouldRejectUnknownAdminRoleTemplateAssignment` | Create admin user referencing non-existent role template. | Request is rejected and user is not created. |
| `AdminBoundaryCoverageTest` | `activePasswordResetTemplateShouldControlApprovedInitialPassword` | Configure active email template with custom reset password, then approve request. | User receives configured initial password. |
| `AdminBoundaryCoverageTest` | `bulkNotificationShouldSkipInactiveRecipientsAndCreateEvidenceFile` | Broadcast to TA role containing active and inactive accounts. | Active user receives notice, inactive user is skipped, evidence file exists. |
| `AdminBoundaryCoverageTest` | `standardAdminShouldNotProcessPasswordResetRequest` | Standard administrator attempts approval of TA reset. | Request remains pending and password unchanged. |

### 9.5 Shared Access, Service Integrity And AI Tests

| Test Class | Test Method | Test Design | Expected Result |
|---|---|---|---|
| `AuthUploadDraftCoverageTest` | `authShouldRejectSelfRegisteredAdminAccounts` | Submit public registration request with role `ADMIN`. | Registration is forbidden and no admin account is created. |
| `AuthUploadDraftCoverageTest` | `authLogoutShouldInvalidateSessionToken` | Authenticate, logout, then reuse old token for current-user request. | Token no longer authorises access. |
| `ExtendedAuthenticationAndAccessTest` | `loginShouldRejectDeactivatedAccountWithoutIssuingAccess` | Attempt valid login for deactivated TA. | Login is forbidden and token is not issued. |
| `ExtendedAuthenticationAndAccessTest` | `moRegistrationShouldPersistTeacherIdAndExposeItFromCurrentUser` | Register MO with teacher ID. | ID is stored and exposed as `teacherId` in authenticated response. |
| `ExtendedAuthenticationAndAccessTest` | `passwordResetRoleShouldTargetMoWhenTaAndMoShareIdentifier` | TA and MO share an ID; process reset specifically for MO role. | MO password resets while TA password remains unchanged. |
| `ExtendedAuthenticationAndAccessTest` | `notificationShouldRejectReadingAnotherUsersItem` | Different TA attempts to mark owner's notification read. | Request is forbidden and read flag remains false. |
| `ExtendedAuthenticationAndAccessTest` | `draftEndpointsShouldBeRestrictedToTaUsers` | MO attempts to save an application draft. | Request is forbidden and no draft is saved. |
| `ExtendedAuthenticationAndAccessTest` | `uploadAndDownloadShouldRequireAuthentication` | Upload and download file without session token. | Both operations reject unauthenticated request. |
| `DataServiceIntegrityCoverageTest` | `notificationReconciliationShouldBackfillApplicationEventsOnlyOnce` | Reconcile notifications twice after approved application data exists. | MO and TA workflow notifications each appear once only. |
| `DataServiceIntegrityCoverageTest` | `structuredScheduleShouldCalculateHoursPerWeekFromSelectedPeriods` | Supply structured selected periods across two weeks. | Weekly hours equal period count multiplied by configured duration. |
| `DataServiceIntegrityCoverageTest` | `finalExamWorkloadShouldUseExamDurationAsSingleWorkloadBucket` | Supply final-exam job with exam duration. | Duration is returned in single workload bucket. |
| `DataServiceIntegrityCoverageTest` | `invalidWorkloadSettingShouldFallBackToDefaultLimit` | Persist a non-numeric maximum-hour setting. | Service returns default limit of 20 hours. |
| `AiMatchingStoriesTest` | `aiMatchShouldExplainMatchedAndMissingSkillsForTa` | Request AI match for TA/job with mock matching enabled. | Response contains match explanation, skills and recommendation information. |
| `AiMatchingStoriesTest` | `moApplicationListShouldOnlyIncludeSavedAiMatchAfterRun` | List MO applications before and after a stored AI matching run. | AI summary appears only after saved matching result exists. |
| `AiMatchingStoriesTest` | `taAiMatchShouldBeLimitedToThreeSuccessfulCalls` | TA invokes successful AI matching repeatedly. | First three succeed; further use is restricted. |
| `AiMatchingStoriesTest` | `aiMatchShouldRejectUnsupportedModel` | Request AI matching with an unsupported model identifier. | Request is rejected. |

## 10. Selected Critical Test Design Details

The following cases were selected because they protect high-value workflows:
identity separation, submission integrity, workload safety, selection fairness,
privileged administration, and controlled AI use.

### 10.1 Shared Identifier Password Reset Targets The Correct Role

| Item | Detail |
|---|---|
| Test method | `ExtendedAuthenticationAndAccessTest.passwordResetRoleShouldTargetMoWhenTaAndMoShareIdentifier` |
| Risk addressed | A TA and an MO may use the same numeric identifier; a reset must not change the wrong person's password. |
| Preconditions | One `TA` and one `MO` user are stored with identifier `SHARED-110`, each with a distinct original password. |
| Input | Password-reset request with `role="MO"` and `teacherId="SHARED-110"`, followed by super-admin approval. |
| Execution | Call `/api/auth/password-reset`, locate stored MO reset request, then call `/api/admin/password-resets/{id}` with action `APPROVE`. |
| Assertions | Request is created and approved; TA password remains `ta-old`; MO password becomes the initial reset password `123456`. |
| Importance | Demonstrates that role identification is enforced at a sensitive account-recovery boundary. |

### 10.2 Uploaded CV Is Linked To The Submitted Application

| Item | Detail |
|---|---|
| Test method | `TaJobBoundaryStoriesTest.applicationShouldLinkUploadedCvAndNotifyJobOwner` |
| Risk addressed | A resume could be uploaded but not associated with the application reviewed by the MO. |
| Preconditions | Authenticated TA, owning MO and an open vacancy exist. |
| Input | Base64 PDF upload followed by application JSON containing the returned `cvFileName`. |
| Execution | POST upload through `UploadHandler`; POST application through `JobHandler`. |
| Assertions | Upload succeeds; application record contains exactly the saved CV filename; MO receives a `New Application` notification naming the posting. |
| Importance | Verifies the end-to-end data relationship between `uploads/` and `applications.json` used in applicant review. |

### 10.3 Application Is Blocked When Weekly Workload Would Exceed Limit

| Item | Detail |
|---|---|
| Test method | `TaJobBoundaryStoriesTest.applicationShouldBeBlockedWhenApprovedWorkloadWouldExceedWeeklyLimit` |
| Risk addressed | A TA may be approved or apply for duties above the configured weekly workload threshold. |
| Preconditions | `maxWeeklyHours` is set to `2`; TA already has an approved two-hour posting in week 1; another one-hour posting is open. |
| Input | Application for the additional posting with an unused priority. |
| Execution | POST `/api/jobs/{id}/apply` as the TA. |
| Assertions | Response is conflict status `409`; body identifies week 1 overload; a `WORKLOAD` notification and `WORKLOAD_BLOCK` audit record are saved. |
| Importance | Protects workload policy and leaves visible evidence explaining why an application was refused. |

### 10.4 MO Cannot Approve Beyond Posting Quota

| Item | Detail |
|---|---|
| Test method | `MoApplicantReviewStoriesTest.mo07_approveApplicantShouldRejectWhenQuotaIsFull` |
| Risk addressed | An MO could approve more applicants than a posting permits. |
| Preconditions | Vacancy quota is filled by an existing approved TA; a second application remains pending. |
| Input | MO status-update request attempting to approve the pending application. |
| Execution | PUT `/api/applications/{id}/status` with `APPROVED`. |
| Assertions | Response is `409`; message indicates the quota is reached; pending application stays `PENDING`. |
| Importance | Preserves recruitment capacity rules at the decision-making step. |

### 10.5 Only Super Admin Can Process Password Reset Requests

| Item | Detail |
|---|---|
| Test method | `AdminBoundaryCoverageTest.standardAdminShouldNotProcessPasswordResetRequest` |
| Risk addressed | A standard admin could otherwise gain account-control privileges reserved for the system administrator. |
| Preconditions | Standard administrator, TA account and pending reset request exist. |
| Input | Approval request authenticated with standard-admin session. |
| Execution | PUT `/api/admin/password-resets/{id}` with action `APPROVE`. |
| Assertions | Response is forbidden; request remains `PENDING`; TA password remains unchanged. |
| Importance | Validates the difference between normal administrator visibility and super-administrator authority. |

### 10.6 TA AI Matching Use Is Limited

| Item | Detail |
|---|---|
| Test method | `AiMatchingStoriesTest.taAiMatchShouldBeLimitedToThreeSuccessfulCalls` |
| Risk addressed | Unrestricted AI matching calls may exceed intended usage limits or service cost controls. |
| Preconditions | TA and eligible vacancy exist; deterministic AI matching mode is enabled for testing. |
| Input | Repeated supported AI matching requests from the same TA. |
| Execution | Call the job matching endpoint four times. |
| Assertions | Three successful results are accepted; the fourth request is rejected by the per-TA limit. |
| Importance | Verifies additional implemented functionality not stated in the original user-story sheet. |

## 11. Latest Successful Execution Evidence

The complete suite was compiled and executed on 2026-05-24 using the JUnit
Platform Console standalone runner with `--details=tree`.

```text
Test run finished after 3143 ms
[        21 containers found      ]
[         0 containers skipped    ]
[        21 containers started    ]
[         0 containers aborted    ]
[        21 containers successful ]
[         0 containers failed     ]
[       103 tests found           ]
[         0 tests skipped         ]
[       103 tests started         ]
[         0 tests aborted         ]
[       103 tests successful      ]
[         0 tests failed          ]
```

The tree-mode console output displays a pass mark beside each individual test
method. A terminal screenshot after executing the command in Section 5 can be
used as visual submission evidence.

## 12. Scope Limitations And Follow-Up Verification

The JUnit suite proves backend and handler behavior in an isolated local test
environment. It does not replace the following checks:

| Area | Recommended Verification |
|---|---|
| Page layout, responsive design, and user navigation | Manual browser review or UI automation |
| PDF preview rendering before submission | Browser-level test using an actual PDF |
| Deadline reminder scheduling and visible banner timing | Integration test with controlled system time |
| Real SMTP mail delivery | Test mail account or mocked mail transport integration test |
| Real external AI provider response and availability | Controlled staging test with secret supplied through configuration |
| Browser storage behavior, if used by frontend pages | Browser automation |

## 13. Security Observations Identified During Test Review

These findings are outside the JUnit coverage result but should be addressed
before deployment or demonstration with real credentials:

- SMTP connection credentials are stored in plaintext in `data/settings.json`.
- An AI service API key is hardcoded in
  `src/main/java/com/bupt/tarecruit/service/AiMatchingService.java`.
- User password values are represented as plain strings in the JSON-backed
  model and should be replaced by securely hashed password storage for a
  production deployment.

Recommended action: revoke and rotate exposed credentials, load secrets from
environment-specific configuration that is excluded from version control, and
introduce password hashing with migration for existing accounts.
