package com.example.instacare;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.Notification;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private List<Notification> notifications;
    private OnNotificationClickListener listener;
    private boolean isSelectionMode = false;
    private java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();
    private java.util.function.Consumer<Integer> selectionChangeListener;

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public void setSelectionChangeListener(java.util.function.Consumer<Integer> listener) {
        this.selectionChangeListener = listener;
    }

    public void setSelectionMode(boolean enabled) {
        this.isSelectionMode = enabled;
        if (!enabled) selectedPositions.clear();
        notifyDataSetChanged();
        if (selectionChangeListener != null) {
            selectionChangeListener.accept(selectedPositions.size());
        }
    }

    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        if (selectionChangeListener != null) {
            selectionChangeListener.accept(selectedPositions.size());
        }
    }

    public java.util.Set<Integer> getSelectedPositions() {
        return selectedPositions;
    }

    public void updateData(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Notification getNotificationAt(int position) {
        return notifications.get(position);
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotificationViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_notification, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification item = notifications.get(position);
        holder.bind(item, selectedPositions.contains(position), isSelectionMode);
        
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(position);
            } else {
                if (listener != null) listener.onNotificationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView title, message, time;
        private ImageView icon, ivSelect;
        private View unreadDot;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notificationTitle);
            message = itemView.findViewById(R.id.notificationMessage);
            time = itemView.findViewById(R.id.notificationTime);
            icon = itemView.findViewById(R.id.notificationIcon);
            ivSelect = itemView.findViewById(R.id.ivSelect);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }

        void bind(Notification item, boolean isSelected, boolean inSelectionMode) {
            // Highlighting for selection
            if (isSelected) {
                itemView.setBackgroundColor(android.graphics.Color.parseColor("#10E53935")); // Very light red overlay
            } else {
                itemView.setBackgroundResource(com.example.instacare.R.drawable.bg_nav_item_ripple);
            }

            // Selection Indicator visibility and state
            if (inSelectionMode) {
                ivSelect.setVisibility(View.VISIBLE);
                ivSelect.setImageResource(isSelected ? R.drawable.ic_check_circle : R.drawable.bg_circle_outline);
            } else {
                ivSelect.setVisibility(View.GONE);
            }

            String rawTitle = item.getTitle();
            if (rawTitle != null) {
                 // Support legacy "InstaCare!" coloring if not already colored in DB
                 if (rawTitle.contains("InstaCare!") && !rawTitle.contains("<font")) {
                     rawTitle = rawTitle.replace("InstaCare!", "<font color='#E53935'>InstaCare!</font>");
                 }
                 title.setText(android.text.Html.fromHtml(rawTitle, android.text.Html.FROM_HTML_MODE_LEGACY));
            }

            // --- Dynamic First-Name Personalization ---
            new Thread(() -> {
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(itemView.getContext().getApplicationContext())
                    .userDao().getUserById(item.getUserId());
                
                String firstName = "User";
                if (user != null && user.fullName != null) {
                    firstName = user.fullName.split(" ")[0];
                }
                
                final String finalFirstName = firstName;
                itemView.post(() -> {
                    String rawMsg = item.getMessage();
                    if (rawMsg != null) {
                        if ((rawMsg.startsWith("Hi ") || rawMsg.startsWith("Hello ")) && !rawMsg.contains("<b>")) {
                            int spaceIndex = rawMsg.indexOf(" ");
                            int exclamationIndex = rawMsg.indexOf("!");
                            if (spaceIndex != -1 && exclamationIndex != -1 && exclamationIndex > spaceIndex) {
                                String coloredName = "<font color='#E53935'><b>" + finalFirstName + "</b></font>";
                                String newMsg = rawMsg.substring(0, spaceIndex + 1) + coloredName + rawMsg.substring(exclamationIndex);
                                message.setText(android.text.Html.fromHtml(newMsg, android.text.Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                message.setText(android.text.Html.fromHtml(rawMsg, android.text.Html.FROM_HTML_MODE_LEGACY));
                            }
                        } else {
                            message.setText(android.text.Html.fromHtml(rawMsg, android.text.Html.FROM_HTML_MODE_LEGACY));
                        }
                    } else {
                        message.setText("");
                    }
                });
            }).start();
            
            long now = System.currentTimeMillis();
            long diff = now - item.getTimestamp();
            long twentyFourHours = 24 * 60 * 60 * 1000L;
            
            if (diff > twentyFourHours) {
                time.setVisibility(View.GONE);
            } else {
                time.setVisibility(View.VISIBLE);
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(item.getTimestamp());
                time.setText(timeAgo);
            }
            
            // --- Red Dot Indicator for Unread Items ---
            unreadDot.setVisibility(!item.isRead() ? View.VISIBLE : View.GONE);

            // --- Vibrating Alarm Bell for Unread Items ---
            if (!item.isRead()) {
                icon.setImageResource(R.drawable.avd_bell_ringing);
                android.graphics.drawable.Drawable drawable = icon.getDrawable();
                if (drawable instanceof android.graphics.drawable.Animatable) {
                    ((android.graphics.drawable.Animatable) drawable).start();
                }
                icon.setAlpha(1.0f);
            } else {
                icon.setImageResource(R.drawable.ic_bell);
                icon.setAlpha(1.0f); // Reverted alpha reduction (User Request)
            }
        }
    }
}
