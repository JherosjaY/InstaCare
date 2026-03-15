package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private List<ContactItem> contacts;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onCallClick(String number);
    }

    public ContactsAdapter(List<ContactItem> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ContactViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_contact, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        holder.bind(contacts.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private TextView initial, name, relation;
        private ImageView callButton;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            initial = itemView.findViewById(R.id.contactInitial);
            name = itemView.findViewById(R.id.contactName);
            relation = itemView.findViewById(R.id.contactRelation);
            callButton = itemView.findViewById(R.id.callButton);
        }

        void bind(ContactItem item, OnContactClickListener listener) {
            name.setText(item.getName());
            relation.setText(item.getRelation());
            if (item.getName() != null && !item.getName().isEmpty()) {
                initial.setText(String.valueOf(item.getName().charAt(0)));
            }

            callButton.setOnClickListener(v -> {
                if (listener != null) listener.onCallClick(item.getNumber());
            });
        }
    }
}
