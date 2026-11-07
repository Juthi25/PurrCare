package com.purrcare.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.purrcare.app.R;
import com.purrcare.app.adapters.DiaryAdapter;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Cat;
import com.purrcare.app.models.DiaryEntry;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.ImagePickerHelper;
import com.purrcare.app.utils.ImageUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Growth diary: photo memories in a 2-column grid. Photos come from the camera or gallery. */
public class DiaryActivity extends BaseActivity {

    private final List<DiaryEntry> allEntries = new ArrayList<>();
    private DiaryAdapter adapter;
    private TextView tvEmpty;
    private ImagePickerHelper imagePicker;
    private String pendingPhoto = "";        // photo chosen in the open dialog
    private ImageView dialogImage;           // preview inside the open dialog

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("Growth Diary \uD83D\uDCF8", R.id.nav_more, true);
        initCatFilter(true);

        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No memories yet.\nTap + to add a photo \uD83D\uDCF8");

        // Must be created in onCreate (before the screen starts)
        imagePicker = new ImagePickerHelper(this, 700, base64 -> {
            pendingPhoto = base64;
            if (dialogImage != null) ImageUtils.setImage(dialogImage, base64, R.drawable.ic_cat_face);
        });

        adapter = new DiaryAdapter(new DiaryAdapter.DiaryActionListener() {
            @Override
            public void onEdit(DiaryEntry entry) {
                showDialog(entry);
            }

            @Override
            public void onDelete(DiaryEntry entry) {
                confirmDelete("Delete this memory?", () ->
                        FirebaseHelper.delete(DiaryActivity.this, Constants.NODE_DIARY, entry.getId()));
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new GridLayoutManager(this, 2));   // photo grid
        rv.setAdapter(adapter);
        findViewById(R.id.fabAdd).setOnClickListener(v -> showDialog(null));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        addSub(FirebaseHelper.listen(this, Constants.NODE_DIARY, DiaryEntry.class, list -> {
            allEntries.clear();
            allEntries.addAll(list);
            render();
        }));
    }

    @Override
    protected void onFilterChanged() {
        render();
    }

    private void render() {
        List<DiaryEntry> shown = new ArrayList<>();
        for (DiaryEntry e : allEntries) {
            if (filterCatId == null || filterCatId.equals(e.getCatId())) shown.add(e);
        }
        Collections.sort(shown, (a, b) -> UiUtils.nz(b.getDate()).compareTo(UiUtils.nz(a.getDate())));
        adapter.setEntries(shown);
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showDialog(DiaryEntry existing) {
        if (!requireCats()) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_diary, null);
        dialogImage = view.findViewById(R.id.ivDiaryPhoto);
        Spinner spCat = view.findViewById(R.id.spCat);
        TextInputEditText etTitle = view.findViewById(R.id.etTitle);
        TextInputEditText etDescription = view.findViewById(R.id.etDescription);
        TextInputEditText etDate = view.findViewById(R.id.etDate);

        setupCatSpinner(spCat, existing != null ? existing.getCatId() : filterCatId);
        UiUtils.attachDatePicker(this, etDate, true);
        view.findViewById(R.id.btnPickPhoto).setOnClickListener(v -> imagePicker.showChooser());
        dialogImage.setOnClickListener(v -> imagePicker.showChooser());

        pendingPhoto = existing != null ? UiUtils.nz(existing.getPhoto()) : "";
        ImageUtils.setImage(dialogImage, pendingPhoto, R.drawable.ic_cat_face);
        if (existing != null) {
            etTitle.setText(existing.getTitle());
            etDescription.setText(existing.getDescription());
            etDate.setText(existing.getDate());
        } else {
            etDate.setText(DateTimeUtils.todayString());
        }

        showFormDialog(existing == null ? "New memory" : "Edit memory", view, () -> {
            String title = UiUtils.requireText(etTitle, "Enter a title");
            if (title == null) return false;
            String date = UiUtils.requireText(etDate, "Choose the date");
            if (date == null) return false;
            if (pendingPhoto.isEmpty()) {
                UiUtils.toast(this, "Please add a photo first (tap the picture)");
                return false;
            }
            Cat cat = selectedCat(spCat);
            if (cat == null) return false;

            String id = existing != null ? existing.getId() : FirebaseHelper.newId(Constants.NODE_DIARY);
            DiaryEntry entry = new DiaryEntry(id, cat.getId(), cat.getName(), title,
                    UiUtils.text(etDescription), date, pendingPhoto);
            FirebaseHelper.save(this, Constants.NODE_DIARY, id, entry);
            return true;
        });
    }
}
