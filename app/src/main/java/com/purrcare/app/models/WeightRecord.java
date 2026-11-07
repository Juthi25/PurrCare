package com.purrcare.app.models;

/**
 * One weight measurement of one cat.
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class WeightRecord {
    private String id;
    private String catId;
    private String catName;
    private double weightKg;
    private String date;
    private String notes;

    /** Required by Firebase when it converts database data into this object. */
    public WeightRecord() { }

    public WeightRecord(String id, String catId, String catName, double weightKg, String date, String notes) {
        this.id = id;
        this.catId = catId;
        this.catName = catName;
        this.weightKg = weightKg;
        this.date = date;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCatId() { return catId; }
    public void setCatId(String catId) { this.catId = catId; }

    public String getCatName() { return catName; }
    public void setCatName(String catName) { this.catName = catName; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

}
