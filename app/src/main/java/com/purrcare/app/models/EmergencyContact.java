package com.purrcare.app.models;

/**
 * An emergency phone contact (vet, family...).
 * Firebase needs: a public empty constructor + getters/setters for every field.
 */
public class EmergencyContact {
    private String id;
    private String name;
    private String phone;

    /** Required by Firebase when it converts database data into this object. */
    public EmergencyContact() { }

    public EmergencyContact(String id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

}
