package com.purrcare.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.purrcare.app.R;
import com.purrcare.app.models.Cat;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.ImageUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RECYCLERVIEW ADAPTER for cat profile cards.
 * Adapter = gives the RecyclerView the data; ViewHolder = remembers the views of one card.
 * "compact" mode is the small card used on the Home dashboard (no edit/delete buttons).
 */
public class CatAdapter extends RecyclerView.Adapter<CatAdapter.CatViewHolder> {

    public interface OnCatClickListener {
        void onCatClick(Cat cat);
    }

    public interface OnCatActionListener {
        void onEdit(Cat cat);
        void onDelete(Cat cat);
    }

    private final List<Cat> cats = new ArrayList<>();
    private final boolean compact;
    private final OnCatClickListener clickListener;
    private final OnCatActionListener actionListener; // null in compact mode

    public CatAdapter(boolean compact, OnCatClickListener clickListener, OnCatActionListener actionListener) {
        this.compact = compact;
        this.clickListener = clickListener;
        this.actionListener = actionListener;
    }

    public void setCats(List<Cat> newCats) {
        cats.clear();
        cats.addAll(newCats);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cat, parent, false);
        if (compact) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = UiUtils.dp(parent.getContext(), 270);
            view.setLayoutParams(lp);
        }
        return new CatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CatViewHolder h, int position) {
        Cat cat = cats.get(position);
        h.tvName.setText(cat.getName());
        h.tvDetails.setText(UiUtils.nz(cat.getGender()) + " \u2022 " + UiUtils.nz(cat.getBreed()));
        h.tvAge.setText("\uD83C\uDF82 " + DateTimeUtils.calculateAge(cat.getDateOfBirth()));
        h.tvWeight.setText(String.format(Locale.US, "\u2696\uFE0F %.1f kg", cat.getWeight()));
        boolean hasNotes = !UiUtils.nz(cat.getNotes()).isEmpty();
        h.tvNotes.setVisibility(hasNotes && !compact ? View.VISIBLE : View.GONE);
        h.tvNotes.setText("\uD83D\uDCDD " + UiUtils.nz(cat.getNotes()));
        ImageUtils.setImage(h.ivPhoto, cat.getPhoto(), R.drawable.ic_cat_face);

        h.layoutActions.setVisibility(compact ? View.GONE : View.VISIBLE);
        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCatClick(cat);
        });
        if (!compact && actionListener != null) {
            h.btnEdit.setOnClickListener(v -> actionListener.onEdit(cat));
            h.btnDelete.setOnClickListener(v -> actionListener.onDelete(cat));
        }
    }

    @Override
    public int getItemCount() {
        return cats.size();
    }

    static class CatViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        final TextView tvName, tvDetails, tvAge, tvWeight, tvNotes;
        final ImageButton btnEdit, btnDelete;
        final LinearLayout layoutActions;

        CatViewHolder(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivPhoto);
            tvName = v.findViewById(R.id.tvName);
            tvDetails = v.findViewById(R.id.tvDetails);
            tvAge = v.findViewById(R.id.tvAge);
            tvWeight = v.findViewById(R.id.tvWeight);
            tvNotes = v.findViewById(R.id.tvNotes);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
            layoutActions = v.findViewById(R.id.layoutActions);
        }
    }
}
