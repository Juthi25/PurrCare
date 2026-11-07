package com.purrcare.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.purrcare.app.R;
import com.purrcare.app.adapters.CatAdapter;
import com.purrcare.app.firebase.FirebaseHelper;
import com.purrcare.app.models.Cat;
import com.purrcare.app.utils.Constants;

/** "My Cats" tab: scrolling list of cat profile cards. FAB = add a cat, card buttons = edit / delete. */
public class CatsActivity extends BaseActivity {

    private CatAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("My Cats \uD83D\uDC31", R.id.nav_cats, false);

        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No cats yet.\nTap + to add your first cat \uD83D\uDC3E");

        adapter = new CatAdapter(false, this::openEditor, new CatAdapter.OnCatActionListener() {
            @Override
            public void onEdit(Cat cat) {
                openEditor(cat);
            }

            @Override
            public void onDelete(Cat cat) {
                confirmDelete("Delete " + cat.getName() + "? This also deletes all of this cat's health, food, "
                        + "reminder, weight, expense and diary records.", () ->
                        FirebaseHelper.deleteCatWithRecords(CatsActivity.this, cat.getId()));
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        ((FloatingActionButton) findViewById(R.id.fabAdd)).setOnClickListener(v -> openEditor(null));
    }

    private void openEditor(Cat cat) {
        Intent intent = new Intent(this, AddEditCatActivity.class);
        if (cat != null) intent.putExtra(Constants.EXTRA_CAT_ID, cat.getId());
        startActivity(intent);
    }

    @Override
    protected void onCatsChanged() {
        adapter.setCats(allCats);
        tvEmpty.setVisibility(allCats.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
