package com.example.smarthall.ui.customview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ThemeColorCircleView extends View {

    private final Paint primaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint secondaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect = new RectF();

    public ThemeColorCircleView(Context context) {
        super(context);
        init();
    }

    public ThemeColorCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThemeColorCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        primaryPaint.setStyle(Paint.Style.FILL);
        secondaryPaint.setStyle(Paint.Style.FILL);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
        borderPaint.setColor(Color.parseColor("#E0E0E0"));
    }

    public void setColors(int primaryColor, int secondaryColor) {
        primaryPaint.setColor(primaryColor);
        secondaryPaint.setColor(secondaryColor);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        float stroke = borderPaint.getStrokeWidth();
        ovalRect.set(stroke, stroke, width - stroke, height - stroke);

        canvas.drawArc(ovalRect, 180f, 180f, true, primaryPaint);
        canvas.drawArc(ovalRect, 0f, 180f, true, secondaryPaint);
        canvas.drawOval(ovalRect, borderPaint);
    }
}