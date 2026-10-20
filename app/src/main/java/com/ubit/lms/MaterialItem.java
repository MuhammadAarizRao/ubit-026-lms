package com.ubit.lms;

public class MaterialItem {
    private String courseName;
    private String title;
    private String fileUrl;
    private String sizeLabel;

    public MaterialItem(String courseName, String title, String fileUrl, String sizeLabel) {
        this.courseName = courseName;
        this.title = title;
        this.fileUrl = fileUrl;
        this.sizeLabel = sizeLabel;
    }

    public String getCourseName() { return courseName; }
    public String getTitle() { return title; }
    public String getFileUrl() { return fileUrl; }
    public String getSizeLabel() { return sizeLabel; }
}