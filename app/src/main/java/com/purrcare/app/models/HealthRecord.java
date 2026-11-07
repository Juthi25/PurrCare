package com.purrcare.app.models;

/**
 * A health record (vaccination, deworming, medicine, vet visit...).
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class HealthRecord {
    private String id;
    private String catId;
    private String catName;
    private String title;
    private String type;
    private String date;
    private String nextDueDate;
    private String description;
    private String medicineName;

    /** Required by Firebase when it converts database data into this object. */
    public HealthRecord() { }

    public HealthRecord(String id, String catId, String catName, String title, String type, String date, String nextDueDate, String description, String medicineName) {
        this.id = id;
        this.catId = catId;
        this.catName = catName;
        this.title = title;
        this.type = type;
        this.date = date;
        this.nextDueDate = nextDueDate;
        this.description = description;
        this.medicineName = medicineName;
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

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(String nextDueDate) { this.nextDueDate = nextDueDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

}
