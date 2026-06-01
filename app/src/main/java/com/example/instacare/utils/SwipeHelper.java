package com.example.instacare.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.R;

public class SwipeHelper {
    private final Context context;
    private final Paint paint;
    private final Drawable deleteIcon;

    public SwipeHelper(Context context) {
        this.context = context;
        this.paint = new Paint();
        this.deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_trash);
        if (deleteIcon != null) {
            deleteIcon.setTint(Color.WHITE);
        }
    }

    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, 
                           @NonNull RecyclerView.ViewHolder vh, float dX, float dY, 
                           int actionState, boolean isCurrentlyActive) {
        
        View itemView = vh.itemView;
        if (dX < 0) { // Swiping Left
            // 1. Draw Red Background
            paint.setColor(Color.parseColor("#EF4444")); // Red-500
            RectF background = new RectF(
                (float) itemView.getRight() + dX,
                (float) itemView.getTop(),
                (float) itemView.getRight(),
                (float) itemView.getBottom()
            );
            c.drawRect(background, paint);

            // 2. Draw Delete Icon
            if (deleteIcon != null) {
                int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                int iconRight = itemView.getRight() - iconMargin;

                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);
            }
        }
    }
}
