package com.purrcare.app.models;

/**
 * A reminder. dateTimeMillis is the due time in milliseconds.
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class Reminder {
    private String id;
    private String catId;
    private String catName;
    private String title;
    private String type;
    private long dateTimeMillis;
    private String notes;
    private boolean completed;

    /** Required by Firebase when it converts database data into this object. */
    public Reminder() { }

    public Reminder(String id, String catId, String catName, String title, String type, long dateTimeMillis, String notes, boolean completed) {
        this.id = id;
        this.catId = catId;
        this.catName = catName;
        this.title = title;
        this.type = type;
        this.dateTimeMillis = dateTimeMillis;
        this.notes = notes;
        this.completed = completed;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCatId() { return catId; }
    public void setCatId(String catId) { this.catId = catId; }

    public String getCatName() { return catName; }
    public void setCatName(String catName) { this.catName = catName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getDateTimeMillis() { return dateTimeMillis; }
    public void setDateTimeMillis(long dateTimeMillis) { this.dateTimeMillis = dateTimeMillis; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

}
