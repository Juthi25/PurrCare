package com.purrcare.app.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.purrcare.app.R;
import com.purrcare.app.adapters.RecordAdapter;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.EmergencyContact;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Emergency information with TWO separate sections:
 *  1) Pet clinic website (editable URL + "Visit" button -> ACTION_VIEW intent)
 *  2) Emergency contacts (add/edit/delete + "Call" button -> ACTION_DIAL intent)
 */
public class EmergencyActivity extends BaseActivity {

    private static final String CLINIC_PATH = Constants.NODE_SETTINGS + "/clinicUrl";

    private RecordAdapter<EmergencyContact> adapter;
    private TextView tvEmpty, tvClinicUrl;
    private String clinicUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("Emergency Info \uD83D\uDEA8", R.id.nav_more, true);

        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No emergency contacts yet.\nTap + to add one \uD83D\uDCDE");

        // Section 1: clinic website card goes in the "extra" area above the contact list
        FrameLayout extra = findViewById(R.id.extraContainer);
        View clinicSection = getLayoutInflater().inflate(R.layout.layout_clinic, extra, false);
        extra.addView(clinicSection);
        extra.setVisibility(View.VISIBLE);
        tvClinicUrl = clinicSection.findViewById(R.id.tvClinicUrl);
        clinicSection.findViewById(R.id.btnEditClinic).setOnClickListener(v -> showClinicDialog());
        clinicSection.findViewById(R.id.btnVisitClinic).setOnClickListener(v -> openClinicWebsite());

        // Section 2: contacts list
        adapter = new RecordAdapter<>(this::bindContact, new RecordAdapter.ActionListener<EmergencyContact>() {
            @Override
            public void onEdit(EmergencyContact contact) {
                showContactDialog(contact);
            }

            @Override
            public void onDelete(EmergencyContact contact) {
                confirmDelete("Delete " + contact.getName() + "?", () ->
                        FirebaseHelper.delete(EmergencyActivity.this, Constants.NODE_CONTACTS, contact.getId()));
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        findViewById(R.id.fabAdd).setOnClickListener(v -> showContactDialog(null));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        addSub(FirebaseHelper.listenValue(this, CLINIC_PATH, url -> {
            clinicUrl = url;
            tvClinicUrl.setText(url.isEmpty() ? "No website saved yet. Tap Edit to add one." : url);
        }));
        addSub(FirebaseHelper.listen(this, Constants.NODE_CONTACTS, EmergencyContact.class, list -> {
            List<EmergencyContact> sorted = new ArrayList<>(list);
            Collections.sort(sorted, (a, b) -> UiUtils.nz(a.getName()).compareToIgnoreCase(UiUtils.nz(b.getName())));
            adapter.setItems(sorted);
            tvEmpty.setVisibility(sorted.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    // ------------------------------------------------------------------ clinic website

    private String normalizeUrl(String input) {
        String url = input.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        return url;
    }

    private void showClinicDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_single_input, null);
        ((TextInputLayout) view.findViewById(R.id.tilValue)).setHint("Clinic website (e.g. www.myvet.com)");
        TextInputEditText etValue = view.findViewById(R.id.etValue);
        etValue.setText(clinicUrl);

        showFormDialog("Pet clinic website", view, () -> {
            String input = UiUtils.requireText(etValue, "Enter the website address");
            if (input == null) return false;
            String url = normalizeUrl(input);
            if (!Patterns.WEB_URL.matcher(url).matches()) {
                etValue.setError("Enter a valid website address");
                return false;
            }
            FirebaseHelper.saveValue(this, CLINIC_PATH, url);
            return true;
        });
    }

    /** IMPLICIT INTENT (ACTION_VIEW): opens the website in the browser. */
    private void openClinicWebsite() {
        if (clinicUrl.isEmpty()) {
            UiUtils.snack(findViewById(android.R.id.content), "Please save the clinic website first (tap Edit).");
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(clinicUrl))));
        } catch (ActivityNotFoundException e) {
            UiUtils.snack(findViewById(android.R.id.content), "No browser app found on this phone.");
        }
    }

    // ------------------------------------------------------------------ contacts

    private void bindContact(EmergencyContact c, RecordAdapter.RecordViewHolder h) {
        h.tvEmoji.setText("\uD83D\uDCDE");
        h.tvTitle.setText(c.getName());
        h.tvSubtitle.setText(c.getPhone());
        h.btnAction.setVisibility(View.VISIBLE);
        h.btnAction.setText("Call");
        h.btnAction.setOnClickListener(v -> dial(c.getPhone()));
    }

    /** IMPLICIT INTENT (ACTION_DIAL): opens the phone dialer with the number. The user must press the call button. */
    private void dial(String phone) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))));
        } catch (ActivityNotFoundException e) {
            UiUtils.snack(findViewById(android.R.id.content), "No dialer app found on this phone.");
        }
    }

    private void showContactDialog(EmergencyContact existing) {
        View view = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        TextInputEditText etName = view.findViewById(R.id.etName);
        TextInputEditText etPhone = view.findViewById(R.id.etPhone);
        if (existing != null) {
            etName.setText(existing.getName());
            etPhone.setText(existing.getPhone());
        }
        showFormDialog(existing == null ? "Add emergency contact" : "Edit contact", view, () -> {
            String name = UiUtils.requireText(etName, "Enter the contact name");
            if (name == null) return false;
            String phone = UiUtils.requireText(etPhone, "Enter the phone number");
            if (phone == null) return false;
            if (!phone.matches("^\\+?[0-9 \\-]{5,20}$")) {
                etPhone.setError("Enter a valid phone number (digits, + and - only)");
                return false;
            }
            String id = existing != null ? existing.getId() : FirebaseHelper.newId(Constants.NODE_CONTACTS);
            FirebaseHelper.save(this, Constants.NODE_CONTACTS, id, new EmergencyContact(id, name, phone));
            return true;
        });
    }
}
