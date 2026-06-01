package com.example.instacare.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import com.google.android.material.button.MaterialButton;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import com.example.instacare.R;

import com.example.instacare.R;

public class TutorialOverlayView extends FrameLayout {

    private Paint backgroundPaint;
    private Paint erasePaint;
    private Bitmap bitmap;
    private Canvas tempCanvas;
    private RectF targetRect;
    private View targetView;
    private Bitmap blurredBackground; // Frosted glass background
    
    private Paint glowPaint;
    private boolean isCircle = false;
    private int customPadding = 32;
    private View tooltipView;
    private TextView tvMessage;
    private OnTutorialClickListener listener;
    private boolean forceAbove = false;
    
    // Typewriter effect variables
    private Handler typewriterHandler = new Handler(Looper.getMainLooper());
    private Handler cursorHandler = new Handler(Looper.getMainLooper());
    private String fullText = "";
    private int textIndex = 0;
    private boolean isCursorVisible = true;
    private static final long TYPING_SPEED = 95; // ms per character (Slowed to match speech pace)
    private static final long CURSOR_BLINK_SPEED = 500; // ms
    private android.animation.AnimatorSet talkingAnimator;
    private TextToSpeech tts;
    private boolean isTTSReady = false;
    private boolean isMuted = false;
    private String pendingText = null;
    private int voiceTargetIndex = 0;
    private boolean isThinking = false;
    private Runnable thinkingDotsRunnable;

    public interface OnTutorialClickListener {
        void onNextClicked();
    }

    public TutorialOverlayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setClickable(true);
        setFocusable(true);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#99000000")); // Lighter scrim since blur handles depth

        erasePaint = new Paint();
        erasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        erasePaint.setAntiAlias(true);

        glowPaint = new Paint();
        glowPaint.setColor(Color.WHITE);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(6f);
        glowPaint.setAntiAlias(true);
        glowPaint.setMaskFilter(new android.graphics.BlurMaskFilter(15, android.graphics.BlurMaskFilter.Blur.NORMAL));

        targetRect = new RectF();
        typewriterHandler = new Handler(Looper.getMainLooper());
        cursorHandler = new Handler(Looper.getMainLooper());
        
        // --- CENTRALIZED VOICE ENGINE ---
        VoiceManager vm = VoiceManager.getInstance(getContext());
        this.tts = vm.getTts();
        
        if (vm.isReady()) {
            isTTSReady = true;
            setupTTSListener();
        }
    }

    private void setupTTSListener() {
        if (tts == null) return;
        
        this.tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // --- CINEMATIC REVEAL ---
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isThinking) {
                        stopThinkingDots();
                        triggerSyncAnimations();
                    }
                });
            }

            @Override
            public void onDone(String utteranceId) {
                // --- PERFECT SILENCE SYNC ---
                new Handler(Looper.getMainLooper()).post(() -> {
                    textIndex = fullText.length();
                    tvMessage.setText(fullText);
                    stopTalkingAnimation(); 
                });
            }

            @Override
            public void onError(String utteranceId) {
                new Handler(Looper.getMainLooper()).post(() -> triggerSyncAnimations());
            }

            @Override
            public void onRangeStart(String utteranceId, int start, int end, int frame) {
                voiceTargetIndex = end;
            }
        });
        
        if (pendingText != null) {
            String clean = pendingText.replaceAll("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E6}-\\x{1F1FF}]", "");
            tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "CaraSpeech_" + System.currentTimeMillis());
            pendingText = null;
            new Handler(Looper.getMainLooper()).post(() -> triggerSyncAnimations());
        }
    }

    // Polling logic to wait for VoiceManager if it's still initializing
    private void checkVoiceReadiness() {
        VoiceManager vm = VoiceManager.getInstance(getContext());
        if (vm.isReady()) {
            isTTSReady = true;
            setupTTSListener();
            if (pendingText != null) {
                pendingText = null;
                startTypewriter();
            }
        } else if (isThinking) {
            // Keep polling while we are in the "Thinking" state
            typewriterHandler.postDelayed(this::checkVoiceReadiness, 200);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (tts != null) {
            tts.stop();
        }
    }

    public void setTarget(View view, String message, boolean isCircle, int padding, OnTutorialClickListener listener) {
        setTarget(view, message, isCircle, padding, false, false, listener);
    }

    public void setTarget(View view, String message, boolean isCircle, int padding, boolean forceAbove, OnTutorialClickListener listener) {
        setTarget(view, message, isCircle, padding, forceAbove, false, listener);
    }

    public void setTarget(View view, String message, boolean isCircle, int padding, boolean forceAbove, boolean isMuted, OnTutorialClickListener listener) {
        this.targetView = view;
        this.isCircle = isCircle;
        this.customPadding = padding;
        this.listener = listener;
        this.fullText = message;
        this.forceAbove = forceAbove;
        this.isMuted = isMuted;

        // --- STOP PREVIOUS VOICE IMMEDIATELY ---
        if (tts != null) {
            tts.stop();
        }
        
        // Cancel any pending typing
        typewriterHandler.removeCallbacksAndMessages(null);
        cursorHandler.removeCallbacksAndMessages(null);
        
        // Remove existing views if any
        removeAllViews();
        
        // Inflate custom bubble
        tooltipView = LayoutInflater.from(getContext()).inflate(R.layout.layout_tutorial_bubble, this, false);
        tvMessage = tooltipView.findViewById(R.id.tvTutorialMessage);
        tvMessage.setText(""); // Start empty for typewriter
        
        MaterialButton btnNext = tooltipView.findViewById(R.id.btnTutorialNext);
        btnNext.setOnClickListener(v -> {
            // --- SKIP VOICE ---
            if (tts != null) tts.stop();
            
            if (textIndex < fullText.length()) {
                // Skip typing if clicked mid-way
                textIndex = fullText.length();
                tvMessage.setText(fullText);
                typewriterHandler.removeCallbacksAndMessages(null);
                cursorHandler.removeCallbacksAndMessages(null);
                stopTalkingAnimation();
            } else if (listener != null) {
                // --- OUTRO ANIMATION: Shrink + Slide + Fade Out ---
                animateOutro(() -> listener.onNextClicked());
            }
        });

        addView(tooltipView);
        
        View ivTutorialCara = tooltipView.findViewById(R.id.ivTutorialCara);
        
        // --- CINEMATIC INTRO: Avatar pops in, then bubble slides up ---
        ivTutorialCara.setScaleX(0f);
        ivTutorialCara.setScaleY(0f);
        ivTutorialCara.setAlpha(0f);
        
        // Bubble starts hidden, slightly below, and small
        tooltipView.setAlpha(0f);
        tooltipView.setScaleX(0.85f);
        tooltipView.setScaleY(0.85f);
        tooltipView.setTranslationY(60f);
        
        ivTutorialCara.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(350)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                .withEndAction(() -> {
                    // --- BUBBLE INTRO: Scale up + Slide up + Fade in ---
                    tooltipView.animate()
                            .alpha(1.0f)
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationY(0f)
                            .setDuration(350)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                            .withEndAction(() -> startTypewriter())
                            .start();
                })
                .start();
        
        if (ivTutorialCara != null) {
            float density = getResources().getDisplayMetrics().density;
            ivTutorialCara.setCameraDistance(8000 * density);
        }

        // Calculate target rect using Screen coordinates for absolute precision (including status/nav bars)
        if (view != null) {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            targetRect.set(location[0] - customPadding, location[1] - customPadding, 
                           location[0] + view.getWidth() + customPadding, 
                           location[1] + view.getHeight() + customPadding);
            
            // Position tooltip
            positionTooltip();
        } else {
            // Intro step with no target
            targetRect.setEmpty();
            // Center tooltip
            LayoutParams lp = (LayoutParams) tooltipView.getLayoutParams();
            lp.gravity = android.view.Gravity.CENTER;
            tooltipView.setLayoutParams(lp);
        }
        
        // Capture blurred background from activity content
        captureBlurredBackground();
        
        invalidate();
    }

    private void positionTooltip() {
        LayoutParams lp = (LayoutParams) tooltipView.getLayoutParams();
        // Use full display height including system bars for positioning
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        
        // If forced or target is in lower half, place tooltip above, else below
        if (!targetRect.isEmpty() && (forceAbove || targetRect.centerY() > screenHeight / 2)) {
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
            // Subtract more to move it HIGHER up (avoiding overlay)
            // Use dynamic density for precise lift
            float density = getResources().getDisplayMetrics().density;
            lp.topMargin = (int) (targetRect.top - 280 * density); 
            if (lp.topMargin < 120) lp.topMargin = 120; // Ensure it doesn't hit status bar
        } else if (!targetRect.isEmpty()) {
            lp.gravity = android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL;
            // Add more to move it LOWER down
            lp.topMargin = (int) (targetRect.bottom + 40 * getResources().getDisplayMetrics().density);
        } else {
            // Default center for intro
            lp.gravity = android.view.Gravity.CENTER;
        }
        tooltipView.setLayoutParams(lp);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Ensure bitmap matches the full screen size
        if (bitmap == null || bitmap.getWidth() != getWidth() || bitmap.getHeight() != getHeight()) {
            if (bitmap != null) bitmap.recycle();
            bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            tempCanvas = new Canvas(bitmap);
        }

        bitmap.eraseColor(Color.TRANSPARENT);
        
        // --- FROSTED GLASS: Draw blurred background first ---
        if (blurredBackground != null && !blurredBackground.isRecycled()) {
            tempCanvas.drawBitmap(blurredBackground, 0, 0, null);
        }
        
        // Semi-transparent dark scrim on top of blur
        tempCanvas.drawPaint(backgroundPaint);

        if (!targetRect.isEmpty()) {
            if (isCircle) {
                float radius = Math.max(targetRect.width(), targetRect.height()) / 2f;
                // Draw glowing halo (siga)
                tempCanvas.drawCircle(targetRect.centerX(), targetRect.centerY(), radius, glowPaint);
                // Draw hole
                tempCanvas.drawCircle(targetRect.centerX(), targetRect.centerY(), radius, erasePaint);
            } else {
                // Draw glowing halo (siga)
                tempCanvas.drawRoundRect(targetRect, 24, 24, glowPaint);
                // Draw hole
                tempCanvas.drawRoundRect(targetRect, 24, 24, erasePaint);
            }
        }

        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        // Re-calculate target rect on layout changes to ensure alignment stays perfect
        if (targetView != null) {
            int[] location = new int[2];
            targetView.getLocationOnScreen(location);
            targetRect.set(location[0] - customPadding, location[1] - customPadding, 
                           location[0] + targetView.getWidth() + customPadding, 
                           location[1] + targetView.getHeight() + customPadding);
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        // Absolute block: consume all touches to prevent background button clicks
        return true;
    }

    private void startTypewriter() {
        textIndex = 0;
        tvMessage.setText("");
        
        // Lock Next button while talking
        MaterialButton btnNext = tooltipView.findViewById(R.id.btnTutorialNext);
        if (btnNext != null) {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
        }
        
        // --- CINEMATIC SEQUENCING ---
        isThinking = true;
        tvMessage.setText("");
        
        // --- STEP 1: START THINKING DOTS ---
        startThinkingDots();
        
        // --- AI VOICE ---
        if (!isMuted) {
            VoiceManager vm = VoiceManager.getInstance(getContext());
            if (vm.isReady()) {
                isTTSReady = true;
                setupTTSListener();
                
                String cleanText = fullText.replaceAll("[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{1F1E6}-\\x{1F1FF}]", "");
                pendingText = null;
                voiceTargetIndex = 0;
                
                // Track ID to ensure we only react to the current speech
                tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "CaraSpeech_" + System.currentTimeMillis());
            } else {
                // Not ready? Buffer the text and start polling!
                pendingText = fullText;
                checkVoiceReadiness();
            }
        }
        
        // --- AUTOMATIC FALLBACK ---
        // If voice fails to start for 2.5 seconds, force the reveal anyway
        typewriterHandler.postDelayed(() -> {
            if (isThinking) {
                stopThinkingDots();
                triggerSyncAnimations();
                voiceTargetIndex = fullText.length(); // Allow fallback typing
                typewriterHandler.post(typewriterRunnable);
            }
        }, 2500);
    }

    private void startThinkingDots() {
        if (!isThinking) return;
        final String[] dots = {"", ".", "..", "..."};
        final int[] dotIndex = {0};
        
        thinkingDotsRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isThinking) return;
                tvMessage.setText(dots[dotIndex[0]]);
                dotIndex[0] = (dotIndex[0] + 1) % dots.length;
                typewriterHandler.postDelayed(this, 300);
            }
        };
        typewriterHandler.post(thinkingDotsRunnable);
    }

    private void stopThinkingDots() {
        isThinking = false;
        typewriterHandler.removeCallbacks(thinkingDotsRunnable);
        tvMessage.setText("");
    }

    private void triggerSyncAnimations() {
        if (textIndex > 0) return; // Already started
        startTalkingAnimation();
        typewriterHandler.post(typewriterRunnable);
        cursorHandler.post(cursorRunnable);
    }

    private void startTalkingAnimation() {
        View avatar = tooltipView.findViewById(R.id.ivTutorialCara);
        View waveL = tooltipView.findViewById(R.id.tvWaveLeft);
        View waveR = tooltipView.findViewById(R.id.tvWaveRight);
        View waveOuterL = tooltipView.findViewById(R.id.tvWaveOuterLeft);
        View waveOuterRight = tooltipView.findViewById(R.id.tvWaveOuterRight);
        
        if (avatar == null) return;
        
        if (talkingAnimator != null) talkingAnimator.cancel();
        
        // --- PROUNCED VISIBILITY: Higher scale and bounce ---
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(avatar, "scaleX", 1f, 1.15f, 1f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(avatar, "scaleY", 1f, 1.15f, 1f);
        android.animation.ObjectAnimator bounce = android.animation.ObjectAnimator.ofFloat(avatar, "translationY", 0f, -8f, 0f);
        
        talkingAnimator = new android.animation.AnimatorSet();
        
        // --- SOUND RIPPLES (Only if NOT muted) ---
        if (!isMuted) {
            // Inner ' ( ) '
            android.animation.ObjectAnimator waveAlphaL = android.animation.ObjectAnimator.ofFloat(waveL, "alpha", 0f, 1f, 0f);
            android.animation.ObjectAnimator waveAlphaR = android.animation.ObjectAnimator.ofFloat(waveR, "alpha", 0f, 1f, 0f);
            android.animation.ObjectAnimator waveScaleL = android.animation.ObjectAnimator.ofFloat(waveL, "scaleX", 0.6f, 1.5f);
            android.animation.ObjectAnimator waveScaleR = android.animation.ObjectAnimator.ofFloat(waveR, "scaleX", 0.6f, 1.5f);
            
            // Outer ' (( )) ' (Syncopated)
            android.animation.ObjectAnimator waveOuterAlphaL = android.animation.ObjectAnimator.ofFloat(waveOuterL, "alpha", 0f, 0.7f, 0f);
            android.animation.ObjectAnimator waveOuterAlphaR = android.animation.ObjectAnimator.ofFloat(waveOuterRight, "alpha", 0f, 0.7f, 0f);
            android.animation.ObjectAnimator waveOuterScaleL = android.animation.ObjectAnimator.ofFloat(waveOuterL, "scaleX", 0.4f, 1.8f);
            android.animation.ObjectAnimator waveOuterScaleR = android.animation.ObjectAnimator.ofFloat(waveOuterRight, "scaleX", 0.4f, 1.8f);

            talkingAnimator.playTogether(
                scaleX, scaleY, bounce, 
                waveAlphaL, waveAlphaR, waveScaleL, waveScaleR,
                waveOuterAlphaL, waveOuterAlphaR, waveOuterScaleL, waveOuterScaleR
            );
        } else {
            // Just the avatar bounce if muted
            talkingAnimator.playTogether(scaleX, scaleY, bounce);
        }
        talkingAnimator.setDuration(400); 
        talkingAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        
        talkingAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Check if still speaking or typing
                if (textIndex <= fullText.length() && (tts != null && tts.isSpeaking())) {
                    animation.start();
                }
            }
        });
        talkingAnimator.start();
    }

    private void stopTalkingAnimation() {
        // --- CLEANUP ---
        if (talkingAnimator != null) {
            talkingAnimator.cancel();
            talkingAnimator = null;
        }

        // --- UNLOCK INTERACTION ---
        MaterialButton btnNext = tooltipView.findViewById(R.id.btnTutorialNext);
        if (btnNext != null) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
        }

        // --- VISUAL RESET: Flush all ripples to zero ---
        View avatar = tooltipView.findViewById(R.id.ivTutorialCara);
        View waveL = tooltipView.findViewById(R.id.tvWaveLeft);
        View waveR = tooltipView.findViewById(R.id.tvWaveRight);
        View waveOuterL = tooltipView.findViewById(R.id.tvWaveOuterLeft);
        View waveOuterRight = tooltipView.findViewById(R.id.tvWaveOuterRight);
        
        if (avatar != null) {
            avatar.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(200).start();
        }
        if (waveL != null) waveL.animate().alpha(0f).setDuration(200).start();
        if (waveR != null) waveR.animate().alpha(0f).setDuration(200).start();
        if (waveOuterL != null) waveOuterL.animate().alpha(0f).setDuration(200).start();
        if (waveOuterRight != null) waveOuterRight.animate().alpha(0f).setDuration(200).start();
    }

    /**
     * Outro animation: Bubble shrinks + slides down + fades out,
     * Avatar shrinks + fades. Calls onComplete after both finish.
     */
    private void animateOutro(Runnable onComplete) {
        if (tooltipView == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        
        // Stop any active typing/cursor
        typewriterHandler.removeCallbacksAndMessages(null);
        cursorHandler.removeCallbacksAndMessages(null);
        
        View avatar = tooltipView.findViewById(R.id.ivTutorialCara);
        
        // --- BUBBLE EXIT: Shrink + Slide down + Fade ---
        tooltipView.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .translationY(50f)
                .setDuration(250)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .start();
        
        // --- AVATAR EXIT: Shrink + Fade ---
        if (avatar != null) {
            avatar.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(200)
                    .setStartDelay(100) // Slight stagger after bubble starts
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        if (onComplete != null) onComplete.run();
                    })
                    .start();
        } else {
            // Fallback: run after bubble animation
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (onComplete != null) onComplete.run();
            }, 280);
        }
    }

    private void updateTypewriterText() {
        if (tvMessage == null || fullText == null) return;
        String revealedText = fullText.substring(0, Math.min(textIndex, fullText.length()));
        tvMessage.setText(revealedText + (isCursorVisible ? "|" : ""));
        
        // --- ZERO-DEADLOCK SAFETY ---
        // Automatically unlock the Next button when typing is done, 
        // ensuring the user is never stuck even if voice fails.
        if (textIndex >= fullText.length()) {
            stopTalkingAnimation();
        }
    }

    private final Runnable typewriterRunnable = new Runnable() {
        @Override
        public void run() {
            // --- FLUID SYLLABLE CHASE ---
            if (textIndex < voiceTargetIndex) {
                textIndex++;
                updateTypewriterText();
            } 
            // --- FALLBACK SLOW-DRAW ---
            // If voice hasn't started yet, keep drawing slowly to show activity
            else if (textIndex < fullText.length() && voiceTargetIndex == 0) {
                textIndex++;
                updateTypewriterText();
            }
            
            if (textIndex < fullText.length()) {
                // Adjust frequency based on state
                long delay = (voiceTargetIndex > 0) ? 30 : 120; 
                typewriterHandler.postDelayed(this, delay); 
            }
        }
    };

    private final Runnable cursorRunnable = new Runnable() {
        @Override
        public void run() {
            isCursorVisible = !isCursorVisible;
            String current = tvMessage.getText().toString();
            if (textIndex <= fullText.length()) {
                String revealed = fullText.substring(0, Math.min(textIndex, fullText.length()));
                tvMessage.setText(revealed + (isCursorVisible ? "|" : " "));
            }
            cursorHandler.postDelayed(this, CURSOR_BLINK_SPEED);
        }
    };

    /**
     * Capture the activity content and apply a strong blur for the frosted glass effect.
     * This runs once per setTarget call so the blur snapshot matches the current screen state.
     */
    private void captureBlurredBackground() {
        try {
            // Find the activity's decor view for a clean full-screen snapshot
            android.app.Activity activity = (android.app.Activity) getContext();
            ViewGroup parent = (ViewGroup) activity.getWindow().getDecorView();
            if (parent == null) return;
            
            // Temporarily hide this overlay to capture clean background
            setVisibility(INVISIBLE);
            
            // Capture at reduced resolution for performance
            float scale = 0.25f;
            int w = (int) (parent.getWidth() * scale);
            int h = (int) (parent.getHeight() * scale);
            if (w <= 0 || h <= 0) { setVisibility(VISIBLE); return; }
            
            Bitmap capture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas captureCanvas = new Canvas(capture);
            captureCanvas.scale(scale, scale);
            parent.draw(captureCanvas);
            
            setVisibility(VISIBLE);
            
            // Apply StackBlur (works on all API levels)
            if (blurredBackground != null && !blurredBackground.isRecycled()) {
                blurredBackground.recycle();
            }
            blurredBackground = stackBlur(capture, 25);
            
            // Scale back up to screen size
            blurredBackground = Bitmap.createScaledBitmap(blurredBackground, parent.getWidth(), parent.getHeight(), true);
            capture.recycle();
        } catch (Exception e) {
            e.printStackTrace();
            setVisibility(VISIBLE);
        }
    }
    
    /**
     * Fast StackBlur implementation for frosted glass effect.
     * Applies a box blur approximation that runs efficiently on all devices.
     */
    private static Bitmap stackBlur(Bitmap sentBitmap, int radius) {
        Bitmap bmp = sentBitmap.copy(sentBitmap.getConfig(), true);
        if (radius < 1) return bmp;
        
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pix = new int[w * h];
        bmp.getPixels(pix, 0, w, 0, 0, w, h);
        
        int wm = w - 1, hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;
        int[] r = new int[wh], g = new int[wh], b = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int[] vmin = new int[Math.max(w, h)];
        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int[] dv = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) dv[i] = (i / divsum);
        
        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer, stackstart, rbs;
        int[] sir;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum, rinsum, ginsum, binsum;
        
        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; }
            }
            stackpointer = radius;
            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum]; g[yi] = dv[gsum]; b[yi] = dv[bsum];
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2];
                if (y == 0) vmin[x] = Math.min(x + radius + 1, wm);
                p = pix[yw + vmin[x]];
                sir[0] = (p & 0xff0000) >> 16; sir[1] = (p & 0x00ff00) >> 8; sir[2] = (p & 0x0000ff);
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                rsum += rinsum; gsum += ginsum; bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer % div];
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2];
                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;
                sir = stack[i + radius];
                sir[0] = r[yi]; sir[1] = g[yi]; sir[2] = b[yi];
                rbs = r1 - Math.abs(i);
                rsum += r[yi] * rbs; gsum += g[yi] * rbs; bsum += b[yi] * rbs;
                if (i > 0) { rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2]; }
                else { routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2]; }
                if (i < hm) yp += w;
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                rsum -= routsum; gsum -= goutsum; bsum -= boutsum;
                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];
                routsum -= sir[0]; goutsum -= sir[1]; boutsum -= sir[2];
                if (x == 0) vmin[y] = Math.min(y + r1, hm) * w;
                p = x + vmin[y];
                sir[0] = r[p]; sir[1] = g[p]; sir[2] = b[p];
                rinsum += sir[0]; ginsum += sir[1]; binsum += sir[2];
                rsum += rinsum; gsum += ginsum; bsum += binsum;
                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];
                routsum += sir[0]; goutsum += sir[1]; boutsum += sir[2];
                rinsum -= sir[0]; ginsum -= sir[1]; binsum -= sir[2];
                yi += w;
            }
        }
        bmp.setPixels(pix, 0, w, 0, 0, w, h);
        return bmp;
    }
}
