package com.purrcare.app.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ALL Firebase Realtime Database code lives here, so the screens stay short.
 *
 * Data layout:  users / {userId} / cats | healthRecords | foodSchedules | reminders | ...
 * Every record is saved under its own unique id, and only the logged-in user can read/write
 * their own "users/{userId}" branch (see firebase_rules/database.rules.json).
 */
public final class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    private FirebaseHelper() { }

    // ------------------------------------------------------------------ current user / paths

    public static FirebaseUser currentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    /** users/{uid} of the logged-in user. */
    public static DatabaseReference userRef() {
        FirebaseUser user = currentUser();
        if (user == null) throw new IllegalStateException("No logged-in user");
        return FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
    }

    /** users/{uid}/{nodeName} */
    public static DatabaseReference node(String nodeName) {
        return userRef().child(nodeName);
    }

    /** Creates a new unique id for a record (does not write anything yet). */
    public static String newId(String nodeName) {
        return node(nodeName).push().getKey();
    }

    // ------------------------------------------------------------------ REAL-TIME LISTENERS

    public interface ListListener<T> {
        void onData(List<T> items);
    }

    public interface ValueListener {
        void onValue(String value);
    }

    /** Remember it and call remove() in onStop() so the listener does not keep running. */
    public static class Subscription {
        private final DatabaseReference ref;
        private final ValueEventListener listener;

        Subscription(DatabaseReference ref, ValueEventListener listener) {
            this.ref = ref;
            this.listener = listener;
        }

        public void remove() {
            ref.removeEventListener(listener);
        }
    }

    /**
     * REAL-TIME LISTENER: onData() is called once now and again every time the data changes in Firebase,
     * so the list on screen updates by itself.
     */
    public static <T> Subscription listen(Context context, String nodeName, Class<T> type, ListListener<T> callback) {
        DatabaseReference ref = node(nodeName);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<T> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        T item = child.getValue(type);
                        if (item != null) items.add(item);
                    } catch (Exception e) {
                        Log.w(TAG, "Skipping unreadable record in " + nodeName, e);
                    }
                }
                callback.onData(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                UiUtils.toast(context, "Could not load data: " + error.getMessage());
            }
        };
        ref.addValueEventListener(listener);
        return new Subscription(ref, listener);
    }

    /** Real-time listener for one text value, e.g. "settings/clinicUrl". Gives "" when nothing is saved. */
    public static Subscription listenValue(Context context, String path, ValueListener callback) {
        DatabaseReference ref = userRef().child(path);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String value = "";
                try {
                    String v = snapshot.getValue(String.class);
                    if (v != null) value = v;
                } catch (Exception ignored) {
                    // not a text value - treat as empty
                }
                callback.onValue(value);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                UiUtils.toast(context, "Could not load data: " + error.getMessage());
            }
        };
        ref.addValueEventListener(listener);
        return new Subscription(ref, listener);
    }

    // ------------------------------------------------------------------ CREATE / UPDATE / DELETE

    /** Adds a new record or replaces the record that has this id (same call for Add and Edit). */
    public static void save(Context context, String nodeName, String id, Object value) {
        Context app = context.getApplicationContext();
        node(nodeName).child(id).setValue(value)
                .addOnSuccessListener(unused -> UiUtils.toast(app, "Saved successfully"))
                .addOnFailureListener(e -> UiUtils.toast(app, "Save failed: " + e.getMessage()));
    }

    /** Changes one field of a record, e.g. mark a feeding as done. */
    public static void updateField(Context context, String nodeName, String id, String field, Object value) {
        Context app = context.getApplicationContext();
        node(nodeName).child(id).child(field).setValue(value)
                .addOnFailureListener(e -> UiUtils.toast(app, "Update failed: " + e.getMessage()));
    }

    public static void delete(Context context, String nodeName, String id) {
        Context app = context.getApplicationContext();
        node(nodeName).child(id).removeValue()
                .addOnSuccessListener(unused -> UiUtils.toast(app, "Deleted"))
                .addOnFailureListener(e -> UiUtils.toast(app, "Delete failed: " + e.getMessage()));
    }

    /** Deletes a cat AND every health/food/weight/expense/reminder/diary record that belongs to it. */
    public static void deleteCatWithRecords(Context context, String catId) {
        for (String nodeName : Constants.CAT_RELATED_NODES) {
            node(nodeName).orderByChild("catId").equalTo(catId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                child.getRef().removeValue();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w(TAG, "Could not clean " + nodeName + ": " + error.getMessage());
                        }
                    });
        }
        delete(context, Constants.NODE_CATS, catId);
    }

    /** Saves one text value, e.g. path "settings/clinicUrl". */
    public static void saveValue(Context context, String path, String value) {
        Context app = context.getApplicationContext();
        userRef().child(path).setValue(value)
                .addOnSuccessListener(unused -> UiUtils.toast(app, "Saved successfully"))
                .addOnFailureListener(e -> UiUtils.toast(app, "Save failed: " + e.getMessage()));
    }
}
