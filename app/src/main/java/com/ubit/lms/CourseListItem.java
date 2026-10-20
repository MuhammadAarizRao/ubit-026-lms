package com.ubit.lms;

public class CourseListItem {
    private String courseId;
    private String name;
    private String teacherName;

    public CourseListItem(String courseId, String name, String teacherName) {
        this.courseId = courseId;
        this.name = name;
        this.teacherName = teacherName;
    }

    public String getCourseId() { return courseId; }
    public String getName() { return name; }
    public String getTeacherName() { return teacherName; }
}