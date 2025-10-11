package com.example.bridge.models;

public class PagerItem {
    private int image;
    private String title;
    private String subTitle;
    private int stroke;
    private int iconTint;

    public PagerItem(int image, String title, String subTitle, int stroke, int iconTint) {
        this.image = image;
        this.title = title;
        this.subTitle = subTitle;title = title;
        this.stroke = stroke;
        this.iconTint = iconTint;
    }
    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStroke() {
        return stroke;
    }

    public void setStroke(int stroke) {
        this.stroke = stroke;
    }

    public int getIconTint() {
        return iconTint;
    }

    public void setIconTint(int iconTint) {
        this.iconTint = iconTint;
    }

    public String getSubTitle() {
        return subTitle;
    }
    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }
}
