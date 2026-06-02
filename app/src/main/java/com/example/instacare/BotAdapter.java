package com.example.instacare;

import static com.example.instacare.R.id.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AssistantMessage;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class BotAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_TEXT = 0;
    public static final int TYPE_ACTION_CARD = 1;
    public static final int TYPE_FEEDBACK = 2; // NEW: Organized Feedback
    public static final int TYPE_NOTIFICATION = 3;
    public static final int TYPE_TYPING = 4;
    public static final int TYPE_HEADER = 5;
    public static final int TYPE_LANGUAGE_SELECTION = 6;

    private List<AssistantMessage> messages;
    private OnCardActionListener actionListener;

    public interface OnCardActionListener {
        void onActionProceed(AssistantMessage message);
        void onActionCancel(AssistantMessage message);
    }

    public BotAdapter(List<AssistantMessage> messages, OnCardActionListener listener) {
        this.messages = messages;
        this.actionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ACTION_CARD || viewType == TYPE_FEEDBACK) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bot_action_card, parent, false);
            return new ActionCardViewHolder(v);
        } else if (viewType == TYPE_NOTIFICATION) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bot_action_card, parent, false);
            return new NotificationViewHolder(v);
        } else if (viewType == TYPE_TYPING) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_typing_indicator, parent, false);
            return new TypingViewHolder(v);
        } else if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cara_header, parent, false);
            return new HeaderViewHolder(v);
        } else if (viewType == TYPE_LANGUAGE_SELECTION) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bot_language_selection, parent, false);
            return new LanguageViewHolder(v);
        }
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bot_message, parent, false);
        return new TextViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AssistantMessage msg = messages.get(position);
        
        if (holder instanceof TextViewHolder) {
            TextViewHolder textHolder = (TextViewHolder) holder;
            if (msg.isBot) {
                textHolder.llBot.setVisibility(View.VISIBLE);
                textHolder.llUser.setVisibility(View.GONE);
                textHolder.tvBot.setText(msg.text);
            } else {
                textHolder.llBot.setVisibility(View.GONE);
                textHolder.llUser.setVisibility(View.VISIBLE);
                textHolder.tvUser.setText(msg.text);
            }
        } else if (holder instanceof ActionCardViewHolder) {
            ((ActionCardViewHolder) holder).bind(msg);
        } else if (holder instanceof NotificationViewHolder) {
            ((NotificationViewHolder) holder).bind(msg);
        } else if (holder instanceof TypingViewHolder) {
            ((TypingViewHolder) holder).startAnimation();
        } else if (holder instanceof HeaderViewHolder) {
            // Static header for now, no binding needed
        } else if (holder instanceof LanguageViewHolder) {
            ((LanguageViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class TextViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llBot, llUser;
        TextView tvBot, tvUser;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            llBot = itemView.findViewById(R.id.llBotMessage);
            llUser = itemView.findViewById(R.id.llUserMessage);
            tvBot = itemView.findViewById(R.id.tvBotText);
            tvUser = itemView.findViewById(R.id.tvUserText);
        }
    }

    class ActionCardViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent;
        MaterialButton btnProceed, btnCancel;
        android.widget.ImageView ivAvatar;

        ActionCardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvCardTitle);
            tvContent = itemView.findViewById(R.id.tvCardContent);
            btnProceed = itemView.findViewById(R.id.btnCardPrimary);
            btnCancel = itemView.findViewById(R.id.btnCardSecondary);
            ivAvatar = itemView.findViewById(R.id.ivActionCaraAvatar);
        }

        void bind(AssistantMessage msg) {
            boolean isInfo = "INFO".equals(msg.metadata);
            tvTitle.setText(isInfo ? "Your Information" : "Review Action");
            tvContent.setText(msg.text);
            
            if (isInfo) {
                btnProceed.setVisibility(View.GONE);
                btnCancel.setVisibility(View.GONE);
            } else {
                btnProceed.setVisibility(View.VISIBLE);
                btnCancel.setVisibility(View.VISIBLE);
                
                // --- ORGANIZE: Custom labels for Feedback Loop ---
                if (msg.type == TYPE_FEEDBACK) {
                    tvTitle.setText("User Feedback");
                    btnProceed.setText("Yes");
                    btnCancel.setText("No");
                } else {
                    btnProceed.setText("Proceed");
                    btnCancel.setText("Cancel");
                }

                btnProceed.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onActionProceed(msg);
                });
                btnCancel.setOnClickListener(v -> {
                    if (actionListener != null) actionListener.onActionCancel(msg);
                });
            }
        }
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent;
        MaterialButton btnAction, btnCancel;
        android.widget.ImageView ivAvatar;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvCardTitle);
            tvContent = itemView.findViewById(R.id.tvCardContent);
            btnAction = itemView.findViewById(R.id.btnCardPrimary);
            btnCancel = itemView.findViewById(R.id.btnCardSecondary);
            ivAvatar = itemView.findViewById(R.id.ivActionCaraAvatar);
        }

        void bind(AssistantMessage msg) {
            tvTitle.setText("System Notification");
            tvContent.setText(msg.text);
            btnAction.setText("View Details");
            btnAction.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onActionProceed(msg);
            });
            btnCancel.setVisibility(View.GONE);
        }
    }

    class TypingViewHolder extends RecyclerView.ViewHolder {
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

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            
            // Check if we should play the welcome animation
            SharedPreferences prefs = itemView.getContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);
            boolean alreadyAnimated = prefs.getBoolean("CARA_WELCOME_ANIMATED", false);
            
            View avatar = itemView.findViewById(R.id.ivCaraAvatar);
            View name = itemView.findViewById(R.id.tvCaraName);
            View subtitle = itemView.findViewById(R.id.tvCaraSubtitle);

            if (!alreadyAnimated && avatar != null) {
                // Initial hidden state
                avatar.setScaleX(0f);
                avatar.setScaleY(0f);
                avatar.setAlpha(0f);
                
                // 3D Setup: Set camera distance for depth
                float density = itemView.getContext().getResources().getDisplayMetrics().density;
                avatar.setCameraDistance(10000 * density);
                avatar.setRotationY(0f);
                
                if (name != null) name.setAlpha(0f);
                if (subtitle != null) subtitle.setAlpha(0f);

                // Premium 3D Pop-in Animation
                avatar.postDelayed(() -> {
                    // Combine scale/fade with 3D Spin
                    avatar.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .alpha(1f)
                        .setDuration(800)
                        .setInterpolator(new OvershootInterpolator(1.4f))
                        .withEndAction(() -> {
                            avatar.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
                        })
                        .start();
                        
                    // 360 Degree 3D Spin
                    android.animation.ObjectAnimator.ofFloat(avatar, "rotationY", 0f, 360f)
                        .setDuration(1000)
                        .start();
                        
                    if (name != null) {
                        name.animate().alpha(1f).setDuration(600).setStartDelay(400).start();
                    }
                    if (subtitle != null) {
                        subtitle.animate().alpha(1f).setDuration(600).setStartDelay(600).start();
                    }
                    
                    // Mark as animated so it doesn't repeat
                    prefs.edit().putBoolean("CARA_WELCOME_ANIMATED", true).apply();
                }, 400); // Small delay to ensure layout is ready
            }
        }
    }

    public interface OnLanguageClickListener {
        void onLanguageSelected(String lang);
    }

    private OnLanguageClickListener languageClickListener;

    public void setOnLanguageClickListener(OnLanguageClickListener listener) {
        this.languageClickListener = listener;
    }

    class LanguageViewHolder extends RecyclerView.ViewHolder {
        MaterialButton btnBisaya, btnTagalog, btnEnglish;

        LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            btnBisaya = itemView.findViewById(R.id.btnLangBisaya);
            btnTagalog = itemView.findViewById(R.id.btnLangTagalog);
            btnEnglish = itemView.findViewById(R.id.btnLangEnglish);
        }

        void bind(AssistantMessage msg) {
            btnBisaya.setOnClickListener(v -> {
                if (languageClickListener != null) languageClickListener.onLanguageSelected("Bisaya");
            });
            btnTagalog.setOnClickListener(v -> {
                if (languageClickListener != null) languageClickListener.onLanguageSelected("Tagalog");
            });
            btnEnglish.setOnClickListener(v -> {
                if (languageClickListener != null) languageClickListener.onLanguageSelected("English");
            });
        }
    }
}
