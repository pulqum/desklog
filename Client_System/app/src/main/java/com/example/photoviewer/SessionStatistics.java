package com.example.photoviewer;

public class SessionStatistics {
    private int totalStudy;
    private int totalPhone;
    private int totalAway;
    private double focusScore;
    private int totalCount;

    public SessionStatistics(int totalStudy, int totalPhone, int totalAway, double focusScore) {
        this.totalStudy = totalStudy;
        this.totalPhone = totalPhone;
        this.totalAway = totalAway;
        this.focusScore = focusScore;
        this.totalCount = totalStudy + totalPhone + totalAway;
    }

    public int getTotalStudy() { return totalStudy; }
    public int getTotalPhone() { return totalPhone; }
    public int getTotalAway() { return totalAway; }
    public double getFocusScore() { return focusScore; }
    public int getTotalCount() { return totalCount; }
}

