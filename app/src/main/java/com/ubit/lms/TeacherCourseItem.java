package com.ubit.lms;

import java.util.List;

public class TeacherCourseItem {
    private String courseId;
    private String name;
    private List<String> studentIds;
    private String targetSection;

    public TeacherCourseItem(String courseId, String name, List<String> studentIds, String targetSection) {
        this.courseId = courseId;
        this.name = name;
        this.studentIds = studentIds;
        this.targetSection = targetSection;
    }

    public String getCourseId() { return courseId; }
    public String getName() { return name; }
    public List<String> getStudentIds() { return studentIds; }
    public String getTargetSection() { return targetSection; }
}