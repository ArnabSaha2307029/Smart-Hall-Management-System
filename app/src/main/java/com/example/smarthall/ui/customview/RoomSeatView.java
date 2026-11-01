package com.example.smarthall.ui.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.example.smarthall.R;
import com.example.smarthall.model.Room;

import java.util.HashMap;
import java.util.Map;

public class RoomSeatView extends View {

    public static final int STATE_AVAILABLE = 0;
    public static final int STATE_OCCUPIED = 1;
    public static final int STATE_MY_SEAT = 2;
    public static final int STATE_SELECTED = 3;

    private static final String[] SEAT_LABELS = {Room.SEAT_A, Room.SEAT_B, Room.SEAT_C, Room.SEAT_D};
    private static final int[][] GRID = {{0, 0}, {0, 1}, {1, 0}, {1, 1}};

    private Paint seatPaint;
    private Paint borderPaint;
    private Paint labelPaint;
    private Paint occupantPaint;
    private Paint legendPaint;
    private Paint titlePaint;
    private Paint legendRectPaint;

    private int colorAvailable;
    private int colorOccupied;
    private int colorMySeat;
    private int colorSelected;
    private int colorBorder;
    private int colorLabel = Color.WHITE;
    private int colorBackground = Color.parseColor("#FAFAFA");

    private int[] seatStates = new int[]{STATE_AVAILABLE, STATE_AVAILABLE, STATE_AVAILABLE, STATE_AVAILABLE};
    private String[] seatOccupantNames = new String[]{"Empty", "Empty", "Empty", "Empty"};
    private int selectedSeatIndex = -1;
    private boolean interactiveMode = false;
    private float roomTitleTextSize = 14f;
    private String roomTitle = "Room";

    private OnSeatSelectedListener onSeatSelectedListener;

    private RectF[] seatRects = new RectF[4];
    private float legendHeight = 60f;
    private float titleHeight = 40f;
    private float padding = 16f;
    private float cornerRadius = 12f;

    public RoomSeatView(Context context) {
        super(context);
        init(context, null);
    }

    public RoomSeatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RoomSeatView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        float density = context.getResources().getDisplayMetrics().density;
        padding = 16f * density;
        legendHeight = 64f * density;
        titleHeight = 40f * density;
        cornerRadius = 10f * density;
        roomTitleTextSize = 14f * density;

        TypedValue typedValue = new TypedValue();
        int primaryColor = 0xFFC62828;
        if (context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            primaryColor = typedValue.data;
        }

        int containerColor = 0xFFFFCDD2;
        if (context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)) {
            containerColor = typedValue.data;
        } else {
            containerColor = Color.argb(60, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
        }

        colorMySeat = primaryColor;
        colorSelected = primaryColor;
        colorBorder = primaryColor;
        colorAvailable = containerColor;

        float[] hsv = new float[3];
        Color.colorToHSV(primaryColor, hsv);
        hsv[2] = Math.max(0.15f, hsv[2] * 0.45f);
        colorOccupied = Color.HSVToColor(hsv);

        if (attrs != null) {
            try {
                TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RoomSeatView);
                interactiveMode = ta.getBoolean(R.styleable.RoomSeatView_interactiveMode, false);
                roomTitleTextSize = ta.getDimension(R.styleable.RoomSeatView_roomTitleTextSize, roomTitleTextSize);
                colorAvailable = ta.getColor(R.styleable.RoomSeatView_colorAvailable, colorAvailable);
                colorOccupied = ta.getColor(R.styleable.RoomSeatView_colorOccupied, colorOccupied);
                colorMySeat = ta.getColor(R.styleable.RoomSeatView_colorMySeat, colorMySeat);
                colorSelected = ta.getColor(R.styleable.RoomSeatView_colorSelected, colorSelected);
                ta.recycle();
            } catch (Exception ignored) {
            }
        }

        setupPaints();
        for (int i = 0; i < 4; i++) {
            seatRects[i] = new RectF();
        }
    }

    private void setupPaints() {
        seatPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        seatPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);
        borderPaint.setColor(Color.WHITE);

        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(colorLabel);
        labelPaint.setTextSize(roomTitleTextSize * 1.1f);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        occupantPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        occupantPaint.setColor(Color.WHITE);
        occupantPaint.setTextSize(roomTitleTextSize * 0.72f);
        occupantPaint.setTextAlign(Paint.Align.CENTER);
        occupantPaint.setAlpha(220);

        legendPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legendPaint.setColor(Color.parseColor("#212121"));
        legendPaint.setTextSize(roomTitleTextSize * 0.78f);
        legendPaint.setTextAlign(Paint.Align.LEFT);

        titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(colorMySeat);
        titlePaint.setTextSize(roomTitleTextSize * 1.0f);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        legendRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        legendRectPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) (300 * getContext().getResources().getDisplayMetrics().density);
        int width = resolveSize(desiredWidth, widthMeasureSpec);
        float cellSize = (width - padding * 3) / 2f;
        int desiredHeight = (int) (titleHeight + padding + cellSize * 2 + padding * 2 + legendHeight);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        canvas.drawColor(colorBackground);

        float titleY = titleHeight * 0.7f;
        canvas.drawText(roomTitle, width / 2f, titleY, titlePaint);

        float gridTop = titleHeight;
        float gridHeight = height - titleHeight - legendHeight;
        float cellW = (width - padding * 3) / 2f;
        float cellH = (gridHeight - padding * 3) / 2f;

        for (int i = 0; i < 4; i++) {
            int row = GRID[i][0];
            int col = GRID[i][1];
            float left = padding + col * (cellW + padding);
            float top = gridTop + padding + row * (cellH + padding);
            float right = left + cellW;
            float bottom = top + cellH;

            seatRects[i].set(left, top, right, bottom);

            int state = seatStates[i];
            if (i == selectedSeatIndex && interactiveMode) {
                seatPaint.setColor(colorSelected);
            } else {
                switch (state) {
                    case STATE_MY_SEAT: seatPaint.setColor(colorMySeat); break;
                    case STATE_OCCUPIED: seatPaint.setColor(colorOccupied); break;
                    case STATE_AVAILABLE: seatPaint.setColor(colorAvailable); break;
                    default: seatPaint.setColor(colorAvailable); break;
                }
            }

            canvas.drawRoundRect(seatRects[i], cornerRadius, cornerRadius, seatPaint);
            canvas.drawRoundRect(seatRects[i], cornerRadius, cornerRadius, borderPaint);

            if (state == STATE_AVAILABLE && (i != selectedSeatIndex || !interactiveMode)) {
                labelPaint.setColor(colorMySeat);
                occupantPaint.setColor(colorMySeat);
            } else {
                labelPaint.setColor(Color.WHITE);
                occupantPaint.setColor(Color.WHITE);
            }

            float centerX = (left + right) / 2f;
            float centerY = (top + bottom) / 2f;
            Paint.FontMetrics fm = labelPaint.getFontMetrics();
            float labelY = centerY - (fm.ascent + fm.descent) / 2f - cellH * 0.1f;
            canvas.drawText("Seat " + SEAT_LABELS[i], centerX, labelY, labelPaint);

            String occupant = seatOccupantNames[i];
            if (occupant != null && !occupant.isEmpty() && !occupant.equals("Empty")) {
                float oY = labelY + labelPaint.getTextSize() * 1.2f;
                String displayName = occupant.length() > 12 ? occupant.substring(0, 11) + "\u2026" : occupant;
                canvas.drawText(displayName, centerX, oY, occupantPaint);
            } else if (state == STATE_AVAILABLE) {
                float oY = labelY + labelPaint.getTextSize() * 1.2f;
                canvas.drawText("Available", centerX, oY, occupantPaint);
            }
        }

        drawLegend(canvas, width, height);
    }

    private void drawLegend(Canvas canvas, int width, int height) {
        float legendTop = height - legendHeight + 8f;
        float boxSize = legendHeight * 0.28f;
        float textOffset = boxSize + 6f;
        float itemW = width / 4f;

        int[] legendColors = {colorAvailable, colorOccupied, colorMySeat, colorSelected};
        String[] legendLabels = {"Available", "Occupied", "My Seat", "Selected"};

        for (int i = 0; i < 4; i++) {
            float startX = i * itemW + 8f;
            float boxTop = legendTop;
            float boxBottom = boxTop + boxSize;

            legendRectPaint.setColor(legendColors[i]);
            canvas.drawRoundRect(new RectF(startX, boxTop, startX + boxSize, boxBottom),
                    4f, 4f, legendRectPaint);

            canvas.drawText(legendLabels[i], startX + textOffset,
                    boxTop + boxSize * 0.8f, legendPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!interactiveMode) return super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            for (int i = 0; i < seatRects.length; i++) {
                if (seatRects[i] != null && seatRects[i].contains(x, y)) {
                    handleSeatTap(i);
                    return true;
                }
            }
        }
        return true;
    }

    private void handleSeatTap(int index) {
        if (seatStates[index] == STATE_AVAILABLE || seatStates[index] == STATE_MY_SEAT) {
            selectedSeatIndex = index;
            invalidate();
            if (onSeatSelectedListener != null) {
                onSeatSelectedListener.onSeatSelected(SEAT_LABELS[index], index);
            }
        } else {
            if (onSeatSelectedListener != null) {
                onSeatSelectedListener.onSeatOccupied(SEAT_LABELS[index]);
            }
        }
    }

    public void setSeatData(int[] states, String[] occupantNames) {
        if (states != null && states.length >= 4) {
            System.arraycopy(states, 0, seatStates, 0, 4);
        }
        if (occupantNames != null && occupantNames.length >= 4) {
            System.arraycopy(occupantNames, 0, seatOccupantNames, 0, 4);
        }
        selectedSeatIndex = -1;
        invalidate();
    }

    public void setRoomTitle(String title) {
        this.roomTitle = title;
        invalidate();
    }

    public void setInteractiveMode(boolean interactive) {
        this.interactiveMode = interactive;
        invalidate();
    }

    public void setOnSeatSelectedListener(OnSeatSelectedListener listener) {
        this.onSeatSelectedListener = listener;
    }

    public int getSelectedSeatIndex() {
        return selectedSeatIndex;
    }

    public String getSelectedSeatLabel() {
        if (selectedSeatIndex >= 0 && selectedSeatIndex < SEAT_LABELS.length) {
            return SEAT_LABELS[selectedSeatIndex];
        }
        return null;
    }

    public void clearSelection() {
        selectedSeatIndex = -1;
        invalidate();
    }

    public interface OnSeatSelectedListener {
        void onSeatSelected(String seatLabel, int seatIndex);
        void onSeatOccupied(String seatLabel);
    }
}
