package com.ubit.lms;

public class AttendanceItem {
    private String courseName;
    private int percentage;

    public AttendanceItem(String courseName, int percentage) {
        this.courseName = courseName;
        this.percentage = percentage;
    }

    public String getCourseName() {
        return courseName;
    }

    public int getPercentage() {
        return percentage;
    }
}