# UBIT-026 LMS — Hard Bugs, Fixes, and File Map

Companion to the main README. This one is for debugging and onboarding a new developer into the actual codebase structure.

---

## Part 1: The Hardest Bugs and How They Were Fixed

### 1. Firestore composite index silently killing queries
**Symptom:** Teacher posts an assignment/notification, it saves fine, but never appears in the list below the form. No error shown.
**Cause:** Queries combining `.whereEqualTo("courseId", x)` with `.orderBy("createdAt", DESC)` require a Firestore composite index. Without one, the query throws — but no `.addOnFailureListener()` was attached, so the failure was invisible.
**Fix:** Removed `orderBy` from the query entirely; fetch with just the `where` clause, pull results into a `List`, and sort them client-side with `.sort((a,b) -> ...)`. Added `.addOnFailureListener()` everywhere so any future query failure shows a Toast instead of failing silently.
**Where this pattern lives:** `TeacherAssignmentsFragment.loadAssignments()`, `TasksFragment.loadAssignments()`, `TeacherNotificationsFragment.loadNotifications()`.

### 2. File-paste mix-ups between similarly structured XML files
**Symptom:** A screen that was working perfectly suddenly shows the wrong fields, or `findViewById` crashes with a null pointer.
**Cause:** Two files edited back-to-back in the same session, built from similar templates — content from one got pasted into the other's file by mistake. Happened with `activity_set_password.xml` / `activity_student_dashboard.xml`, and separately with `item_student_marks_row.xml` being built twice for two different screens under the same filename.
**Fix:** No code fix — just discipline: after any paste, re-open the file and visually confirm the content matches what was intended before moving on. When the same filename is needed for two different purposes, give it two different names instead (`item_student_marks_row.xml` vs `item_marks_summary_row.xml`).

### 3. Security rules blocking legitimate cross-role writes
**Symptom:** `PERMISSION_DENIED` when a Teacher tried to update a Student's GPA, or a Student tried to update `submittedBy` on an assignment doc owned conceptually by the Teacher.
**Cause:** Firestore rules originally restricted `users/{uid}` writes to the user themselves or Admin only — but Teachers legitimately need to write to a student's `semesterGpa`. Same pattern hit `assignments` (Teacher-only write blocked a Student's own submission).
**Fix:** Rules were broadened to `allow read, write: if isSignedIn()` on the collections where this kept recurring, rather than maintaining an increasingly complex matrix of per-field, per-role conditions. This trades some theoretical security tightness for reliability — flagged in the main README as something to revisit before any public/production release.

### 4. `ClassCastException` after changing a Button to a styled LinearLayout/MaterialButton
**Symptom:** App crashes immediately on opening a screen, `ClassCastException: LinearLayout cannot be cast to Button`.
**Cause:** During the UI redesign, several `<Button>` elements were swapped for `<LinearLayout>` (to get custom pill shapes `android:background` can't achieve on a real Button) — but the corresponding `.java` file still declared `private Button someButton;` and tried to cast the `findViewById` result.
**Fix:** Change the Java field's declared type to `View` (or later, properly to `MaterialButton` once we learned that component exists and supports custom shapes natively). This is the single most common crash during the entire UI redesign phase — **always update the Java type immediately when a layout's widget type changes.**

### 5. `MaterialButton`/`Button` ignoring `android:background`
**Symptom:** Buttons that were supposed to show a custom maroon gradient or cream pill instead rendered as solid theme-primary-colored rectangles, sometimes with invisible same-color text on top.
**Cause:** Android's Material components apply an automatic tint on top of `android:background`, overriding custom drawables.
**Fix:** Switch to `com.google.android.material.button.MaterialButton` and style via `app:backgroundTint`, `app:cornerRadius`, `app:strokeColor`/`app:strokeWidth` (for outline buttons) instead of `android:background`. For the one remaining plain `<Button>` (Login), the fix was `style="@style/Widget.AppCompat.Button"` + `android:backgroundTint="@null"`.

### 6. Dark mode crash inside Google's Material library
**Symptom:** Adding `android:textColorSecondary` as a theme override (to fix unreadable Spinner/label text in dark mode) caused an immediate crash: `Resources$NotFoundException: Resource ID #0x0`, traced into `BottomNavigationMenuView`'s internals.
**Cause:** A known fragile interaction between that specific theme attribute and Material's auto-generated icon tint color list — not app code, a library quirk.
**Fix:** Abandoned trying to theme dark mode at all. The app now forces light mode globally via `AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)` (set at the top of `LoginActivity.onCreate()`, before `super.onCreate()`) plus `android:forceDarkAllowed="false"` in the manifest's `<application>` tag.

### 7. Cloud Function parsing the wrong part of a chat ID
**Symptom:** Chat push notifications silently never arrived, despite the function showing successful invocations in the Firebase dashboard.
**Cause:** `chatId` format is `{courseId}_{studentUid}` or `{courseId}_group` — but `courseId` itself contains underscores (e.g. `course_se`). The function used `chatId.split("_")[0]`, which only captured `"course"`, not `"course_se"`, so the course lookup always failed and the function exited early with no recipients found.
**Fix:** `const lastUnderscoreIndex = chatId.lastIndexOf("_"); const courseId = chatId.substring(0, lastUnderscoreIndex);` — split from the *last* underscore, not the first.

### 8. First Cloud Functions deploy always fails once
**Symptom:** `firebase deploy --only functions` fails with an Eventarc/permission error on the very first deploy of a 2nd-gen function to a brand-new project.
**Cause:** Deploying triggers Google Cloud to provision several backend services (Eventarc, Cloud Run, Pub/Sub) behind the scenes; their permissions take a few minutes to fully propagate even though the CLI reports them as "enabled" immediately.
**Fix:** Just wait ~5 minutes and re-run the identical deploy command. Succeeds on the second attempt essentially every time. The CLI's own error message says as much.

### 9. Stale Cloud Function code deployed because the file was never saved
**Symptom:** A fix to `index.js` (the `lastIndexOf` fix above) appeared correct in the editor, deploy succeeded, but the bug persisted exactly as before.
**Cause:** The file was edited in a text editor but never actually saved (Ctrl+S) before running `firebase deploy`. The CLI silently deployed whatever was already on disk — the old buggy version.
**Fix:** No code fix — just a reminder to explicitly save and, when in doubt, close and reopen the file to confirm the edit persisted before deploying.

### 10. Attendance % and the keyboard-pushing-nav-bar issue
Two smaller but instructive layout bugs:
- **Attendance %, early on, was a static manually-set field** (`attendanceSummary` map on the user doc) rather than computed from real daily records — meaning Teacher marking real attendance had zero effect on what Student saw. Fixed by rewriting `HomeFragment.loadRealAttendance()` to query `attendance/{courseId}/records` directly, count present-days vs total-days per enrolled course, and compute the percentage live.
- **Typing in a per-course tab's input field pushed the bottom navigation bar up** with the keyboard, since the default `adjustResize` window mode resizes the whole screen (including the nav bar at its bottom) to fit above the keyboard. Fixed with `android:windowSoftInputMode="adjustPan"` on `CourseDetailActivity`'s manifest entry, which pans content instead of resizing it.

---

## Part 2: Full File Map

Each entry: **what it is**, in two lines, and **what it connects to** (other files, Firestore collections, or external services).

### Login & Shared
- **LoginActivity.java** — Login screen logic: converts UniID to a synthetic email, authenticates, reads the user's role and routes accordingly. Connects to: `activity_login.xml`, Firebase Auth, `users` collection, `FcmTokenHelper`, routes to `SetPasswordActivity`/`AdminDashboardActivity`/`TeacherDashboardActivity`/`StudentDashboardActivity`.
- **SetPasswordActivity.java** — Forces a password change on first login, then updates the `mustChangePassword` flag. Connects to: `activity_set_password.xml`, Firebase Auth, `users` collection, routes to `StudentDashboardActivity`.
- **FcmTokenHelper.java** — Static helper that fetches the device's FCM token and saves it onto the logged-in user's Firestore doc. Connects to: `users` collection (`fcmToken` field), called from `LoginActivity`.
- **MyFirebaseMessagingService.java** — Receives incoming push notifications from FCM and displays them as Android notifications. Connects to: the Cloud Functions that send pushes (chat, assignments, materials, marks, notifications).
- **PdfHelper.java** — Static helper that generates a simple text-based PDF and saves it to the device's Downloads folder. Connects to: `TeacherAttendanceFragment`, `CourseAttendanceActivity` (both call it for attendance PDFs).
- **GradeHelper.java** — Static helper converting a 0–100 numeric mark into a letter grade. Connects to: `HomeFragment` (student marks card display).

### Student Portal
- **StudentDashboardActivity.java** — Container holding the bottom nav and switching between the 4 student fragments; also owns the nav-badge logic for unread materials/assignments/notifications. Connects to: `HomeFragment`, `CoursesFragment`, `TasksFragment`, `AlertsFragment`, `users` collection (`lastMaterialCheck`/`lastAssignmentCheck`/`lastNotificationCheck`), `materials`/`assignments`/`notifications` collections.
- **HomeFragment.java** — Home tab: greeting, Section/Semester/Batch, live attendance, GPA, semester marks breakdown, notification preview, CR/GR chat buttons. Connects to: `users`, `courses`, `attendance`, `marks`, `notifications` collections; `AttendanceAdapter`, `NotificationAdapter`, `item_marks_summary_row.xml`, `ChatActivity`.
- **CoursesFragment.java** — Courses tab: materials grouped by course, filtered to the student's actual enrolled courses. Connects to: `courses` (for enrolled course IDs), `materials` collection; `item_course_group.xml`, `item_material_row.xml`.
- **TasksFragment.java** — Tasks tab: assignment list filtered to enrolled courses, pending/submitted counts. Connects to: `courses`, `assignments` collections; `AssignmentAdapter`, routes to `SubmitAssignmentActivity`.
- **AlertsFragment.java** — Alerts tab: full notification feed filtered to enrolled courses + general. Connects to: `courses`, `notifications` collections; `NotificationAdapter`.
- **AttendanceItem.java / AttendanceAdapter.java** — Model and RecyclerView adapter for the circular attendance rings on Home. Connects to: `CircularProgressView`, `item_attendance_card.xml`.
- **CircularProgressView.java** — Custom `View` that draws the color-coded circular attendance ring (green/orange/red by percentage). Connects to: `AttendanceAdapter`.
- **NotificationItem.java / NotificationAdapter.java** — Model and adapter for notification cards, used on both Home and Alerts. Connects to: `item_notification_card.xml`.
- **MaterialItem.java** — Plain model for one course material (course, title, file URL, size). Connects to: `CoursesFragment`.
- **AssignmentItem.java / AssignmentAdapter.java** — Model (with overdue logic) and adapter for assignment cards; handles expand/collapse of description and Submit/Submitted/Closed button states. Connects to: `item_assignment_card.xml`, routes to `SubmitAssignmentActivity`.
- **SubmitAssignmentActivity.java** — File picker + Firebase Storage upload flow for submitting an assignment. Connects to: `assignments` collection (`submittedBy`, `submissionUrl_{uid}`, `submissionTime_{uid}`), Firebase Storage `submissions/` path.

### Chat
- **ChatMessageItem.java / ChatAdapter.java** — Model and adapter for chat bubbles, styled by sender. Connects to: `item_chat_message.xml`, `chat_bubble_sent.xml`/`chat_bubble_received.xml`.
- **ChatActivity.java** — The actual chat screen: sends messages, listens live via `addSnapshotListener`, marks the chat read. Connects to: `chats/{chatId}/messages` subcollection, `chats/{chatId}` parent doc (`lastRead_{uid}`).
- **ChatInboxActivity.java** — Lists every conversation for the logged-in user (Teacher: per-course CR/GR/group; Student: their own), with unread dots and last-message preview. Connects to: `courses`, `chats` collections; routes to `ChatActivity`.

### Teacher Portal
- **TeacherDashboardActivity.java** — Teacher's landing screen: header with Section/Semester/Batch, "My Chats" entry, course list. Connects to: `users`, `courses` collections; `TeacherCourseAdapter`, routes to `ChatInboxActivity`.
- **TeacherCourseItem.java / TeacherCourseAdapter.java** — Model and adapter for the teacher's course cards. Connects to: `item_teacher_course_card.xml`, routes to `CourseDetailActivity`.
- **CourseDetailActivity.java** — Container for one course: maroon header, Chat CR/GR/Group pills, 5-tab bottom nav. Connects to: `courses` collection (for CR/GR lookup), all 5 teacher fragments below, `ChatActivity`.
- **TeacherAttendanceFragment.java** — Attendance tab: packed behind a "Mark Attendance" button; student list, Save, PDF export, link to full history. Connects to: `courses` (enrolled students), `users` (names), `attendance` collection; `AttendanceMarkingAdapter`, `PdfHelper`, routes to `CourseAttendanceActivity`.
- **StudentAttendanceRow.java / AttendanceMarkingAdapter.java** — Model and adapter for marking attendance, one row per student with a Switch toggle. Connects to: `item_attendance_student_row.xml`.
- **CourseAttendanceActivity.java** — Full attendance history, collapsed by date (tap to expand), full-history PDF export. Connects to: `attendance/{courseId}/records`, `users` (name resolution); `PdfHelper`.
- **TeacherMaterialsFragment.java** — Materials tab: upload button + list of this course's materials. Connects to: `materials` collection; routes to `UploadMaterialActivity`.
- **UploadMaterialActivity.java** — File picker + Firebase Storage upload for course materials. Connects to: `materials` collection, Firebase Storage `materials/` path.
- **TeacherAssignmentsFragment.java** — Assignments tab: post form with date picker, list of posted assignments with submission counts. Connects to: `assignments` collection; routes to `ViewSubmissionsActivity`.
- **ViewSubmissionsActivity.java** — Lists students who submitted an assignment, with download links and submission timestamps. Connects to: `assignments` collection (`submissionUrl_{uid}`, `submissionTime_{uid}`), `users` (name resolution).
- **TeacherMarksFragment.java** — Marks tab: per-student mark entry, triggers GPA recalculation on save. Connects to: `courses` (enrolled students), `marks` collection, `users` (`semesterGpa` field).
- **TeacherNotificationsFragment.java** — Notifications tab: post form + list of this course's posted notifications. Connects to: `notifications` collection.

### Admin Portal
- **AdminDashboardActivity.java** — Container for Admin's 3-tab bottom nav. Connects to: `AdminHomeFragment`, `AdminCoursesFragment`, `AdminUsersFragment`.
- **AdminHomeFragment.java** — Dashboard tab: Add User / Add Course pill buttons, logo. Connects to: routes to `AddUserActivity`, `AddCourseActivity`.
- **AdminCoursesFragment.java** — Courses tab: list of all courses with teacher name and an Assign CR/GR link. Connects to: `courses`, `users` collections; routes to `AssignCrGrActivity`.
- **AdminUsersFragment.java** — Users tab: full user list with Batch/Semester/Section filter dropdowns built from real data present. Connects to: `users` collection; `item_user_row.xml`.
- **AddUserActivity.java** — Creates a real Firebase Auth account + Firestore profile via a **secondary Firebase app instance** (so Admin's own session isn't logged out); for students, also auto-enrolls into any course whose target Batch/Semester/Section matches. Connects to: Firebase Auth (secondary instance), `users` collection, `courses` collection (auto-enroll).
- **AddCourseActivity.java** — Creates a course doc with target Batch/Semester/Section, assigns a teacher, and auto-enrolls all currently-matching students. Connects to: `courses`, `users` collections.
- **AssignCrGrActivity.java** — Assigns Class Representative / Girls Representative from the course's enrolled student list. Connects to: `courses` collection (`crStudentId`, `grStudentId`), `users` (name resolution).

### Cloud Functions (separate Node.js project)
- **index.js** — `sendChatNotification` (new chat message → push to the other party/parties), `notifyNewAssignment`, `notifyNewMaterial`, `notifyMarksUpdate` (first-time marks doc only — see bug #6 equivalent note in main README), `notifyGeneralNotification` (course-scoped or, if `courseId === "general"`, every student). Connects to: Firestore triggers on `chats/*/messages/*`, `assignments/*`, `materials/*`, `marks/*`, `notifications/*`; sends via FCM to `users/{uid}.fcmToken`.

### Supporting Resources (grouped — not one-by-one)
- **Drawables**: `cream_card_bg`, `maroon_pill_button`, `maroon_outline_pill`, `stat_card_bg`, `white_pill_bg`, `badge_red_bg`, `badge_pill_bg`, `header_gradient`, `maroon_header_rounded`, `circle_translucent_bg`, `cream_pill_bg`, `chat_bubble_sent`/`received`, `unread_dot`, `circle_icon_bg`, `rounded_input_card` — shared shape/color backgrounds referenced across almost every screen above; editing one changes every screen using it.
- **Menus**: `bottom_nav_menu.xml` (student, 4 tabs), `bottom_nav_menu_teacher.xml` (per-course, 5 tabs), `bottom_nav_menu_admin.xml` (3 tabs).
- **colors.xml**: `maroon_primary`, `maroon_dark`, `navy_text`, `soft_pink_bg`, `cream_bg`, `card_cream`, `text_muted` — the entire app's color palette in one place.
- **nav_item_color.xml**: color-state selector making the active bottom nav tab maroon and inactive tabs muted rose-grey.

---

## Part 3: Current Firestore Schema (reference copy)

```
users/{uid}
  name, uniId, role, classRole, courses[], mustChangePassword,
  semesterGpa, fcmToken, lastNotificationCheck, lastMaterialCheck,
  lastAssignmentCheck, section, semester, batch

courses/{courseId}
  name, teacherId, studentIds[], crStudentId, grStudentId,
  targetBatch, targetSemester, targetSection

attendance/{courseId}/records/{date}
  date, markedBy, entries{uid: "present"|"absent"}

materials/{autoId}
  courseId, courseName, title, fileUrl, sizeLabel, uploadedAt

assignments/{autoId}
  courseId, courseName, title, description, dueTimestamp, dueDateLabel,
  submittedBy[], createdAt, submissionUrl_{uid}, submissionTime_{uid}

marks/{studentUid}
  subjects{ courseId: score }

notifications/{autoId}
  courseId ("general" for all-student), type, message, timestamp

chats/{chatId}/messages/{msgId}
  senderId, text, timestamp
  (chatId = "{courseId}_{studentUid}" or "{courseId}_group")
chats/{chatId}
  lastRead_{uid}  (parent doc, written by ChatActivity on open)
```
