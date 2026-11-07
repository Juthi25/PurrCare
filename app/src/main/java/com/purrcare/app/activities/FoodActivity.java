package com.purrcare.app.activities;

import android.content.Intent;
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
import com.purrcare.app.models.FoodSchedule;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Food tracker: daily feeding schedule per cat, mark feedings as done. Also opened by the shake quick action. */
public class FoodActivity extends BaseActivity {

    private final List<FoodSchedule> allFood = new ArrayList<>();
    private RecordAdapter<FoodSchedule> adapter;
    private TextView tvSummary, tvEmpty;
    private boolean pendingOpenAdd = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("Food Tracker \uD83C\uDF7D\uFE0F", R.id.nav_more, true);
        initCatFilter(true);

        tvSummary = findViewById(R.id.tvSummary);
        findViewById(R.id.cardSummary).setVisibility(View.VISIBLE);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No feeding schedule yet.\nTap + to add one \uD83C\uDF7D\uFE0F");

        adapter = new RecordAdapter<>(this::bindFood, new RecordAdapter.ActionListener<FoodSchedule>() {
            @Override
            public void onEdit(FoodSchedule food) {
                showDialog(food);
            }

            @Override
            public void onDelete(FoodSchedule food) {
                confirmDelete("Delete this feeding schedule?", () ->
                        FirebaseHelper.delete(FoodActivity.this, Constants.NODE_FOOD, food.getId()));
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        findViewById(R.id.fabAdd).setOnClickListener(v -> showDialog(null));

        // The shake dialog can ask us to open the "add" form directly
        pendingOpenAdd = getIntent().getBooleanExtra(Constants.EXTRA_OPEN_ADD, false);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra(Constants.EXTRA_OPEN_ADD, false)) showDialog(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        addSub(FirebaseHelper.listen(this, Constants.NODE_FOOD, FoodSchedule.class, list -> {
            allFood.clear();
            allFood.addAll(list);
            render();
        }));
    }

    @Override
    protected void onCatsChanged() {
        if (pendingOpenAdd) {          // cats are loaded now, so the form can show the cat list
            pendingOpenAdd = false;
            showDialog(null);
        }
    }

    @Override
    protected void onFilterChanged() {
        render();
    }

    private boolean isDoneToday(FoodSchedule f) {
        return DateTimeUtils.todayString().equals(f.getLastCompletedDate());
    }

    private void render() {
        List<FoodSchedule> shown = new ArrayList<>();
        for (FoodSchedule f : allFood) {
            if (filterCatId == null || filterCatId.equals(f.getCatId())) shown.add(f);
        }
        Collections.sort(shown, (a, b) -> UiUtils.nz(a.getFeedingTime()).compareTo(UiUtils.nz(b.getFeedingTime())));
        adapter.setItems(shown);
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);

        int done = 0;
        for (FoodSchedule f : shown) if (isDoneToday(f)) done++;
        tvSummary.setText("\uD83D\uDCC5 Today's feedings: " + done + " of " + shown.size() + " completed");
    }

    private String emojiFor(String type) {
        if ("Dry Food".equals(type)) return "\uD83E\uDD63";
        if ("Wet Food".equals(type)) return "\uD83E\uDD6B";
        if ("Homemade Food".equals(type)) return "\uD83C\uDF72";
        return "\uD83C\uDF6A";
    }

    private void bindFood(FoodSchedule f, RecordAdapter.RecordViewHolder h) {
        boolean done = isDoneToday(f);
        h.tvEmoji.setText(emojiFor(f.getFoodType()));
        h.tvTitle.setText(f.getFoodName() + " \u2022 " + UiUtils.nz(f.getQuantity()));
        h.tvSubtitle.setText(catName(f.getCatId(), f.getCatName()) + " \u2022 " + f.getFoodType() + " \u2022 "
                + DateTimeUtils.formatTime12(f.getFeedingTime()));
        h.showOptional(h.tvDetail, f.getNotes());
        h.showOptional(h.tvTag, done ? "Done today \u2705" : "Pending");

        h.btnAction.setVisibility(View.VISIBLE);
        h.btnAction.setText(done ? "Undo" : "Mark done");
        h.btnAction.setOnClickListener(v -> FirebaseHelper.updateField(this, Constants.NODE_FOOD, f.getId(),
                "lastCompletedDate", done ? "" : DateTimeUtils.todayString()));
    }

    private void showDialog(FoodSchedule existing) {
        if (!requireCats()) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_food, null);
        Spinner spCat = view.findViewById(R.id.spCat);
        Spinner spFoodType = view.findViewById(R.id.spFoodType);
        TextInputEditText etFoodName = view.findViewById(R.id.etFoodName);
        TextInputEditText etTime = view.findViewById(R.id.etTime);
        TextInputEditText etQuantity = view.findViewById(R.id.etQuantity);
        TextInputEditText etNotes = view.findViewById(R.id.etNotes);

        setupCatSpinner(spCat, existing != null ? existing.getCatId() : filterCatId);
        UiUtils.setupSpinner(this, spFoodType, Constants.FOOD_TYPES);
        UiUtils.attachTimePicker(this, etTime);

        if (existing != null) {
            etFoodName.setText(existing.getFoodName());
            spFoodType.setSelection(UiUtils.indexOf(Constants.FOOD_TYPES, existing.getFoodType()));
            etTime.setText(existing.getFeedingTime());
            etQuantity.setText(existing.getQuantity());
            etNotes.setText(existing.getNotes());
        }

        showFormDialog(existing == null ? "Add feeding" : "Edit feeding", view, () -> {
            String foodName = UiUtils.requireText(etFoodName, "Enter the food name");
            if (foodName == null) return false;
            String time = UiUtils.requireText(etTime, "Choose the feeding time");
            if (time == null) return false;
            String quantity = UiUtils.requireText(etQuantity, "Enter the quantity (e.g. 50 g)");
            if (quantity == null) return false;
            Cat cat = selectedCat(spCat);
            if (cat == null) return false;

            String id = existing != null ? existing.getId() : FirebaseHelper.newId(Constants.NODE_FOOD);
            String lastDone = existing != null ? UiUtils.nz(existing.getLastCompletedDate()) : "";
            FoodSchedule food = new FoodSchedule(id, cat.getId(), cat.getName(), foodName,
                    (String) spFoodType.getSelectedItem(), time, quantity, UiUtils.text(etNotes), lastDone);
            FirebaseHelper.save(this, Constants.NODE_FOOD, id, food);
            return true;
        });
    }
}
