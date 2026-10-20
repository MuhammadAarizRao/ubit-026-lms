package com.ubit.lms;

public class NotificationItem {
    private String courseTag;
    private String type;
    private String message;
    private String timeAgo;

    public NotificationItem(String courseTag, String type, String message, String timeAgo) {
        this.courseTag = courseTag;
        this.type = type;
        this.message = message;
        this.timeAgo = timeAgo;
    }

    public String getCourseTag() { return courseTag; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getTimeAgo() { return timeAgo; }
}