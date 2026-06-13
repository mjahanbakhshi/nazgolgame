package com.example.simplegridgame;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

import java.util.Random;

public class GameView extends View {
    private static final int BOARD_SIZE = 8;
    private static final int PIECE_COUNT = 3;
    private static final int HIGH_SCORE_COUNT = 5;
    private static final String PREFS_NAME = "block_puzzle_scores";
    private static final String HIGH_SCORE_KEY_PREFIX = "high_score_";

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint decorationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final SharedPreferences preferences;
    private final int[] highScores = new int[HIGH_SCORE_COUNT];
    private final float touchPadding;

    private final boolean[][] board = new boolean[BOARD_SIZE][BOARD_SIZE];
    private final Piece[] pieces = new Piece[PIECE_COUNT];

    private int score = 0;
    private int draggedPieceIndex = -1;
    private float dragX;
    private float dragY;
    private boolean gameOver = false;
    private boolean scoreSaved = false;

    private float boardLeft;
    private float boardTop;
    private float boardCellSize;

    public GameView(Context context) {
        super(context);
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        touchPadding = 36f * getResources().getDisplayMetrics().density;

        backgroundPaint.setColor(Color.rgb(255, 246, 232));
        cellPaint.setColor(Color.rgb(255, 235, 245));

        gridPaint.setColor(Color.rgb(255, 146, 174));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(7f);

        ghostPaint.setColor(Color.argb(120, 66, 214, 164));
        ghostPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.rgb(92, 55, 132));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));

        loadHighScores();
        dealPieces();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        updateLayout();

        drawBackground(canvas);
        drawScore(canvas);
        drawBoard(canvas);
        drawPlacementPreview(canvas);
        drawTrayPieces(canvas);
        drawDraggedPiece(canvas);

        if (gameOver) {
            drawGameOver(canvas);
        }
    }

    private void updateLayout() {
        float width = getWidth();
        float height = getHeight();
        float safeTop = getTopSafeInset();
        float headerHeight = Math.max(150f, safeTop + 130f);
        float trayHeight = 250f;
        float boardSize = Math.min(width * 0.92f, height - headerHeight - trayHeight);

        boardCellSize = boardSize / BOARD_SIZE;
        boardLeft = (width - boardSize) / 2f;
        boardTop = headerHeight;
    }

    private void drawScore(Canvas canvas) {
        float safeTop = getTopSafeInset();

        float cardLeft = getWidth() * 0.08f;
        float cardTop = safeTop + 18f;
        float cardRight = getWidth() * 0.92f;
        float cardBottom = safeTop + 136f;
        decorationPaint.setStyle(Paint.Style.FILL);
        decorationPaint.setColor(Color.WHITE);
        rect.set(cardLeft, cardTop, cardRight, cardBottom);
        canvas.drawRoundRect(rect, 44f, 44f, decorationPaint);

        decorationPaint.setColor(Color.rgb(255, 210, 87));
        canvas.drawCircle(cardLeft + 42f, cardTop + 42f, 17f, decorationPaint);
        decorationPaint.setColor(Color.rgb(80, 220, 180));
        canvas.drawCircle(cardRight - 45f, cardBottom - 38f, 20f, decorationPaint);

        textPaint.setColor(Color.rgb(102, 61, 156));
        textPaint.setTextSize(62f);
        canvas.drawText("Score " + score, getWidth() / 2f, safeTop + 76f, textPaint);

        textPaint.setColor(Color.rgb(255, 118, 137));
        textPaint.setTextSize(32f);
        canvas.drawText("Best " + highScores[0], getWidth() / 2f, safeTop + 116f, textPaint);
    }

    private void drawBoard(Canvas canvas) {
        float padding = boardCellSize * 0.08f;
        float boardRight = boardLeft + boardCellSize * BOARD_SIZE;
        float boardBottom = boardTop + boardCellSize * BOARD_SIZE;

        decorationPaint.setStyle(Paint.Style.FILL);
        decorationPaint.setColor(Color.rgb(255, 255, 255));
        rect.set(boardLeft - 14f, boardTop - 14f, boardRight + 14f, boardBottom + 14f);
        canvas.drawRoundRect(rect, 36f, 36f, decorationPaint);

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                float left = boardLeft + column * boardCellSize + padding;
                float top = boardTop + row * boardCellSize + padding;
                rect.set(left, top, left + boardCellSize - padding * 2f, top + boardCellSize - padding * 2f);

                if (board[row][column]) {
                    cellPaint.setColor(Color.rgb(123, 97, 255));
                } else {
                    cellPaint.setColor((row + column) % 2 == 0 ? Color.rgb(255, 229, 239) : Color.rgb(235, 246, 255));
                }

                canvas.drawRoundRect(rect, 16f, 16f, cellPaint);
            }
        }

        rect.set(boardLeft, boardTop, boardRight, boardBottom);
        canvas.drawRoundRect(rect, 24f, 24f, gridPaint);
    }

    private void drawPlacementPreview(Canvas canvas) {
        if (draggedPieceIndex < 0) {
            return;
        }

        DropTarget target = getDropTarget(pieces[draggedPieceIndex]);
        if (!target.isValid) {
            return;
        }

        drawPiece(canvas, pieces[draggedPieceIndex], target.left, target.top, boardCellSize, ghostPaint, true);
    }

    private void drawTrayPieces(Canvas canvas) {
        float trayTop = boardTop + boardCellSize * BOARD_SIZE + 60f;
        float slotWidth = getWidth() / (float) PIECE_COUNT;
        float previewCellSize = Math.min(boardCellSize * 0.92f, slotWidth / 3.6f);

        for (int i = 0; i < PIECE_COUNT; i++) {
            Piece piece = pieces[i];
            if (piece.isUsed || i == draggedPieceIndex) {
                continue;
            }

            float pieceWidth = piece.getColumnCount() * previewCellSize;
            float pieceHeight = piece.getRowCount() * previewCellSize;
            float left = slotWidth * i + (slotWidth - pieceWidth) / 2f;
            float top = trayTop + (boardCellSize * 2.8f - pieceHeight) / 2f;

            decorationPaint.setColor(Color.WHITE);
            decorationPaint.setStyle(Paint.Style.FILL);
            rect.set(slotWidth * i + 12f, trayTop - 14f, slotWidth * (i + 1) - 12f, trayTop + boardCellSize * 2.8f + 14f);
            canvas.drawRoundRect(rect, 36f, 36f, decorationPaint);

            piece.previewLeft = left;
            piece.previewTop = top;
            piece.previewCellSize = previewCellSize;
            drawPiece(canvas, piece, left, top, previewCellSize, null, false);
        }
    }

    private void drawDraggedPiece(Canvas canvas) {
        if (draggedPieceIndex < 0) {
            return;
        }

        Piece piece = pieces[draggedPieceIndex];
        float left = dragX - piece.getColumnCount() * boardCellSize / 2f;
        float top = dragY - piece.getRowCount() * boardCellSize / 2f;
        drawPiece(canvas, piece, left, top, boardCellSize, null, false);
    }

    private void drawPiece(Canvas canvas, Piece piece, float left, float top, float cellSize, Paint overridePaint, boolean isPreview) {
        float padding = cellSize * 0.1f;

        for (int[] cell : piece.cells) {
            float cellLeft = left + cell[1] * cellSize + padding;
            float cellTop = top + cell[0] * cellSize + padding;
            rect.set(cellLeft, cellTop, cellLeft + cellSize - padding * 2f, cellTop + cellSize - padding * 2f);

            if (overridePaint == null) {
                cellPaint.setColor(piece.shadowColor);
                canvas.drawRoundRect(cellLeft + 4f, cellTop + 6f, rect.right + 4f, rect.bottom + 6f, 18f, 18f, cellPaint);
                cellPaint.setColor(piece.color);
                canvas.drawRoundRect(rect, 18f, 18f, cellPaint);

                cellPaint.setColor(piece.highlightColor);
                rect.set(cellLeft + cellSize * 0.12f, cellTop + cellSize * 0.12f, cellLeft + cellSize * 0.34f, cellTop + cellSize * 0.23f);
                canvas.drawRoundRect(rect, 10f, 10f, cellPaint);
            } else {
                canvas.drawRoundRect(rect, isPreview ? 12f : 18f, isPreview ? 12f : 18f, overridePaint);
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(backgroundPaint.getColor());

        decorationPaint.setStyle(Paint.Style.FILL);
        decorationPaint.setColor(Color.rgb(255, 214, 104));
        canvas.drawCircle(getWidth() * 0.16f, getHeight() * 0.12f, 44f, decorationPaint);

        decorationPaint.setColor(Color.rgb(130, 235, 255));
        canvas.drawCircle(getWidth() * 0.86f, getHeight() * 0.18f, 58f, decorationPaint);

        decorationPaint.setColor(Color.rgb(255, 175, 214));
        canvas.drawCircle(getWidth() * 0.10f, getHeight() * 0.78f, 64f, decorationPaint);

        decorationPaint.setColor(Color.rgb(178, 238, 128));
        canvas.drawCircle(getWidth() * 0.90f, getHeight() * 0.82f, 46f, decorationPaint);
    }

    private void drawGameOver(Canvas canvas) {
        Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.argb(205, 112, 83, 185));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), overlayPaint);

        decorationPaint.setStyle(Paint.Style.FILL);
        decorationPaint.setColor(Color.WHITE);
        rect.set(getWidth() * 0.10f, getHeight() / 2f - 180f, getWidth() * 0.90f, getHeight() / 2f + 270f);
        canvas.drawRoundRect(rect, 54f, 54f, decorationPaint);

        textPaint.setColor(Color.WHITE);
        decorationPaint.setColor(Color.rgb(255, 118, 137));
        rect.set(getWidth() * 0.18f, getHeight() / 2f - 155f, getWidth() * 0.82f, getHeight() / 2f - 75f);
        canvas.drawRoundRect(rect, 38f, 38f, decorationPaint);

        textPaint.setTextSize(56f);
        canvas.drawText("Game Over", getWidth() / 2f, getHeight() / 2f - 110f, textPaint);
        textPaint.setColor(Color.rgb(102, 61, 156));
        textPaint.setTextSize(36f);
        canvas.drawText("Top Scores", getWidth() / 2f, getHeight() / 2f - 45f, textPaint);

        textPaint.setTextSize(32f);
        for (int i = 0; i < HIGH_SCORE_COUNT; i++) {
            textPaint.setColor(i == 0 ? Color.rgb(255, 152, 0) : Color.rgb(102, 61, 156));
            canvas.drawText((i + 1) + ". " + highScores[i], getWidth() / 2f, getHeight() / 2f + 5f + i * 38f, textPaint);
        }

        textPaint.setColor(Color.rgb(0, 166, 148));
        textPaint.setTextSize(34f);
        canvas.drawText("Tap to restart", getWidth() / 2f, getHeight() / 2f + 225f, textPaint);
        textPaint.setColor(Color.rgb(92, 55, 132));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (gameOver) {
                    resetGame();
                    return true;
                }

                draggedPieceIndex = findTouchedPiece(event.getX(), event.getY());
                dragX = event.getX();
                dragY = event.getY();
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (draggedPieceIndex >= 0) {
                    dragX = event.getX();
                    dragY = event.getY();
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggedPieceIndex >= 0) {
                    tryPlaceDraggedPiece();
                    draggedPieceIndex = -1;
                    invalidate();
                }
                return true;
            default:
                return true;
        }
    }

    private int findTouchedPiece(float x, float y) {
        for (int i = 0; i < PIECE_COUNT; i++) {
            Piece piece = pieces[i];
            if (piece.isUsed) {
                continue;
            }

            float width = piece.getColumnCount() * piece.previewCellSize;
            float height = piece.getRowCount() * piece.previewCellSize;

            if (x >= piece.previewLeft - touchPadding
                    && x <= piece.previewLeft + width + touchPadding
                    && y >= piece.previewTop - touchPadding
                    && y <= piece.previewTop + height + touchPadding) {
                return i;
            }
        }

        return -1;
    }

    private void tryPlaceDraggedPiece() {
        Piece piece = pieces[draggedPieceIndex];
        DropTarget target = getDropTarget(piece);

        if (!target.isValid) {
            return;
        }

        for (int[] cell : piece.cells) {
            board[target.row + cell[0]][target.column + cell[1]] = true;
        }

        piece.isUsed = true;
        score += piece.cells.length;
        clearCompletedLines();

        if (allPiecesUsed()) {
            dealPieces();
        }

        gameOver = !hasAnyMove();
        if (gameOver) {
            saveScoreIfNeeded();
        }
    }

    private DropTarget getDropTarget(Piece piece) {
        float pieceWidth = piece.getColumnCount() * boardCellSize;
        float pieceHeight = piece.getRowCount() * boardCellSize;
        int column = Math.round((dragX - pieceWidth / 2f - boardLeft) / boardCellSize);
        int row = Math.round((dragY - pieceHeight / 2f - boardTop) / boardCellSize);

        float left = boardLeft + column * boardCellSize;
        float top = boardTop + row * boardCellSize;
        return new DropTarget(row, column, left, top, canPlace(piece, row, column));
    }

    private boolean canPlace(Piece piece, int row, int column) {
        for (int[] cell : piece.cells) {
            int boardRow = row + cell[0];
            int boardColumn = column + cell[1];

            if (boardRow < 0 || boardRow >= BOARD_SIZE || boardColumn < 0 || boardColumn >= BOARD_SIZE) {
                return false;
            }

            if (board[boardRow][boardColumn]) {
                return false;
            }
        }

        return true;
    }

    private void clearCompletedLines() {
        boolean[] rowsToClear = new boolean[BOARD_SIZE];
        boolean[] columnsToClear = new boolean[BOARD_SIZE];
        int clearCount = 0;

        for (int row = 0; row < BOARD_SIZE; row++) {
            boolean isFull = true;
            for (int column = 0; column < BOARD_SIZE; column++) {
                isFull = isFull && board[row][column];
            }
            rowsToClear[row] = isFull;
            if (isFull) {
                clearCount++;
            }
        }

        for (int column = 0; column < BOARD_SIZE; column++) {
            boolean isFull = true;
            for (int row = 0; row < BOARD_SIZE; row++) {
                isFull = isFull && board[row][column];
            }
            columnsToClear[column] = isFull;
            if (isFull) {
                clearCount++;
            }
        }

        if (clearCount == 0) {
            return;
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                if (rowsToClear[row] || columnsToClear[column]) {
                    board[row][column] = false;
                }
            }
        }

        score += clearCount * 10;
    }

    private boolean allPiecesUsed() {
        for (Piece piece : pieces) {
            if (!piece.isUsed) {
                return false;
            }
        }
        return true;
    }

    private boolean hasAnyMove() {
        for (Piece piece : pieces) {
            if (piece.isUsed) {
                continue;
            }

            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int column = 0; column < BOARD_SIZE; column++) {
                    if (canPlace(piece, row, column)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void resetGame() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                board[row][column] = false;
            }
        }

        score = 0;
        gameOver = false;
        scoreSaved = false;
        draggedPieceIndex = -1;
        dealPieces();
        invalidate();
    }

    private int getTopSafeInset() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return 0;
        }

        WindowInsets insets = getRootWindowInsets();
        if (insets == null) {
            return 0;
        }

        int safeTop = insets.getSystemWindowInsetTop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                safeTop = Math.max(safeTop, cutout.getSafeInsetTop());
            }
        }

        return safeTop;
    }

    private void loadHighScores() {
        for (int i = 0; i < HIGH_SCORE_COUNT; i++) {
            highScores[i] = preferences.getInt(HIGH_SCORE_KEY_PREFIX + i, 0);
        }
    }

    private void saveScoreIfNeeded() {
        if (scoreSaved) {
            return;
        }

        scoreSaved = true;

        for (int i = 0; i < HIGH_SCORE_COUNT; i++) {
            if (score > highScores[i]) {
                for (int j = HIGH_SCORE_COUNT - 1; j > i; j--) {
                    highScores[j] = highScores[j - 1];
                }
                highScores[i] = score;
                saveHighScores();
                return;
            }
        }
    }

    private void saveHighScores() {
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 0; i < HIGH_SCORE_COUNT; i++) {
            editor.putInt(HIGH_SCORE_KEY_PREFIX + i, highScores[i]);
        }
        editor.apply();
    }

    private void dealPieces() {
        for (int i = 0; i < PIECE_COUNT; i++) {
            pieces[i] = createRandomPiece();
        }
    }

    private Piece createRandomPiece() {
        int[][][] shapes = new int[][][] {
                {{0, 0}},
                {{0, 0}, {0, 1}},
                {{0, 0}, {1, 0}},
                {{0, 0}, {0, 1}, {0, 2}},
                {{0, 0}, {1, 0}, {2, 0}},
                {{0, 0}, {0, 1}, {1, 0}, {1, 1}},
                {{0, 0}, {1, 0}, {1, 1}},
                {{0, 1}, {1, 0}, {1, 1}},
                {{0, 0}, {0, 1}, {0, 2}, {1, 1}},
                {{0, 1}, {1, 1}, {2, 1}, {2, 0}},
                {{0, 0}, {0, 1}, {1, 1}, {1, 2}}
        };

        int[] colors = new int[] {
                Color.rgb(255, 99, 132),
                Color.rgb(54, 209, 220),
                Color.rgb(255, 193, 7),
                Color.rgb(171, 71, 188),
                Color.rgb(76, 217, 100),
                Color.rgb(255, 138, 76),
                Color.rgb(90, 133, 255)
        };

        return new Piece(shapes[random.nextInt(shapes.length)], colors[random.nextInt(colors.length)]);
    }

    private static class DropTarget {
        final int row;
        final int column;
        final float left;
        final float top;
        final boolean isValid;

        DropTarget(int row, int column, float left, float top, boolean isValid) {
            this.row = row;
            this.column = column;
            this.left = left;
            this.top = top;
            this.isValid = isValid;
        }
    }

    private class Piece {
        final int[][] cells;
        final int color;
        final int shadowColor;
        final int highlightColor;
        boolean isUsed = false;
        float previewLeft = 0f;
        float previewTop = 0f;
        float previewCellSize = 0f;

        Piece(int[][] cells, int color) {
            this.cells = cells;
            this.color = color;
            this.shadowColor = Color.argb(90, Color.red(color) / 2, Color.green(color) / 2, Color.blue(color) / 2);
            this.highlightColor = Color.argb(150, 255, 255, 255);
        }

        int getRowCount() {
            int max = 0;
            for (int[] cell : cells) {
                max = Math.max(max, cell[0]);
            }
            return max + 1;
        }

        int getColumnCount() {
            int max = 0;
            for (int[] cell : cells) {
                max = Math.max(max, cell[1]);
            }
            return max + 1;
        }
    }
}
