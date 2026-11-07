package com.purrcare.app.models;

/**
 * A growth diary memory with a photo.
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class DiaryEntry {
    private String id;
    private String catId;
    private String catName;
    private String title;
    private String description;
    private String date;
    private String photo;

    /** Required by Firebase when it converts database data into this object. */
    public DiaryEntry() { }

    public DiaryEntry(String id, String catId, String catName, String title, String description, String date, String photo) {
        this.id = id;
        this.catId = catId;
        this.catName = catName;
        this.title = title;
        this.description = description;
        this.date = date;
        this.photo = photo;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCatId() { return catId; }
    public void setCatId(String catId) { this.catId = catId; }

    public String getCatName() { return catName; }
    public void setCatName(String catName) { this.catName = catName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

}
