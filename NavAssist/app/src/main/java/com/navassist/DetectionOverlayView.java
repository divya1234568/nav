package com.navassist;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * Transparent overlay drawn on top of camera preview.
 * Shows bounding boxes around detected objects.
 */
public class DetectionOverlayView extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private List<RectF> boxes = new ArrayList<>();

    public DetectionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        boxPaint.setColor(Color.parseColor("#FF0057FF"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(5f);
        boxPaint.setPathEffect(new DashPathEffect(new float[]{10, 5}, 0));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setShadowLayer(4, 2, 2, Color.BLACK);
    }

    public void setBoxes(List<RectF> newBoxes) {
        this.boxes = new ArrayList<>(newBoxes);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RectF box : boxes) {
            // Scale box to view dimensions
            float scaleX = getWidth() / 1280f;
            float scaleY = getHeight() / 720f;
            RectF scaled = new RectF(
                box.left * scaleX,
                box.top * scaleY,
                box.right * scaleX,
                box.bottom * scaleY
            );
            canvas.drawRoundRect(scaled, 12, 12, boxPaint);
        }
    }
}
