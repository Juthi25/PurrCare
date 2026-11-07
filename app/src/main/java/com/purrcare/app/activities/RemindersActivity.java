package com.purrcare.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.purrcare.app.R;
import com.purrcare.app.adapters.RecordAdapter;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Cat;
import com.purrcare.app.models.Reminder;
import com.purrcare.app.notifications.ReminderScheduler;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reminders: add/edit/delete, mark as completed, and schedule an Android notification for the due time. */
public class RemindersActivity extends BaseActivity {

    private RecordAdapter<Reminder> adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("Reminders \u23F0", R.id.nav_reminders, false);

        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No reminders yet.\nTap + to add one \u23F0");

        adapter = new RecordAdapter<>(this::bindReminder, new RecordAdapter.ActionListener<Reminder>() {
            @Override
            public void onEdit(Reminder reminder) {
                showDialog(reminder);
            }

            @Override
            public void onDelete(Reminder reminder) {
                confirmDelete("Delete this reminder?", () -> {
                    ReminderScheduler.cancel(RemindersActivity.this, reminder);   // also cancel its notification
                    FirebaseHelper.delete(RemindersActivity.this, Constants.NODE_REMINDERS, reminder.getId());
                });
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            requestNotificationPermission();
            showDialog(null);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        addSub(FirebaseHelper.listen(this, Constants.NODE_REMINDERS, Reminder.class, list -> {
            ReminderScheduler.rescheduleAll(this, list);
            // pending reminders first (soonest on top), completed ones at the bottom
            List<Reminder> sorted = new ArrayList<>(list);
            Collections.sort(sorted, (a, b) -> {
                if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                return a.isCompleted() ? Long.compare(b.getDateTimeMillis(), a.getDateTimeMillis())
                        : Long.compare(a.getDateTimeMillis(), b.getDateTimeMillis());
            });
            adapter.setItems(sorted);
            tvEmpty.setVisibility(sorted.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    private String emojiFor(String type) {
        if ("Feeding".equals(type)) return "\uD83C\uDF7D\uFE0F";
        if ("Medicine".equals(type)) return "\uD83D\uDC8A";
        if ("Vaccination".equals(type)) return "\uD83D\uDC89";
        if ("Deworming".equals(type)) return "\uD83D\uDC1B";
        return "\uD83C\uDFE5";
    }

    private void bindReminder(Reminder r, RecordAdapter.RecordViewHolder h) {
        h.tvEmoji.setText(emojiFor(r.getType()));
        h.tvTitle.setText(r.getTitle());
        h.tvSubtitle.setText(catName(r.getCatId(), r.getCatName()) + " \u2022 " + r.getType());
        String detail = "\uD83D\uDD52 " + DateTimeUtils.formatDateTime(r.getDateTimeMillis());
        if (!UiUtils.nz(r.getNotes()).isEmpty()) detail += "\n" + r.getNotes();
        h.showOptional(h.tvDetail, detail);

        String tag;
        if (r.isCompleted()) tag = "Completed \u2705";
        else if (r.getDateTimeMillis() < System.currentTimeMillis()) tag = "Overdue \u26A0\uFE0F";
        else tag = "Upcoming";
        h.showOptional(h.tvTag, tag);

        h.btnAction.setVisibility(View.VISIBLE);
        h.btnAction.setText(r.isCompleted() ? "Undo" : "Mark done");
        h.btnAction.setOnClickListener(v -> {
            boolean nowCompleted = !r.isCompleted();
            FirebaseHelper.updateField(this, Constants.NODE_REMINDERS, r.getId(), "completed", nowCompleted);
            r.setCompleted(nowCompleted);
            if (nowCompleted) ReminderScheduler.cancel(this, r);
            else ReminderScheduler.schedule(this, r);
        });
    }

    private void showDialog(Reminder existing) {
        if (!requireCats()) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_reminder, null);
        Spinner spCat = view.findViewById(R.id.spCat);
        Spinner spType = view.findViewById(R.id.spType);
        TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        TextInputEditText etDate = view.findViewById(R.id.etDate);
        TextInputEditText etTime = view.findViewById(R.id.etTime);
        TextInputEditText etNotes = view.findViewById(R.id.etNotes);

        setupCatSpinner(spCat, existing != null ? existing.getCatId() : null);
        UiUtils.setupSpinner(this, spType, Constants.REMINDER_TYPES);
        UiUtils.attachDatePicker(this, etDate, false);
        UiUtils.attachTimePicker(this, etTime);

        if (existing != null) {
            etTitle.setText(existing.getTitle());
            spType.setSelection(UiUtils.indexOf(Constants.REMINDER_TYPES, existing.getType()));
            etDate.setText(DateTimeUtils.dateFromMillis(existing.getDateTimeMillis()));
            etTime.setText(DateTimeUtils.timeFromMillis(existing.getDateTimeMillis()));
            etNotes.setText(existing.getNotes());
        }

        showFormDialog(existing == null ? "Add reminder" : "Edit reminder", view, () -> {
            String title = UiUtils.requireText(etTitle, "Enter a title");
            if (title == null) return false;
            String date = UiUtils.requireText(etDate, "Choose the date");
            if (date == null) return false;
            String time = UiUtils.requireText(etTime, "Choose the time");
            if (time == null) return false;
            long millis = DateTimeUtils.toMillis(date, time);
            if (millis < 0) {
                etTime.setError("Invalid date or time");
                return false;
            }
            boolean timeChanged = existing == null || existing.getDateTimeMillis() != millis;
            if (timeChanged && millis <= System.currentTimeMillis()) {
                etTime.setError("Choose a time in the future");
                return false;
            }
            Cat cat = selectedCat(spCat);
            if (cat == null) return false;

            String id = existing != null ? existing.getId() : FirebaseHelper.newId(Constants.NODE_REMINDERS);
            boolean completed = existing != null && existing.isCompleted() && !timeChanged;
            Reminder reminder = new Reminder(id, cat.getId(), cat.getName(), title,
                    (String) spType.getSelectedItem(), millis, UiUtils.text(etNotes), completed);
            FirebaseHelper.save(this, Constants.NODE_REMINDERS, id, reminder);

            // Replace the old alarm with the new one
            ReminderScheduler.cancel(this, reminder);
            ReminderScheduler.schedule(this, reminder);
            return true;
        });
    }
}
