package com.example.photoviewer;

import android.graphics.Bitmap;

public class Post {
    private int id;
    private String title;
    private String text;
    private String publishedDate;
    private String category; // STUDY, PHONE, AWAY
    private Bitmap image;
    private String imageUrl;

    public Post(int id, String title, String text, String publishedDate, String category, Bitmap image, String imageUrl) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.publishedDate = publishedDate;
        this.category = category;
        this.image = image;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getPublishedDate() { return publishedDate; }
    public String getCategory() { return category; }
    public Bitmap getImage() { return image; }
    public String getImageUrl() { return imageUrl; }

    public String getFormattedTime() {
        try {
            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            java.util.Date date = inputFormat.parse(publishedDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return publishedDate;
        }
    }
}
