package com.example.photoviewer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudySession {
    private int id;
    private String startTime;
    private String endTime;
    private boolean isActive;
    private int totalStudy;
    private int totalPhone;
    private int totalAway;
    private double focusScore;

    public StudySession(int id, String startTime, String endTime, boolean isActive,
                       int totalStudy, int totalPhone, int totalAway, double focusScore) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isActive = isActive;
        this.totalStudy = totalStudy;
        this.totalPhone = totalPhone;
        this.totalAway = totalAway;
        this.focusScore = focusScore;
    }

    public int getId() { return id; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public boolean isActive() { return isActive; }
    public int getTotalStudy() { return totalStudy; }
    public int getTotalPhone() { return totalPhone; }
    public int getTotalAway() { return totalAway; }
    public double getFocusScore() { return focusScore; }

    public String getFormattedDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy.MM.dd", Locale.getDefault());
            Date date = inputFormat.parse(startTime);
            return outputFormat.format(date);
        } catch (Exception e) {
            return startTime;
        }
    }

    public String getFormattedTimeRange() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            String start = timeFormat.format(inputFormat.parse(startTime));
            String end = endTime != null && !endTime.isEmpty() 
                ? timeFormat.format(inputFormat.parse(endTime)) 
                : "진행 중";
            return start + " ~ " + end;
        } catch (Exception e) {
            return startTime + " ~ " + (endTime != null ? endTime : "진행 중");
        }
    }
}

