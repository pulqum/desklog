package com.example.photoviewer;

import android.graphics.Bitmap;

public class Post {
    private int id;
    private String title;
    private String text;
    private String publishedDate;
    private Bitmap image;

    public Post(int id, String title, String text, String publishedDate, Bitmap image) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.publishedDate = publishedDate;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public Bitmap getImage() {
        return image;
    }
}
