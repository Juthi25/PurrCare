package com.purrcare.app.utils;

/** All fixed names/choices in one place so they are never mistyped in different files. */
public final class Constants {
    private Constants() { }

    // Firebase Realtime Database node names under users/{uid}/
    public static final String NODE_PROFILE = "profile";
    public static final String NODE_CATS = "cats";
    public static final String NODE_HEALTH = "healthRecords";
    public static final String NODE_FOOD = "foodSchedules";
    public static final String NODE_REMINDERS = "reminders";
    public static final String NODE_WEIGHT = "weightRecords";
    public static final String NODE_EXPENSES = "expenses";
    public static final String NODE_DIARY = "diary";
    public static final String NODE_CONTACTS = "emergencyContacts";
    public static final String NODE_SETTINGS = "settings";

    /** Nodes whose records belong to one cat (deleted together with the cat). */
    public static final String[] CAT_RELATED_NODES = {
            NODE_HEALTH, NODE_FOOD, NODE_REMINDERS, NODE_WEIGHT, NODE_EXPENSES, NODE_DIARY};

    public static final String[] GENDERS = {"Female", "Male"};
    public static final String[] HEALTH_TYPES = {"Vaccination", "Deworming", "Medicine", "Vet Visit", "Other"};
    public static final String[] FOOD_TYPES = {"Dry Food", "Wet Food", "Homemade Food", "Treats"};
    public static final String[] REMINDER_TYPES = {"Feeding", "Medicine", "Vaccination", "Deworming", "Vet Appointment"};
    public static final String[] EXPENSE_CATEGORIES = {"Food", "Medicine", "Vaccination", "Vet Visit", "Grooming", "Accessories", "Other"};

    /** Bangladeshi Taka sign. */
    public static final String CURRENCY = "\u09F3";

    // Intent extras
    public static final String EXTRA_CAT_ID = "cat_id";
    public static final String EXTRA_OPEN_ADD = "open_add";
}
