package com.purrcare.app.models;

/**
 * One cat-related expense (amount in Bangladeshi Taka).
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class Expense {
    private String id;
    private String catId;
    private String catName;
    private String category;
    private double amount;
    private String date;
    private String description;

    /** Required by Firebase when it converts database data into this object. */
    public Expense() { }

    public Expense(String id, String catId, String catName, String category, double amount, String date, String description) {
        this.id = id;
        this.catId = catId;
        this.catName = catName;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCatId() { return catId; }
    public void setCatId(String catId) { this.catId = catId; }

    public String getCatName() { return catName; }
    public void setCatName(String catName) { this.catName = catName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

}
