package com.purrcare.app.models;

/**
 * A feeding schedule entry. lastCompletedDate equals today's date when the feeding is done.
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class FoodSchedule {
    private String id;
    private String catId;
    private String catName;
    private String foodName;
    private String foodType;
    private String feedingTime;
    private String quantity;
    private String notes;
    private String lastCompletedDate;

    /** Required by Firebase when it converts database data into this object. */
    public FoodSchedule() { }

    public FoodSchedule(String id, String catId, String catName, String foodName, String foodType, String feedingTime, String quantity, String notes, String lastCompletedDate) {
        this.id = id;
        this.catId = catId;
        this.catName = catName;
        this.foodName = foodName;
        this.foodType = foodType;
        this.feedingTime = feedingTime;
        this.quantity = quantity;
        this.notes = notes;
        this.lastCompletedDate = lastCompletedDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCatId() { return catId; }
    public void setCatId(String catId) { this.catId = catId; }

    public String getCatName() { return catName; }
    public void setCatName(String catName) { this.catName = catName; }

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }

    public String getFeedingTime() { return feedingTime; }
    public void setFeedingTime(String feedingTime) { this.feedingTime = feedingTime; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(String lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }

}
