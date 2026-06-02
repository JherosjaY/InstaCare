package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.ChatMessage;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_MESSAGE = 0;
    public static final int TYPE_TYPING = 4;

    private List<ChatMessage> messages;
    private int currentUserId;
    private int userId;
    private String currentUserRole;
    private OnMessageLongClickListener longClickListener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatMessage message);
    }

    public ChatAdapter(List<ChatMessage> messages, int currentUserId, String currentUserRole, OnMessageLongClickListener longClickListener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.userId = -1; // Default
        this.currentUserRole = currentUserRole;
        this.longClickListener = longClickListener;
    }

    public void setUserId(int userId) {
        this.userId = userId;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (messages.get(position).isTyping) return TYPE_TYPING;
        return TYPE_MESSAGE;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        this.messages = newMessages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_TYPING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_typing_indicator, parent, false);
            return new TypingViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).startAnimation();
            return;
        }
        
        ChatViewHolder chatHolder = (ChatViewHolder) holder;
        ChatMessage msg = messages.get(position);
        
        // A message is "mine" if:
        // 1. I specifically sent it (UID match)
        // 2. OR I am a Barangay Staff and the message was sent as a Barangay Role (e.g. auto-welcome)
        boolean isMe = msg.senderId == currentUserId || 
                      ("BARANGAY".equals(currentUserRole) && "BARANGAY".equals(msg.senderRole));

        // 1. Resolve role and orientation
        String role = msg.senderRole != null ? msg.senderRole : "USER";
        int roleColor = "BARANGAY".equals(role) ? 
                holder.itemView.getContext().getResources().getColor(R.color.barangay_primary) : 
                holder.itemView.getContext().getResources().getColor(R.color.primary);

        // 2. Total State Reset for Recycled Views (Incoming & Outgoing)
        float density = chatHolder.itemView.getContext().getResources().getDisplayMetrics().density;
        int padding4dp = (int) (4 * density);

        // Reset BOTH views to original XML parameters
        for (ImageView iv : new ImageView[]{chatHolder.ivAvatarIncoming, chatHolder.ivAvatarOutgoing}) {
            Glide.with(chatHolder.itemView.getContext()).clear(iv);
            iv.setPadding(padding4dp, padding4dp, padding4dp, padding4dp);
            iv.setBackgroundResource(R.drawable.bg_icon_circle);
            iv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(roleColor));
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        }

        ImageView targetAvatar = isMe ? chatHolder.ivAvatarOutgoing : chatHolder.ivAvatarIncoming;

        if ("ADMIN".equals(role) || "BARANGAY".equals(role)) {
            targetAvatar.setImageResource(R.drawable.ic_shield);
        } else {
            // USER Role: Try to load profile picture
            if (userId != -1) {
                java.io.File file = new java.io.File(chatHolder.itemView.getContext().getFilesDir(), "profile_avatar_" + userId + ".png");
                if (file.exists()) {
                    // PHOTO MODE: Clear tint and background so photo is clear
                    targetAvatar.setImageTintList(null); 
                    targetAvatar.setBackgroundTintList(null);
                    targetAvatar.setPadding(0, 0, 0, 0); // Photo fills the circle
                    
                    Glide.with(chatHolder.itemView.getContext())
                        .load(file.getAbsolutePath())
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(targetAvatar);
                } else {
                    targetAvatar.setImageResource(R.drawable.ic_user);
                }
            } else {
                targetAvatar.setImageResource(R.drawable.ic_user);
            }
        }

        if (isMe) {
            chatHolder.layoutOutgoing.setVisibility(View.VISIBLE);
            chatHolder.layoutIncoming.setVisibility(View.GONE);
            chatHolder.tvOutgoingTime.setText(timeFormat.format(new Date(msg.timestamp)));
            chatHolder.btnReplyOutgoing.setColorFilter(chatHolder.itemView.getContext().getResources().getColor(R.color.text_tertiary), android.graphics.PorterDuff.Mode.SRC_IN);
            chatHolder.btnReplyOutgoing.setVisibility(View.VISIBLE);

            int outgoingColor = "BARANGAY".equals(currentUserRole) ? 
                chatHolder.itemView.getContext().getResources().getColor(R.color.barangay_primary) : 
                chatHolder.itemView.getContext().getResources().getColor(R.color.primary);
            
            if (msg.imagePath != null) {
                chatHolder.ivOutgoingImage.setVisibility(View.VISIBLE);
                chatHolder.tvOutgoingMsg.setVisibility(msg.message != null && !msg.message.isEmpty() ? View.VISIBLE : View.GONE);
                chatHolder.tvOutgoingMsg.setText(msg.message);
                
                // Hide bubble style for pure images
                chatHolder.bubbleOutgoing.setBackground(null);
                
                Glide.with(chatHolder.itemView.getContext())
                    .load(msg.imagePath)
                    .centerCrop()
                    .into(chatHolder.ivOutgoingImage);
                
                chatHolder.ivOutgoingImage.setOnClickListener(v -> showFullScreenImage(msg.imagePath, chatHolder.itemView.getContext()));
            } else {
                chatHolder.ivOutgoingImage.setVisibility(View.GONE);
                chatHolder.tvOutgoingMsg.setVisibility(View.VISIBLE);
                chatHolder.tvOutgoingMsg.setText(msg.message);
                
                // Restore bubble style
                chatHolder.bubbleOutgoing.setBackgroundResource(R.drawable.bg_chat_bubble_outgoing);
                chatHolder.bubbleOutgoing.getBackground().setTint(outgoingColor);
            }

            if (msg.replyToId != null) {
                chatHolder.layoutReplyOutgoing.setVisibility(View.VISIBLE);
                
                String replyPrefix = " | ";
                String replyContent = msg.replyToMessage != null ? msg.replyToMessage : "";
                String fullReplyText = replyPrefix + replyContent;
                SpannableString spannable = new SpannableString(fullReplyText);
                spannable.setSpan(new ForegroundColorSpan(outgoingColor), 0, replyPrefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                chatHolder.tvReplyMsgOutgoing.setText(spannable);

                // Load Reply Image (Outgoing)
                if (msg.replyToImagePath != null && !msg.replyToImagePath.isEmpty()) {
                    chatHolder.ivReplyImageOutgoing.setVisibility(View.VISIBLE);
                    Glide.with(chatHolder.itemView.getContext())
                        .load(msg.replyToImagePath)
                        .centerCrop()
                        .into(chatHolder.ivReplyImageOutgoing);
                } else {
                    chatHolder.ivReplyImageOutgoing.setVisibility(View.GONE);
                }
            } else {
                chatHolder.layoutReplyOutgoing.setVisibility(View.GONE);
            }

            // Reply button click
            chatHolder.btnReplyOutgoing.setOnClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(msg);
                }
            });

            // Long Press to Reply
            chatHolder.bubbleOutgoing.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(msg);
                }
                return true;
            });
            chatHolder.ivOutgoingImage.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(msg);
                }
                return true;
            });
        } else {
            chatHolder.layoutIncoming.setVisibility(View.VISIBLE);
            chatHolder.layoutOutgoing.setVisibility(View.GONE);
            chatHolder.tvIncomingTime.setText(timeFormat.format(new Date(msg.timestamp)));
            chatHolder.btnReplyIncoming.setColorFilter(chatHolder.itemView.getContext().getResources().getColor(R.color.text_tertiary), android.graphics.PorterDuff.Mode.SRC_IN);
            chatHolder.btnReplyIncoming.setVisibility(View.VISIBLE);

            if (msg.imagePath != null) {
                chatHolder.ivIncomingImage.setVisibility(View.VISIBLE);
                chatHolder.tvIncomingMsg.setVisibility(msg.message != null && !msg.message.isEmpty() ? View.VISIBLE : View.GONE);
                chatHolder.tvIncomingMsg.setText(msg.message);
                
                // Hide bubble style for pure images
                chatHolder.bubbleIncoming.setBackground(null);
                
                Glide.with(chatHolder.itemView.getContext())
                    .load(msg.imagePath)
                    .centerCrop()
                    .into(chatHolder.ivIncomingImage);
                
                chatHolder.ivIncomingImage.setOnClickListener(v -> showFullScreenImage(msg.imagePath, chatHolder.itemView.getContext()));
            } else {
                chatHolder.ivIncomingImage.setVisibility(View.GONE);
                chatHolder.tvIncomingMsg.setVisibility(View.VISIBLE);
                chatHolder.tvIncomingMsg.setText(msg.message);
                
                // Restore bubble style
                chatHolder.bubbleIncoming.setBackgroundResource(R.drawable.bg_chat_bubble_incoming);
            }

            if (msg.replyToId != null) {
                chatHolder.layoutReplyIncoming.setVisibility(View.VISIBLE);
                
                // Color incoming reply pipe based on viewer's (my) role theme (POV request)
                int themeColor = "BARANGAY".equals(currentUserRole) ? 
                    chatHolder.itemView.getContext().getResources().getColor(R.color.barangay_primary) : 
                    chatHolder.itemView.getContext().getResources().getColor(R.color.primary);

                String replyPrefix = " | ";
                String replyContent = msg.replyToMessage != null ? msg.replyToMessage : "";
                String fullReplyText = replyPrefix + replyContent;
                SpannableString spannable = new SpannableString(fullReplyText);
                spannable.setSpan(new ForegroundColorSpan(themeColor), 0, replyPrefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                chatHolder.tvReplyMsgIncoming.setText(spannable);

                // Load Reply Image (Incoming)
                if (msg.replyToImagePath != null && !msg.replyToImagePath.isEmpty()) {
                    chatHolder.ivReplyImageIncoming.setVisibility(View.VISIBLE);
                    Glide.with(chatHolder.itemView.getContext())
                        .load(msg.replyToImagePath)
                        .centerCrop()
                        .into(chatHolder.ivReplyImageIncoming);
                } else {
                    chatHolder.ivReplyImageIncoming.setVisibility(View.GONE);
                }
            } else {
                chatHolder.layoutReplyIncoming.setVisibility(View.GONE);
            }

            // Reply button click
            chatHolder.btnReplyIncoming.setOnClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(msg);
                }
            });

            // Long Press to Reply
            chatHolder.bubbleIncoming.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(msg);
                }
                return true;
            });
            chatHolder.ivIncomingImage.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(msg);
                }
                return true;
            });
        }
    }

    private void showFullScreenImage(String path, android.content.Context context) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_full_image, null);
        android.widget.ImageView ivFull = dialogView.findViewById(R.id.ivFullImage);
        
        Glide.with(context).load(path).into(ivFull);
        
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(dialogView);
        dialogView.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public ChatMessage getMessageAt(int position) {
        return messages.get(position);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View layoutIncoming, layoutOutgoing, layoutReplyIncoming, layoutReplyOutgoing;
        View bubbleIncoming, bubbleOutgoing;
        TextView tvIncomingMsg, tvOutgoingMsg, tvIncomingTime, tvOutgoingTime;
        TextView tvReplyMsgIncoming, tvReplyMsgOutgoing;
        android.widget.ImageView ivAvatarIncoming, ivAvatarOutgoing;
        android.widget.ImageView ivIncomingImage, ivOutgoingImage;
        android.widget.ImageView ivReplyImageIncoming, ivReplyImageOutgoing;
        android.widget.ImageView btnReplyIncoming, btnReplyOutgoing;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutIncoming = itemView.findViewById(R.id.layoutIncoming);
            layoutOutgoing = itemView.findViewById(R.id.layoutOutgoing);
            bubbleIncoming = itemView.findViewById(R.id.bubbleIncoming);
            bubbleOutgoing = itemView.findViewById(R.id.bubbleOutgoing);
            tvIncomingMsg = itemView.findViewById(R.id.tvIncomingMessage);
            tvOutgoingMsg = itemView.findViewById(R.id.tvOutgoingMessage);
            tvIncomingTime = itemView.findViewById(R.id.tvIncomingTime);
            tvOutgoingTime = itemView.findViewById(R.id.tvOutgoingTime);

            layoutReplyIncoming = itemView.findViewById(R.id.layoutReplyIncoming);
            layoutReplyOutgoing = itemView.findViewById(R.id.layoutReplyOutgoing);
            tvReplyMsgIncoming = itemView.findViewById(R.id.tvReplyMsgIncoming);
            tvReplyMsgOutgoing = itemView.findViewById(R.id.tvReplyMsgOutgoing);
            
            ivAvatarIncoming = itemView.findViewById(R.id.ivAvatarIncoming);
            ivAvatarOutgoing = itemView.findViewById(R.id.ivAvatarOutgoing);
            ivIncomingImage = itemView.findViewById(R.id.ivIncomingImage);
            ivOutgoingImage = itemView.findViewById(R.id.ivOutgoingImage);
            ivReplyImageIncoming = itemView.findViewById(R.id.ivReplyImageIncoming);
            ivReplyImageOutgoing = itemView.findViewById(R.id.ivReplyImageOutgoing);
            btnReplyIncoming = itemView.findViewById(R.id.btnReplyIncoming);
            btnReplyOutgoing = itemView.findViewById(R.id.btnReplyOutgoing);
        }
    }

    static class TypingViewHolder extends RecyclerView.ViewHolder {
        View dot1, dot2, dot3;

        TypingViewHolder(@NonNull View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.dot1);
            dot2 = itemView.findViewById(R.id.dot2);
            dot3 = itemView.findViewById(R.id.dot3);
        }

        void startAnimation() {
            animateDot(dot1, 0);
            animateDot(dot2, 200);
            animateDot(dot3, 400);
        }

        private void animateDot(View dot, long delay) {
            dot.setTranslationY(0f);
            android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofFloat(dot, "translationY", 0f, -10f * itemView.getContext().getResources().getDisplayMetrics().density, 0f);
            animator.setDuration(800);
            animator.setStartDelay(delay);
            animator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
            animator.setRepeatMode(android.animation.ObjectAnimator.RESTART);
            animator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            animator.start();
        }
    }
}
