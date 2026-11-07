package com.purrcare.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.purrcare.app.R;
import com.purrcare.app.models.DiaryEntry;
import com.purrcare.app.utils.DateTimeUtils;
import com.purrcare.app.utils.ImageUtils;
import com.purrcare.app.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

/** RECYCLERVIEW ADAPTER for the photo grid of the Growth Diary (used with a GridLayoutManager). */
public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.DiaryViewHolder> {

    public interface DiaryActionListener {
        void onEdit(DiaryEntry entry);
        void onDelete(DiaryEntry entry);
    }

    private final List<DiaryEntry> entries = new ArrayList<>();
    private final DiaryActionListener listener;

    public DiaryAdapter(DiaryActionListener listener) {
        this.listener = listener;
    }

    public void setEntries(List<DiaryEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_diary, parent, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryViewHolder h, int position) {
        DiaryEntry e = entries.get(position);
        ImageUtils.setImage(h.ivPhoto, e.getPhoto(), R.drawable.ic_cat_face);
        h.tvTitle.setText(e.getTitle());
        h.tvMeta.setText(UiUtils.nz(e.getCatName()) + " \u2022 " + DateTimeUtils.pretty(e.getDate()));
        h.tvDescription.setText(UiUtils.nz(e.getDescription()));
        h.btnEdit.setOnClickListener(v -> listener.onEdit(e));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(e));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class DiaryViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        final TextView tvTitle, tvMeta, tvDescription;
        final ImageButton btnEdit, btnDelete;

        DiaryViewHolder(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivPhoto);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvMeta = v.findViewById(R.id.tvMeta);
            tvDescription = v.findViewById(R.id.tvDescription);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
