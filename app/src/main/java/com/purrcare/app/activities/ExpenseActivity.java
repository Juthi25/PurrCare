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
import com.purrcare.app.models.Expense;
import com.purrcare.app.utils.Constants;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Expense tracker (Bangladeshi Taka): history, total for this month and category-wise summary. */
public class ExpenseActivity extends BaseActivity {

    private final List<Expense> allExpenses = new ArrayList<>();
    private RecordAdapter<Expense> adapter;
    private TextView tvSummary, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!ensureLoggedIn()) return;
        setContentView(R.layout.activity_list);
        initListScreen("Expense Tracker \uD83D\uDCB0", R.id.nav_more, true);
        initCatFilter(true);

        tvSummary = findViewById(R.id.tvSummary);
        findViewById(R.id.cardSummary).setVisibility(View.VISIBLE);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvEmpty.setText("No expenses yet.\nTap + to add one \uD83D\uDCB0");

        adapter = new RecordAdapter<>(this::bindExpense, new RecordAdapter.ActionListener<Expense>() {
            @Override
            public void onEdit(Expense expense) {
                showDialog(expense);
            }

            @Override
            public void onDelete(Expense expense) {
                confirmDelete("Delete this expense?", () ->
                        FirebaseHelper.delete(ExpenseActivity.this, Constants.NODE_EXPENSES, expense.getId()));
            }
        });
        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        findViewById(R.id.fabAdd).setOnClickListener(v -> showDialog(null));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!loggedIn) return;
        addSub(FirebaseHelper.listen(this, Constants.NODE_EXPENSES, Expense.class, list -> {
            allExpenses.clear();
            allExpenses.addAll(list);
            render();
        }));
    }

    @Override
    protected void onFilterChanged() {
        render();
    }

    private void render() {
        List<Expense> shown = new ArrayList<>();
        for (Expense e : allExpenses) {
            if (filterCatId == null || filterCatId.equals(e.getCatId())) shown.add(e);
        }
        Collections.sort(shown, (a, b) -> UiUtils.nz(b.getDate()).compareTo(UiUtils.nz(a.getDate())));
        adapter.setItems(shown);
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);

        // Monthly total + category-wise summary
        String month = DateTimeUtils.currentMonthPrefix();
        double monthTotal = 0, allTotal = 0;
        Map<String, Double> byCategory = new LinkedHashMap<>();
        for (String category : Constants.EXPENSE_CATEGORIES) byCategory.put(category, 0.0);
        for (Expense e : shown) {
            allTotal += e.getAmount();
            if (UiUtils.nz(e.getDate()).startsWith(month)) {
                monthTotal += e.getAmount();
                Double current = byCategory.get(e.getCategory());
                byCategory.put(UiUtils.nz(e.getCategory()), (current == null ? 0 : current) + e.getAmount());
            }
        }
        StringBuilder sb = new StringBuilder("\uD83D\uDCB0 This month (" + DateTimeUtils.monthLabel() + "): " + UiUtils.money(monthTotal));
        for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
            if (entry.getValue() > 0) sb.append("\n\u2022 ").append(entry.getKey()).append(": ").append(UiUtils.money(entry.getValue()));
        }
        sb.append("\nAll-time total: ").append(UiUtils.money(allTotal));
        tvSummary.setText(sb.toString());
    }

    private String emojiFor(String category) {
        if ("Food".equals(category)) return "\uD83C\uDF57";
        if ("Medicine".equals(category)) return "\uD83D\uDC8A";
        if ("Vaccination".equals(category)) return "\uD83D\uDC89";
        if ("Vet Visit".equals(category)) return "\uD83C\uDFE5";
        if ("Grooming".equals(category)) return "\u2702\uFE0F";
        if ("Accessories".equals(category)) return "\uD83E\uDDF6";
        return "\uD83E\uDDFE";
    }

    private void bindExpense(Expense e, RecordAdapter.RecordViewHolder h) {
        h.tvEmoji.setText(emojiFor(e.getCategory()));
        h.tvTitle.setText(UiUtils.money(e.getAmount()));
        h.tvSubtitle.setText(catName(e.getCatId(), e.getCatName()) + " \u2022 " + e.getCategory() + " \u2022 " + DateTimeUtils.pretty(e.getDate()));
        h.showOptional(h.tvDetail, e.getDescription());
    }

    private void showDialog(Expense existing) {
        if (!requireCats()) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_expense, null);
        Spinner spCat = view.findViewById(R.id.spCat);
        Spinner spCategory = view.findViewById(R.id.spCategory);
        TextInputEditText etAmount = view.findViewById(R.id.etAmount);
        TextInputEditText etDate = view.findViewById(R.id.etDate);
        TextInputEditText etDescription = view.findViewById(R.id.etDescription);

        setupCatSpinner(spCat, existing != null ? existing.getCatId() : filterCatId);
        UiUtils.setupSpinner(this, spCategory, Constants.EXPENSE_CATEGORIES);
        UiUtils.attachDatePicker(this, etDate, true);

        if (existing != null) {
            spCategory.setSelection(UiUtils.indexOf(Constants.EXPENSE_CATEGORIES, existing.getCategory()));
            etAmount.setText(String.valueOf(existing.getAmount()));
            etDate.setText(existing.getDate());
            etDescription.setText(existing.getDescription());
        } else {
            etDate.setText(DateTimeUtils.todayString());
        }

        showFormDialog(existing == null ? "Add expense" : "Edit expense", view, () -> {
            Double amount = UiUtils.requirePositiveNumber(etAmount, "Enter the amount in " + Constants.CURRENCY);
            if (amount == null) return false;
            String date = UiUtils.requireText(etDate, "Choose the date");
            if (date == null) return false;
            Cat cat = selectedCat(spCat);
            if (cat == null) return false;

            String id = existing != null ? existing.getId() : FirebaseHelper.newId(Constants.NODE_EXPENSES);
            Expense expense = new Expense(id, cat.getId(), cat.getName(), (String) spCategory.getSelectedItem(),
                    amount, date, UiUtils.text(etDescription));
            FirebaseHelper.save(this, Constants.NODE_EXPENSES, id, expense);
            return true;
        });
    }
}
