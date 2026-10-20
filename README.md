# UBIT-026 LMS — Developer README

This app is a native Android LMS (Java) with three portals — Student, Teacher, Admin — built on Firebase (Auth, Firestore, Storage, Cloud Functions). This doc explains what every file does and, more importantly, **exactly what to touch if you want to reuse this codebase for a different school/department** instead of UBIT.

---

## 1. Tech Stack & Setup

- **Android**: Java, XML layouts, Fragments + BottomNavigationView
- **Firebase**: Auth, Firestore (region `asia-south1`), Storage (requires **Blaze** plan — Spark/free plan cannot enable Storage), Cloud Functions (Node.js, 2nd gen) for push notifications via FCM
- **Firebase CLI** (separate from Android Studio) lives in its own folder, e.g. `C:\Users\<you>\FirebaseFunctions\<project>-functions`, deployed with `firebase deploy --only functions`

### To stand this up for a new organization from scratch:
1. Create a new Firebase project in console.firebase.google.com
2. Enable Authentication → Email/Password
3. Enable Firestore (production mode)
4. Upgrade to Blaze, set a budget alert (we used $1)
5. Enable Storage
6. Register the Android app with your new package name, download the new `google-services.json`, drop it in `app/`
7. Set up Firestore security rules (see Section 3)
8. Run `firebase init functions` in a separate folder, paste in the Cloud Function code (see Section 6), deploy

---

## 2. Firestore Schema

- **users/{uid}**: `name`, `uniId`, `role` (`admin`/`teacher`/`student`), `classRole` (`none`/`CR`/`GR`), `courses[]`, `mustChangePassword`, `semesterGpa`, `fcmToken`, `lastNotificationCheck`, `lastMaterialCheck`, `lastAssignmentCheck`, `section`, `semester`, `batch`
- **courses/{courseId}**: `name`, `teacherId`, `studentIds[]`, `crStudentId`, `grStudentId`, `targetBatch`, `targetSemester`, `targetSection` (used for auto-enrollment matching)
- **attendance/{courseId}/records/{date}**: `date`, `markedBy`, `entries{uid: "present"/"absent"}`
- **materials/{autoId}**: `courseId`, `courseName`, `title`, `fileUrl`, `sizeLabel`, `uploadedAt`
- **assignments/{autoId}**: `courseId`, `courseName`, `title`, `description`, `dueTimestamp`, `dueDateLabel`, `submittedBy[]`, `createdAt`, `submissionUrl_{uid}`, `submissionTime_{uid}`
- **marks/{studentUid}**: `subjects{courseId: score}` — GPA computed client-side as `avg% / 100 × 4`
- **notifications/{autoId}**: `courseId` (or `"general"`), `type`, `message`, `timestamp`
- **chats/{chatId}/messages/{msgId}**: `senderId`, `text`, `timestamp`. `chatId` = `{courseId}_{studentUid}` or `{courseId}_group`. Parent chat doc also stores `lastRead_{uid}`.

**Login convention:** UniID → email via `uniId.toLowerCase().replace("-", "") + "@ubit-lms.app"`. **To reuse for a new org, change the domain suffix** in `LoginActivity.java`, `AddUserActivity.java`, and `AssignCrGrActivity.java`'s `uniIdToEmail()` method — search for `@ubit-lms.app` across the project and replace every occurrence.

**Scalability note:** `courseId` strings (e.g. `course_se`) may contain underscores. Chat ID parsing therefore uses `lastIndexOf("_")`, not `split("_")[0]` — keep this in mind if you add new ID schemes.

---

## 3. Firestore Security Rules (current, simplified)

Most collections use `allow read, write: if isSignedIn()` rather than granular role checks. This was a deliberate simplification after repeated permission-denied errors when one role needed to write to another role's data (e.g., teacher updating a student's GPA). If you need tighter rules for a production/public release, expect to spend real time re-introducing granular checks and testing each write path again.

---

## 4. Full File Reference

### Shared / Login
| File | Purpose | Customize for new org? |
|---|---|---|
| `LoginActivity.java` | Login, UniID→email conversion, role routing, notification permission | Yes — email domain suffix |
| `SetPasswordActivity.java` | Forced password change on first login | No |
| `FcmTokenHelper.java` | Saves device push token | No |
| `MyFirebaseMessagingService.java` | Displays incoming push notifications | No |
| `PdfHelper.java` | Generates attendance PDFs | No |
| `GradeHelper.java` | Numeric mark → letter grade | Yes, if your grading scale differs |

### Student Portal
`StudentDashboardActivity`, `HomeFragment`, `CoursesFragment`, `TasksFragment`, `AlertsFragment` — tab container + 4 tabs. `AttendanceItem`/`Adapter`, `CircularProgressView`, `NotificationItem`/`Adapter`, `MaterialItem`, `AssignmentItem`/`Adapter`, `SubmitAssignmentActivity` — supporting models/screens.

### Chat
`ChatMessageItem`/`ChatAdapter`, `ChatActivity`, `ChatInboxActivity`.

### Teacher Portal
`TeacherDashboardActivity`, `TeacherCourseItem`/`Adapter`, `CourseDetailActivity` (per-course tab container), `TeacherAttendanceFragment`, `StudentAttendanceRow`/`AttendanceMarkingAdapter`, `CourseAttendanceActivity`, `TeacherMaterialsFragment`, `UploadMaterialActivity`, `TeacherAssignmentsFragment`, `ViewSubmissionsActivity`, `TeacherMarksFragment`, `TeacherNotificationsFragment`.

### Admin Portal
`AdminDashboardActivity` (3-tab container), `AdminHomeFragment`, `AdminCoursesFragment`, `AdminUsersFragment` (with Batch/Semester/Section filter dropdowns), `AddUserActivity` (creates real Auth + Firestore accounts via a **secondary Firebase app instance** so the admin doesn't get logged out — this pattern is reusable anywhere you need to create accounts while staying logged in as a different user), `AddCourseActivity` (also handles auto-enrollment matching), `AssignCrGrActivity`.

### Cloud Functions (separate Node.js project)
`index.js` — `sendChatNotification`, `notifyNewAssignment`, `notifyNewMaterial`, `notifyMarksUpdate` (fires only on first-ever marks doc creation, not updates — see Section 7), `notifyGeneralNotification`.

---

## 5. Branding / White-Label Checklist

To reskin this app for a different organization, touch these in order:

1. **App name & package**: `AndroidManifest.xml` (`android:label`), `build.gradle.kts` (`applicationId`), re-register the app in Firebase with the new package name
2. **Logo**: replace `res/drawable/ubit_logo` with the new org's logo (same filename, or update every `@drawable/ubit_logo` reference — it appears in Login, Student Home header, Teacher Home header, Admin Home)
3. **Background image**: replace `res/drawable/login_bg`
4. **Colors** — this is the fastest lever. Edit `res/values/colors.xml`:
   - `maroon_primary`, `maroon_dark` → your primary brand color + its darker shade
   - `cream_bg`, `card_cream` → your background/card colors
   - `text_muted` → your secondary text color
   All screens built after the redesign reference these color names, not hardcoded hex values, so changing these 5 lines re-themes most of the app. **Exception:** a handful of early screens (none remaining as of this writing, but check `activity_login.xml`'s Login button background and any file you haven't touched since) may still have hardcoded hex — search for stray `#1565C0` (old blue) as a sanity check.
5. **Email domain suffix**: `@ubit-lms.app` in `uniIdToEmail()` — see Section 2
6. **University/department name text**: hardcoded strings like "UBIT", "DEPARTMENT OF COMPUTER SCIENCE\nUNIVERSITY OF KARACHI" in `activity_teacher_dashboard.xml` and similar — search for "UBIT" and "Karachi" across `res/layout`
7. **Course ID naming convention**: currently `course_se`, `course_oop` style — purely a convention, no code depends on this exact format, but keep IDs consistent since `AssignCrGrActivity`/chat ID parsing splits on the **last** underscore
8. **Firestore data**: none of your existing data carries over automatically — a new Firebase project starts empty; you'll re-create Admin, test courses, etc. from scratch

---

## 6. Cloud Function Deployment (reference)

```
cd <your-functions-folder>
firebase deploy --only functions
```
First-ever deploy of a 2nd-gen function often fails once with a permission-propagation error — wait ~5 minutes and retry the same command; it typically succeeds the second time.

---

## 7. Known Lessons / Quirks (worth reading before extending)

1. **Composite indexes**: combining `whereEqualTo`/`whereIn` with `orderBy` in the same Firestore query usually requires a composite index. The app's current pattern: strip `orderBy` from the query, sort results client-side in a `.sort()` lambda instead, and always attach `.addOnFailureListener()` so a missing-index failure is visible instead of silently returning nothing.
2. **`whereIn` caps at 30 values** — used for filtering materials/assignments/notifications by a student's enrolled course list. Fine at current scale; would need pagination/batching past 30 enrolled courses.
3. **LinearLayout can't have two `match_parent`-width children in a `horizontal` orientation** — causes broken vertical text wrapping. Use `vertical` orientation when stacking full-width text.
4. **Material `Button` ignores `android:background`** — it auto-tints with the theme's primary color regardless of a custom drawable. Fix: use `com.google.android.material.button.MaterialButton` with `app:backgroundTint` (not `android:backgroundTint`), or add `android:backgroundTint="@null"` to a plain `Button` with `style="@style/Widget.AppCompat.Button"`.
5. **Dark mode broke the Material BottomNavigationView** when `android:textColorSecondary` was overridden globally (a Material internals bug, not app code). Resolution: the app is forced to light mode always, via `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)` in `LoginActivity.onCreate()`, plus `android:forceDarkAllowed="false"` in the manifest.
6. **`notifyMarksUpdate` Cloud Function only fires once** — it's an `onDocumentCreated` trigger, so it notifies the student the *first* time marks exist for them, but not on subsequent updates (since those use `.set(..., merge: true)` on an *existing* doc). Switch to `onDocumentWritten` if you want a notification on every mark change.
7. **File-paste mix-ups**: several bugs throughout this build came from pasting the wrong file's content into a similarly-named file (two different `item_student_marks_row.xml` purposes, swapped layout content between `activity_set_password.xml` and `activity_student_dashboard.xml`). When two files look alike, double-check contents after pasting, especially after a break.
8. **Auto-enrollment matching is exact-string, case-sensitive**: a course's `targetBatch`/`targetSemester`/`targetSection` must match a student's `batch`/`semester`/`section` character-for-character. "2022" ≠ "2022 " ≠ "Batch 2022". No normalization is currently applied.

---

## 8. Testing Checklist for a Fresh Deployment

- [ ] Create 1 Admin account manually (Firebase Console + Firestore)
- [ ] Admin creates 1 teacher, 1 student with matching Batch/Semester/Section
- [ ] Admin creates 1 course with the same target Batch/Semester/Section — confirm auto-enrollment Toast shows 1 student matched
- [ ] Teacher logs in, confirms course appears, marks attendance, uploads a material, posts an assignment, posts a notification, enters a mark
- [ ] Student logs in, confirms attendance %, the uploaded material, the assignment (with working Submit), the notification, and the GPA all show correctly — and that nothing from *other* courses leaks in
- [ ] Admin assigns CR/GR, confirms chat buttons appear on that student's Home tab and the Teacher's Course Panel
- [ ] Send a chat message each direction, confirm push notification arrives
- [ ] Repeat with a second teacher/course combination, confirm no cross-contamination between the two teachers' courses or students
