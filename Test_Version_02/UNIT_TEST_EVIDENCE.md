# Unit Test Evidence

## 1. Purpose

This document records the unit testing evidence for `Test_Version_02` of the BUPT International School TA Recruitment System. The tests are written with JUnit 5 and focus on backend API handlers, data persistence, role-based access control, application workflow rules, workload calculation, AI-assisted matching, upload handling, notifications, and administrative operations.

The tests are intended to provide evidence that the main TA, MO, and Admin functions can be verified automatically without relying on manual browser interaction.

## 2. Test Environment

| Item | Evidence |
|---|---|
| Project folder | `Test_Version_02` |
| Language | Java |
| Test framework | JUnit 5 |
| Test runner | `junit-platform-console-standalone-1.10.2.jar` |
| JSON library | `gson-2.10.1.jar` |
| Test source folder | `src/main/java/com/bupt/tarecruit/unit_testing` |
| Data storage during tests | Temporary folders via JUnit `@TempDir` |
| Database dependency | None |

## 3. How To Run The Tests

Open PowerShell and run the following commands from the project root.

```powershell
cd D:\Documents\GitHub\Software_Group_54_new\Test_Version_02
```

Clean and create isolated evidence build folders:

```powershell
$main = "out\junit-main"
$test = "out\junit-test"
$root = (Resolve-Path .).Path
foreach ($p in @($main, $test)) {
  $full = Join-Path $root $p
  if (Test-Path -LiteralPath $full) {
    $resolved = (Resolve-Path -LiteralPath $full).Path
    if (-not $resolved.StartsWith($root)) { throw "Refusing to remove outside workspace: $resolved" }
    Remove-Item -LiteralPath $resolved -Recurse -Force
  }
  New-Item -ItemType Directory -Path $full -Force | Out-Null
}
```

Compile the main source code into the isolated evidence folder:

```powershell
javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar" -d out\junit-main src\main\java\com\bupt\tarecruit\model\*.java src\main\java\com\bupt\tarecruit\service\*.java src\main\java\com\bupt\tarecruit\handler\*.java src\main\java\com\bupt\tarecruit\Main.java
```

Compile the unit tests into the isolated evidence folder:

```powershell
javac -encoding UTF-8 -cp "lib\gson-2.10.1.jar;lib\junit-platform-console-standalone-1.10.2.jar;out\junit-main" -d out\junit-test src\main\java\com\bupt\tarecruit\unit_testing\*.java
```

Run all JUnit tests:

```powershell
java -jar lib\junit-platform-console-standalone-1.10.2.jar --class-path "out\junit-test;out\junit-main;lib\gson-2.10.1.jar" --scan-class-path
```

Expected result:

```text
70 tests found
70 tests successful
0 tests failed
```

If the number differs, check that all files in `src/main/java/com/bupt/tarecruit/unit_testing` have been compiled into `out/junit-test`.

## 4. Unit Test Scope

The unit tests use handler-level tests rather than browser UI tests. They simulate HTTP requests through `TestHttpExchange`, authenticate users with session tokens, call the same handlers used by the running server, and verify response codes, response bodies, JSON persistence, notifications, audit logs, uploaded files, and role permissions.

The following areas are covered:

- TA authentication, registration, profile update, password change, forgot-password request.
- TA job application flow, priority selection, active-application limit, withdrawal rules.
- TA CV upload validation, PDF download, draft save/recovery/delete, notification read state.
- MO registration/login, job creation/update/filtering/deletion, applicant review, CV information, approval/rejection.
- Admin user management, job oversight, workload monitoring, statistics, audit logs, export, backup, role templates, bulk notifications, password reset processing.
- AI-assisted matching output, including match score, matched skills, missing skills, workload risk, and MO applicant-list integration.

## 5. Test Case Summary

### 5.1 TA Tests

| Test ID | Test Class | Test Method | Evidence / Expected Result |
|---|---|---|---|
| TA-01 | `TaDataServiceStoriesTest` | `ta01_profileUpdateShouldPersistPersonalInformation` | Profile fields such as name, phone, gender, school, supervisor, degree, and year are persisted. |
| TA-02 | `TaAuthStoriesTest` | `ta02_loginShouldAllowTaUsingStudentId` | TA can log in through the TA portal using student ID and receives a token. |
| TA-03 | `TaAuthStoriesTest` | `ta02_loginShouldRejectWrongPortalRole` | TA credentials cannot be used to enter the MO portal. |
| TA-04 | `TaDataServiceStoriesTest` | `ta03_uploadStorageShouldSanitizeFileNameAndKeepBytes` | Uploaded CV filename is sanitised and stored bytes are preserved. |
| TA-05 | `TaDataServiceStoriesTest` | `ta03_uploadApiShouldRejectNonPdfFile` | Non-PDF upload is rejected. |
| TA-06 | `TaDataServiceStoriesTest` | `ta03_uploadApiShouldRejectInvalidBase64` | Invalid base64 upload payload is rejected. |
| TA-07 | `TaDataServiceStoriesTest` | `ta03_uploadApiShouldRejectEmptyFile` | Empty upload payload is rejected. |
| TA-08 | `AuthUploadDraftCoverageTest` | `uploadDownloadShouldReturnStoredPdfBytesForAuthenticatedUser` | Authenticated user can download the same PDF bytes that were uploaded. |
| TA-09 | `TaAuthStoriesTest` | `ta04_changePasswordShouldUpdateStoredPassword` | Correct old password updates the stored password. |
| TA-10 | `TaAuthStoriesTest` | `ta04_changePasswordShouldRejectWrongOldPassword` | Wrong old password is rejected and password remains unchanged. |
| TA-11 | `TaDataServiceStoriesTest` | `ta05_passwordResetRequestShouldBeStoredAsPending` | Forgot-password request is stored as `PENDING`. |
| TA-12 | `TaAuthStoriesTest` | `ta06_registerShouldCreateTaAccountAndReturnToken` | TA registration creates a TA user and returns login token. |
| TA-13 | `TaAuthStoriesTest` | `ta06_registerShouldRejectDuplicateUsername` | Duplicate TA username is rejected. |
| TA-14 | `TaApplicationAccessAndNotificationTest` | `ta07_listApplicationsShouldReturnOnlyCurrentTasOwnApplications` | TA application list is scoped to the current TA only. |
| TA-15 | `TaApplicationAccessAndNotificationTest` | `ta08_getApplicationDetailShouldAllowOwnApplication` | TA can view their own application detail. |
| TA-16 | `TaApplicationAccessAndNotificationTest` | `ta08_getApplicationDetailShouldRejectOtherTasApplication` | TA cannot view another TA's application detail. |
| TA-17 | `TaDataServiceStoriesTest` | `ta09_saveDraftShouldOverwriteExistingDraftForSameUserAndJob` | Saving a draft for the same user/job overwrites the older draft. |
| TA-18 | `TaDataServiceStoriesTest` | `ta09_deleteDraftShouldRespectNormalizedJobId` | Draft deletion handles normalised job IDs. |
| TA-19 | `TaDataServiceStoriesTest` | `ta09_dashboardShouldListOnlyCurrentUsersDraftsWithJobInfo` | Dashboard draft list shows only the current TA's drafts with job information. |
| TA-20 | `AuthUploadDraftCoverageTest` | `draftHandlerShouldReturnEmptyDraftAndAllowUpsertThenGet` | Draft API returns explicit `draft:null`, then supports save and recovery. |
| TA-21 | `TaApplicationAccessAndNotificationTest` | `ta10_notificationsShouldListOnlyUsersNotificationsAndMarkSingleAsRead` | TA sees only own notifications and can mark one notification as read. |
| TA-22 | `TaApplicationAccessAndNotificationTest` | `ta10_notificationsShouldMarkAllAsRead` | TA can mark all own notifications as read. |
| TA-23 | `TaApplicationFlowTest` | `ta11_withdrawShouldAllowOwnApplication` | TA can withdraw their own pending application. |
| TA-24 | `TaApplicationFlowTest` | `ta11_withdrawShouldRejectNonPendingApplication` | Approved/rejected/non-pending application cannot be withdrawn. |
| TA-25 | `TaApplicationFlowTest` | `ta11_withdrawShouldRejectOtherUsersApplication` | TA cannot withdraw another user's application. |
| TA-26 | `TaApplicationFlowTest` | `ta12_applyShouldRejectPriorityOutsideRange` | Priority must be 1, 2, or 3. |
| TA-27 | `TaApplicationFlowTest` | `ta12_applyShouldRejectDuplicatePriorityAmongActiveApplications` | Duplicate priority among active applications is rejected. |
| TA-28 | `TaApplicationFlowTest` | `ta12_applyShouldRejectWhenThreeActiveApplicationsAlreadyExist` | TA cannot exceed three active applications. |
| TA-29 | `TaApplicationFlowTest` | `ta12_applyShouldAllowReusingPriorityFromWithdrawnOrRejectedApplications` | Withdrawn/rejected applications do not block priority reuse. |

### 5.2 MO Tests

| Test ID | Test Class | Test Method | Evidence / Expected Result |
|---|---|---|---|
| MO-01 | `MoAuthStoriesTest` | `mo03_registerShouldCreateMoAccount` | MO registration creates a MO account. |
| MO-02 | `MoAuthStoriesTest` | `mo03_loginShouldAllowMoPortalAccess` | MO can log in through the MO portal. |
| MO-03 | `MoAuthStoriesTest` | `mo08_changePasswordShouldUpdateStoredPassword` | MO can change password with correct current password. |
| MO-04 | `MoAuthStoriesTest` | `mo08_changePasswordShouldRejectWrongOldPassword` | MO password change rejects wrong current password. |
| MO-05 | `MoAuthStoriesTest` | `mo09_forgotPasswordShouldCreateResetRequest` | MO forgot-password request is stored. |
| MO-06 | `MoJobManagementStoriesTest` | `mo01_createJobShouldAllowMoToPostTask` | MO can create a job posting. |
| MO-07 | `MoJobManagementStoriesTest` | `mo01_updateJobShouldAllowOwnerToEditPostedTask` | MO can update own job posting. |
| MO-08 | `MoJobManagementStoriesTest` | `mo01_deleteJobShouldRejectNonOwner` | MO cannot delete another MO's job. |
| MO-09 | `MoJobManagementStoriesTest` | `mo01_listJobsShouldSupportPostedByAndSearchFiltering` | Job list supports owner and search filtering. |
| MO-10 | `MoJobManagementStoriesTest` | `mo11_createJobShouldSupportDifferentCourseTypes` | Job creation supports different TA job types. |
| MO-11 | `MoApplicantReviewStoriesTest` | `mo06_viewApplicantsShouldReturnCvAndApplicantInfoForOwnJob` | MO can view applicants, CV filename, and applicant info for own job. |
| MO-12 | `MoApplicantReviewStoriesTest` | `mo06_viewApplicantsShouldRejectOtherMosJob` | MO cannot view applications for another MO's job. |
| MO-13 | `MoApplicantReviewStoriesTest` | `mo07_approveApplicantShouldUpdateStatusAndNotifyTa` | Approval updates status and notifies TA. |
| MO-14 | `MoApplicantReviewStoriesTest` | `mo07_rejectApplicantShouldUpdateStatusAndNotifyTa` | Rejection updates status and notifies TA. |
| MO-15 | `MoApplicantReviewStoriesTest` | `mo07_shouldRejectInvalidMoStatusChange` | Invalid MO status transition is rejected. |

### 5.3 Admin Tests

| Test ID | Test Class | Test Method | Evidence / Expected Result |
|---|---|---|---|
| AD-01 | `AdWorkloadAndStatsStoriesTest` | `ad01_workloadShouldShowOverloadedTaAndSupportStatusFilter` | Admin workload API detects overloaded TA and supports status/faculty filtering. |
| AD-02 | `AdUserManagementStoriesTest` | `ad02_superAdminShouldCreateUser` | Super admin can create a user. |
| AD-03 | `AdUserManagementStoriesTest` | `ad02_nonSuperAdminShouldNotCreateUser` | Standard admin is blocked from creating users. |
| AD-04 | `AdJobOversightStoriesTest` | `ad03_adminShouldUpdateAnyJobPosting` | Admin can update any job posting. |
| AD-05 | `AdJobOversightStoriesTest` | `ad03_adminShouldDeleteAnyJobPosting` | Admin can delete any job posting. |
| AD-06 | `AdJobOversightStoriesTest` | `ad03_adminShouldViewApplicationsForAnyJob` | Admin can view applications for any job. |
| AD-07 | `AdWorkloadAndStatsStoriesTest` | `ad04_statsShouldReturnApplicationAndUserSummary` | Admin statistics include application and user summary. |
| AD-08 | `AdWorkloadAndStatsStoriesTest` | `ad04_statsOpenJobsShouldReflectAllLiveJobsNotOnlyCreatedInPeriod` | Open job statistics remain accurate with date filters. |
| AD-09 | `AdSystemOperationsStoriesTest` | `ad05_exportShouldCreateExportTaskAndCsvFile` | Admin export creates export task and CSV file. |
| AD-10 | `AdSystemOperationsStoriesTest` | `ad06_auditLogsShouldSupportSearch` | Audit log API supports search. |
| AD-11 | `AdSystemOperationsStoriesTest` | `ad07_bulkNotificationShouldSendToTargetRoles` | Bulk notification is sent to selected roles. |
| AD-12 | `AdSystemOperationsStoriesTest` | `ad08_superAdminShouldUpdateSystemSettings` | Super admin can update system settings. |
| AD-13 | `AdUserManagementStoriesTest` | `ad09_superAdminShouldUpdateRoleAndActiveStatus` | Super admin can update user role and active status. |
| AD-14 | `AdSystemOperationsStoriesTest` | `ad10_approvePasswordResetShouldResetPasswordAndNotifyUser` | Approved password reset changes password and notifies user. |
| AD-15 | `AdUserManagementStoriesTest` | `ad11_userListShouldSupportSearchAndRoleFilter` | User management supports search and role filter. |
| AD-16 | `AdWorkloadAndStatsStoriesTest` | `ad12_statsShouldIncludePriorityAndQuotaSummary` | Statistics include priority and quota summary. |
| AD-17 | `AdUserManagementStoriesTest` | `ad13_shouldViewIndividualUserDetails` | Admin can view individual user details. |
| AD-18 | `AdminAdvancedOperationsStoriesTest` | `adminRoleTemplatesShouldSupportCreateListDetailUpdateDeleteAndAssignedCount` | Admin role templates support create/list/detail/update/delete and assignment count. |
| AD-19 | `AdminAdvancedOperationsStoriesTest` | `backupTasksShouldCreateListDownloadAndRetryBackupZip` | Backup tasks can be created, listed, downloaded, and retried. |
| AD-20 | `AdminAdvancedOperationsStoriesTest` | `exportTasksShouldListDownloadAndRetryFailedExports` | Export task history supports list, download, and retry. |
| AD-21 | `AdminAdvancedOperationsStoriesTest` | `passwordResetRejectAndEscalateShouldPersistReasonAndNotifySuperAdmin` | Rejected reset stores reason and escalation notifies super admin. |
| AD-22 | `AdminAdvancedOperationsStoriesTest` | `standardAdminShouldBeBlockedFromSuperAdminOperations` | Standard admin is blocked from super-admin-only settings and bulk notification operations. |

### 5.4 AI And Shared Tests

| Test ID | Test Class | Test Method | Evidence / Expected Result |
|---|---|---|---|
| AI-01 | `AiMatchingStoriesTest` | `aiMatchShouldExplainMatchedAndMissingSkillsForTa` | AI matching returns matched skills, missing skills, score, and recommendation. |
| AI-02 | `AiMatchingStoriesTest` | `moApplicationListShouldIncludeAiMatchSummary` | MO applicant list includes AI match summary. |
| SH-01 | `AuthUploadDraftCoverageTest` | `authShouldRejectSelfRegisteredAdminAccounts` | Public registration cannot create admin accounts. |
| SH-02 | `AuthUploadDraftCoverageTest` | `authLogoutShouldInvalidateSessionToken` | Logout invalidates the session token. |

## 6. Evidence From Latest Successful Run

Latest successful run result:

```text
70 tests found
70 tests successful
0 tests failed
```

This means all currently compiled JUnit test cases passed.

## 7. Limitations

These are unit/API-level tests. They do not fully replace manual or browser-based testing. The following behaviours should still be checked manually or with future end-to-end tests:

- Visual layout and responsive UI rendering.
- Browser-only storage such as `localStorage` and `IndexedDB`.
- PDF preview popup behaviour.
- Real user click flows across multiple pages.
- External AI API availability when `TA_AI_ENABLE_API=true` or `ta.ai.enableApi=true` is enabled.

For final evidence, a screenshot of the terminal after running the JUnit command can be included together with this document.
