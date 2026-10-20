# UBIT-026 LMS — Scaling Strategy: Section, Semester, Batch

This doc covers one specific piece of the system: how the app scaled from "one course, one student, tested by hand" to "many courses across different batches, semesters, and sections, enrolling automatically." It explains what was built, why, what the real tradeoffs are, and what to consider if you want to push this further.

---

## 1. The problem this solved

Early in the build, every course's `studentIds` array was filled in **manually**, by editing Firestore directly. That was fine for testing with one course and one or two students, but it doesn't scale: a real deployment might have 4 batches × 8 semesters × 2 sections × 8 teachers × dozens of students, and nobody should be hand-editing arrays in a database console to enroll each one.

At the same time, two other things were still broken at any scale beyond "one course total":
- Materials, Assignments, and course-tagged Notifications were being fetched **globally** — every student saw every course's content, not just their own.
- There was no way to look at the Admin's Users list and make sense of more than a handful of accounts.

The fix needed three parts: a way to *describe* which students a course is for, a way to *automatically* connect matching students to that course in both directions, and a way to make sure each student's screens only show *their own* data.

---

## 2. How it was actually built

### 2.1 Three new fields, stored independently

Both `users/{uid}` and `courses/{courseId}` gained three string fields: `section`, `semester`, `batch` (on courses, named `targetSection`, `targetSemester`, `targetBatch` to make clear they describe who the course is *for*, not a property of the course document itself).

Each field defaults to the literal string `"N/A"` if left blank — **independently of the other two**. Leaving Section blank doesn't force Semester or Batch blank too; each is checked and defaulted on its own in code:
```java
user.put("section", section.isEmpty() ? "N/A" : section);
user.put("semester", semester.isEmpty() ? "N/A" : semester);
user.put("batch", batch.isEmpty() ? "N/A" : batch);
```
This matters for the filtering and sorting described below — if these were collapsed into one shared default, an account missing just one field would be indistinguishable from an account missing all three.

### 2.2 Two-way automatic enrollment

Because test data gets created in whichever order is convenient (sometimes a course first, sometimes a student first), enrollment matching was built to run **both directions**:

- **Course created → match existing students.** `AddCourseActivity.enrollMatchingStudents()` queries `users` where `role == "student"` AND `batch/semester/section` all equal the course's target values, then writes the resulting UID list straight into the new course's `studentIds`.
- **Student created → match existing courses.** `AddUserActivity.joinMatchingCourses()` (only runs when `role == "student"`) queries `courses` where `targetBatch/targetSemester/targetSection` all equal the new student's values, then does `arrayUnion(uid)` onto every matching course's `studentIds` — a loop over possibly several matching courses, since the same batch/semester/section typically takes multiple courses at once.

Both are **exact string matches on all three fields simultaneously** — a student only joins a course if Batch, Semester, *and* Section all match exactly. There is currently no partial matching (e.g. "same batch and semester, any section").

### 2.3 Student-side screens filtered by actual enrollment

With enrollment now real, the screens that read course content were rewritten to only show content from courses the logged-in student is actually enrolled in:
- `CoursesFragment` and `TasksFragment` both first query `courses` with `whereArrayContains("studentIds", uid)` to get the student's enrolled course IDs, then query `materials`/`assignments` with `whereIn("courseId", enrolledCourseIds)`.
- `HomeFragment`'s notification preview and `AlertsFragment`'s full feed do the same, but also always include `"general"` in the allowed course ID list, so broadcast-to-everyone notifications still reach every student regardless of enrollment.

`whereIn` caps at 30 values per query in current Firestore SDKs — fine unless a single student is ever enrolled in more than 30 courses at once.

### 2.4 Admin visibility: filters on the Users list

`AdminUsersFragment` loads every user once, builds three `TreeSet`s of whatever Batch/Semester/Section values *actually exist* in the data (plus "N/A"), populates three Spinners from those real values (never a hardcoded list that might not match reality), and re-filters the displayed list client-side whenever any Spinner changes. Each user row also now shows its own Section/Semester/Batch line, so Admin can visually scan without even using the filters.

---

## 3. Known limitations, as-built

- **Exact string match only, case-sensitive, no trimming beyond the initial `.trim()` on input.** "2022" and "2022 " (trailing space) would not match. "A" and "a" would not match. This is the single most likely source of "why didn't this student get enrolled" support questions once the system is in real use.
- **No partial/fuzzy matching.** A course can't currently target "all of Batch 2022, any section" — it always requires all three fields to match.
- **No re-matching on edit.** If an Admin edits a student's Batch *after* account creation, or edits a course's target values after creation, nothing automatically re-runs the matching. Enrollment is a one-time event at creation time for both directions.
- **No unenroll-on-mismatch.** If a student's Batch is edited to no longer match a course they were previously auto-enrolled in, they stay enrolled. Enrollment, once granted, is sticky.
- **No manual override UI.** There's no screen to manually add or remove one specific student from one specific course outside of the automatic matching — if the automatic match gets something wrong, fixing it currently means editing Firestore directly.
- **Filtering fetches all users/courses client-side, then filters in memory.** Fine at dozens-to-low-hundreds of users; would need real server-side filtered queries (or pagination) at much larger scale.

---

## 4. Suggestions for extending this further

Roughly in order of how likely you are to actually need them soon:

1. **Normalize input before matching.** Lowercase and trim Batch/Semester/Section on both the student and course side before comparing (or before saving, so the stored value is already normalized). Cheapest fix with the highest practical payoff — removes the entire "silent mismatch" failure class.
2. **A manual enrollment screen.** A simple "Manage Students" screen per course — open a course, see a checklist of all students (optionally still pre-filtered by the course's target Batch/Semester/Section as a starting point), tick/untick individually. This gives Admin an escape hatch when automatic matching isn't quite right, without touching Firestore by hand.
3. **Dropdowns instead of free text for Batch/Semester/Section**, populated from whatever values already exist in the system (same `TreeSet`-from-real-data trick already used for the Users list filters). Eliminates typos entirely, at the cost of needing at least one existing value before a dropdown has anything to offer — would need a sensible bootstrap for the very first course/student ever created.
4. **Re-run matching on edit**, not just creation — if this becomes a real workflow (students changing sections mid-semester, etc.), trigger `enrollMatchingStudents`/`joinMatchingCourses`-style logic whenever a user's or course's Batch/Semester/Section fields are updated, not only when the document is first created.
5. **A Cloud Function instead of client-side matching**, if multiple Admin devices might ever create users/courses concurrently — the current matching runs entirely on the Admin's device at creation time, which is simple and has worked fine at the tested scale, but a server-side Firestore trigger would be more robust against race conditions as usage grows.
6. **Course codes and richer metadata**, separate from this scaling work but adjacent to it: the reference UI design included a course tag like "CS-601 • CORE SUBJECT" that was deliberately left out since there was no metadata to back it. Adding a `courseCode` and `subjectType` field alongside the existing target fields would unlock that.
7. **Section/Semester/Batch on Teacher accounts** were added for consistency (same fields, same form), but nothing currently *uses* a teacher's own Batch/Semester/Section for any matching or filtering logic — it's purely informational right now. Worth deciding whether that should stay descriptive-only or eventually mean something (e.g., a teacher only assignable to courses targeting their own batch/semester range, for a TA-style role).

---

## 5. Suggested test matrix (if you haven't already run it)

The scale this was built and reasoned about at: **4 batches × 8 semesters × 2 sections, 8 teachers, 24 students**, distributed across different combinations. A good pass of this test should confirm:
- Every student sees only their own enrolled courses' materials/assignments/notifications — zero leakage from other batch/semester/section combinations
- Every teacher sees only their own assigned courses — zero leakage from other teachers' courses
- Admin's Users list filters correctly narrow down to the exact subset expected for any Batch/Semester/Section combination
- Firestore composite index prompts (if any appear at this volume) get created once and don't recur
