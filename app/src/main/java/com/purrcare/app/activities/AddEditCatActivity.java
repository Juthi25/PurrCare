package com.purrcare.app.activities;

import android.os.Bundle;
import android.text.Editable;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.purrcare.app.R;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Cat;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.ImagePickerHelper;
import com.purrcare.app.utils.ImageUtils;
import com.purrcare.app.utils.SimpleTextWatcher;
import com.purrcare.app.utils.UiUtils;

/** Add / Edit screen for one cat (name, photo, gender, birthday, breed, weight, notes). */
public class AddEditCatActivity extends BaseActivity {

    private ImageView ivPhoto;
    private TextInputEditText etName, etDob, etBreed, etWeight, etNotes;
    private Spinner spGender;
    private TextView tvAge;

    private String catId;                 // null = adding a new cat
    private String photoBase64 = "";
    private long createdAt = System.currentTimeMillis();
    private ImagePickerHelper imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_add_edit_cat);

        ivPhoto = findViewById(R.id.ivPhoto);
        etName = findViewById(R.id.etName);
        etDob = findViewById(R.id.etDob);
        etBreed = findViewById(R.id.etBreed);
        etWeight = findViewById(R.id.etWeight);
        etNotes = findViewById(R.id.etNotes);
        spGender = findViewById(R.id.spGender);
        tvAge = findViewById(R.id.tvAge);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        UiUtils.setupSpinner(this, spGender, Constants.GENDERS);
        UiUtils.attachDatePicker(this, etDob, true);

        // Age is calculated automatically every time the birthday changes
        etDob.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String dob = s.toString().trim();
                tvAge.setText(dob.isEmpty() ? "Age: choose a date of birth" : "Age: " + DateTimeUtils.calculateAge(dob));
            }
        });

        // CAMERA / GALLERY: the picked photo is shown in the circle
        imagePicker = new ImagePickerHelper(this, 500, base64 -> {
            photoBase64 = base64;
            ImageUtils.setImage(ivPhoto, base64, R.drawable.ic_cat_face);
        });
        findViewById(R.id.btnChangePhoto).setOnClickListener(v -> imagePicker.showChooser());
        ivPhoto.setOnClickListener(v -> imagePicker.showChooser());

        findViewById(R.id.btnSave).setOnClickListener(v -> saveCat());

        catId = getIntent().getStringExtra(Constants.EXTRA_CAT_ID);
        if (catId != null) {
            ((TextView) findViewById(R.id.tvTitle)).setText("Edit Cat \u270F\uFE0F");
            loadCat();
        }
    }

    /** Reads this cat once from Firebase and fills the form. */
    private void loadCat() {
        FirebaseHelper.node(Constants.NODE_CATS).child(catId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Cat cat = snapshot.getValue(Cat.class);
                        if (cat == null) return;
                        etName.setText(cat.getName());
                        etBreed.setText(cat.getBreed());
                        etDob.setText(cat.getDateOfBirth());
                        etWeight.setText(String.valueOf(cat.getWeight()));
                        etNotes.setText(cat.getNotes());
                        spGender.setSelection(UiUtils.indexOf(Constants.GENDERS, cat.getGender()));
                        photoBase64 = UiUtils.nz(cat.getPhoto());
                        createdAt = cat.getCreatedAt();
                        ImageUtils.setImage(ivPhoto, photoBase64, R.drawable.ic_cat_face);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        UiUtils.toast(AddEditCatActivity.this, "Could not load cat: " + error.getMessage());
                    }
                });
    }

    private void saveCat() {
        // INPUT VALIDATION
        String name = UiUtils.requireText(etName, "Enter the cat's name");
        if (name == null) return;
        String dob = UiUtils.requireText(etDob, "Choose the date of birth");
        if (dob == null) return;
        String breed = UiUtils.requireText(etBreed, "Enter the breed (or write Mixed)");
        if (breed == null) return;
        Double weight = UiUtils.requirePositiveNumber(etWeight, "Enter the current weight in kg");
        if (weight == null) return;

        String id = catId != null ? catId : FirebaseHelper.newId(Constants.NODE_CATS);
        Cat cat = new Cat(id, name, photoBase64, (String) spGender.getSelectedItem(), dob, breed,
                weight, UiUtils.text(etNotes), createdAt);
        FirebaseHelper.save(this, Constants.NODE_CATS, id, cat);
        finish();
    }
}
