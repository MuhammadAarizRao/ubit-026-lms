package com.ubit.lms;

public class AssignmentItem {
    private String id;
    private String courseName;
    private String title;
    private String description;
    private String dueDateLabel;
    private long dueTimestampMillis;
    private boolean submitted;

    public AssignmentItem(String id, String courseName, String title, String description,
                          String dueDateLabel, long dueTimestampMillis, boolean submitted) {
        this.id = id;
        this.courseName = courseName;
        this.title = title;
        this.description = description;
        this.dueDateLabel = dueDateLabel;
        this.dueTimestampMillis = dueTimestampMillis;
        this.submitted = submitted;
    }

    public String getId() { return id; }
    public String getCourseName() { return courseName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDueDateLabel() { return dueDateLabel; }
    public long getDueTimestampMillis() { return dueTimestampMillis; }
    public boolean isSubmitted() { return submitted; }

    public boolean isOverdue() {
        return dueTimestampMillis > 0 && System.currentTimeMillis() > dueTimestampMillis;
    }
}