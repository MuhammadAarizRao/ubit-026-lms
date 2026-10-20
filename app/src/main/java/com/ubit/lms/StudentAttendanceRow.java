package com.ubit.lms;

public class StudentAttendanceRow {
    private String uid;
    private String name;
    private boolean present;

    public StudentAttendanceRow(String uid, String name, boolean present) {
        this.uid = uid;
        this.name = name;
        this.present = present;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
}