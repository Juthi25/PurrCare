package com.purrcare.app.models;

/**
 * A cat profile (users/{uid}/cats/{catId}). The photo is stored as a small Base64 JPEG string.
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class Cat {
    private String id;
    private String name;
    private String photo;
    private String gender;
    private String dateOfBirth;
    private String breed;
    private double weight;
    private String notes;
    private long createdAt;

    /** Required by Firebase when it converts database data into this object. */
    public Cat() { }

    public Cat(String id, String name, String photo, String gender, String dateOfBirth, String breed, double weight, String notes, long createdAt) {
        this.id = id;
        this.name = name;
        this.photo = photo;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.breed = breed;
        this.weight = weight;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

}
