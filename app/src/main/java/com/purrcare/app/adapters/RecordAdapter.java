package com.purrcare.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.purrcare.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * One reusable RECYCLERVIEW ADAPTER for all simple record lists
 * (health records, food, reminders, weights, expenses, emergency contacts).
 * <T> means "any model class". Each screen tells the adapter how to fill the card
 * by passing a Binder, so we do not need 6 almost-identical adapters.
 */
public class RecordAdapter<T> extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    /** Fills the views of one card from one item. */
    public interface Binder<T> {
        void bind(T item, RecordViewHolder holder);
    }

    /** Called when the edit / delete icon of a card is tapped. */
    public interface ActionListener<T> {
        void onEdit(T item);
        void onDelete(T item);
    }

    private final List<T> items = new ArrayList<>();
    private final Binder<T> binder;
    private final ActionListener<T> actionListener;

    public RecordAdapter(Binder<T> binder, ActionListener<T> actionListener) {
        this.binder = binder;
        this.actionListener = actionListener;
    }

    public void setItems(List<T> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder h, int position) {
        T item = items.get(position);
        // reset optional views because RecyclerView re-uses cards
        h.tvTag.setVisibility(View.GONE);
        h.tvDetail.setVisibility(View.GONE);
        h.btnAction.setVisibility(View.GONE);
        h.btnAction.setOnClickListener(null);

        binder.bind(item, h);

        h.btnEdit.setOnClickListener(v -> actionListener.onEdit(item));
        h.btnDelete.setOnClickListener(v -> actionListener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** VIEWHOLDER: keeps references to the views of one card. */
    public static class RecordViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvEmoji, tvTitle, tvSubtitle, tvDetail, tvTag;
        public final MaterialButton btnAction;
        public final ImageButton btnEdit, btnDelete;

        RecordViewHolder(@NonNull View v) {
            super(v);
            tvEmoji = v.findViewById(R.id.tvEmoji);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
            tvDetail = v.findViewById(R.id.tvDetail);
            tvTag = v.findViewById(R.id.tvTag);
            btnAction = v.findViewById(R.id.btnAction);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }

        /** Helper for binders: show a text in an optional view (hidden when empty). */
        public void showOptional(TextView view, String text) {
            if (text == null || text.trim().isEmpty()) {
                view.setVisibility(View.GONE);
            } else {
                view.setText(text);
                view.setVisibility(View.VISIBLE);
            }
        }
    }
}
