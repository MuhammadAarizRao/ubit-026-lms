package com.ubit.lms;

public class MarksRowItem {
    private String courseName;
    private double marks;

    public MarksRowItem(String courseName, double marks) {
        this.courseName = courseName;
        this.marks = marks;
    }

    public String getCourseName() { return courseName; }
    public double getMarks() { return marks; }
}