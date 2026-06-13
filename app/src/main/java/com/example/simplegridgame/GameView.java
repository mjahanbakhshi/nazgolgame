package com.example.simplegridgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {
    private static final int GRID_SIZE = 3;
    private static final float MIN_SWIPE_DISTANCE = 50f;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int blockRow = 1;
    private int blockColumn = 1;
    private int moveCount = 0;
    private float startX;
    private float startY;

    public GameView(Context context) {
        super(context);

        backgroundPaint.setColor(Color.rgb(245, 247, 250));

        gridPaint.setColor(Color.rgb(35, 45, 65));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(6f);

        blockPaint.setColor(Color.rgb(63, 81, 181));

        textPaint.setColor(Color.rgb(25, 30, 40));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(backgroundPaint.getColor());

        float width = getWidth();
        float height = getHeight();
        float boardSize = Math.min(width * 0.82f, height * 0.62f);
        float cellSize = boardSize / GRID_SIZE;
        float left = (width - boardSize) / 2f;
        float top = (height - boardSize) / 2f;

        drawCounter(canvas, width, top);
        drawGrid(canvas, left, top, boardSize, cellSize);
        drawBlock(canvas, left, top, cellSize);
    }

    private void drawCounter(Canvas canvas, float width, float boardTop) {
        textPaint.setTextSize(58f);
        canvas.drawText("Moves: " + moveCount, width / 2f, Math.max(95f, boardTop - 55f), textPaint);
    }

    private void drawGrid(Canvas canvas, float left, float top, float boardSize, float cellSize) {
        rect.set(left, top, left + boardSize, top + boardSize);
        canvas.drawRoundRect(rect, 28f, 28f, gridPaint);

        for (int line = 1; line < GRID_SIZE; line++) {
            float x = left + line * cellSize;
            float y = top + line * cellSize;
            canvas.drawLine(x, top, x, top + boardSize, gridPaint);
            canvas.drawLine(left, y, left + boardSize, y, gridPaint);
        }
    }

    private void drawBlock(Canvas canvas, float left, float top, float cellSize) {
        float padding = cellSize * 0.12f;
        float blockLeft = left + blockColumn * cellSize + padding;
        float blockTop = top + blockRow * cellSize + padding;

        rect.set(blockLeft, blockTop, blockLeft + cellSize - padding * 2f, blockTop + cellSize - padding * 2f);
        canvas.drawRoundRect(rect, 22f, 22f, blockPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startX = event.getX();
            startY = event.getY();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            handleSwipe(event.getX() - startX, event.getY() - startY);
            return true;
        }

        return true;
    }

    private void handleSwipe(float deltaX, float deltaY) {
        if (Math.max(Math.abs(deltaX), Math.abs(deltaY)) < MIN_SWIPE_DISTANCE) {
            return;
        }

        int oldRow = blockRow;
        int oldColumn = blockColumn;

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            blockColumn += deltaX > 0 ? 1 : -1;
        } else {
            blockRow += deltaY > 0 ? 1 : -1;
        }

        blockRow = clamp(blockRow, 0, GRID_SIZE - 1);
        blockColumn = clamp(blockColumn, 0, GRID_SIZE - 1);

        if (blockRow != oldRow || blockColumn != oldColumn) {
            moveCount++;
            invalidate();
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
