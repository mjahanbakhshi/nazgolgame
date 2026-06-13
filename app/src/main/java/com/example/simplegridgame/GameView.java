package com.example.simplegridgame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

public class GameView extends View {
    private static final int BOARD_SIZE = 8;
    private static final int PIECE_COUNT = 3;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();

    private final boolean[][] board = new boolean[BOARD_SIZE][BOARD_SIZE];
    private final Piece[] pieces = new Piece[PIECE_COUNT];

    private int score = 0;
    private int draggedPieceIndex = -1;
    private float dragX;
    private float dragY;
    private boolean gameOver = false;

    private float boardLeft;
    private float boardTop;
    private float boardCellSize;

    public GameView(Context context) {
        super(context);

        backgroundPaint.setColor(Color.rgb(242, 246, 252));
        cellPaint.setColor(Color.rgb(230, 236, 246));

        gridPaint.setColor(Color.rgb(33, 43, 66));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(3f);

        ghostPaint.setColor(Color.argb(90, 48, 128, 255));
        ghostPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.rgb(25, 30, 40));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        dealPieces();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(backgroundPaint.getColor());

        updateLayout();

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
        float boardSize = Math.min(width * 0.92f, height * 0.58f);

        boardCellSize = boardSize / BOARD_SIZE;
        boardLeft = (width - boardSize) / 2f;
        boardTop = Math.max(120f, height * 0.16f);
    }

    private void drawScore(Canvas canvas) {
        textPaint.setTextSize(54f);
        canvas.drawText("Score: " + score, getWidth() / 2f, 82f, textPaint);
    }

    private void drawBoard(Canvas canvas) {
        float padding = boardCellSize * 0.08f;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int column = 0; column < BOARD_SIZE; column++) {
                float left = boardLeft + column * boardCellSize + padding;
                float top = boardTop + row * boardCellSize + padding;
                rect.set(left, top, left + boardCellSize - padding * 2f, top + boardCellSize - padding * 2f);

                if (board[row][column]) {
                    cellPaint.setColor(Color.rgb(63, 81, 181));
                } else {
                    cellPaint.setColor(Color.rgb(222, 229, 241));
                }

                canvas.drawRoundRect(rect, 10f, 10f, cellPaint);
            }
        }

        rect.set(boardLeft, boardTop, boardLeft + boardCellSize * BOARD_SIZE, boardTop + boardCellSize * BOARD_SIZE);
        canvas.drawRoundRect(rect, 18f, 18f, gridPaint);
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
        float previewCellSize = Math.min(boardCellSize * 0.76f, slotWidth / 4.2f);

        for (int i = 0; i < PIECE_COUNT; i++) {
            Piece piece = pieces[i];
            if (piece.isUsed || i == draggedPieceIndex) {
                continue;
            }

            float pieceWidth = piece.getColumnCount() * previewCellSize;
            float pieceHeight = piece.getRowCount() * previewCellSize;
            float left = slotWidth * i + (slotWidth - pieceWidth) / 2f;
            float top = trayTop + (boardCellSize * 2.8f - pieceHeight) / 2f;

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
        cellPaint.setColor(piece.color);

        for (int[] cell : piece.cells) {
            float cellLeft = left + cell[1] * cellSize + padding;
            float cellTop = top + cell[0] * cellSize + padding;
            rect.set(cellLeft, cellTop, cellLeft + cellSize - padding * 2f, cellTop + cellSize - padding * 2f);
            canvas.drawRoundRect(rect, isPreview ? 8f : 12f, isPreview ? 8f : 12f, overridePaint == null ? cellPaint : overridePaint);
        }
    }

    private void drawGameOver(Canvas canvas) {
        Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setColor(Color.argb(185, 10, 18, 30));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), overlayPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(68f);
        canvas.drawText("Game Over", getWidth() / 2f, getHeight() / 2f - 30f, textPaint);
        textPaint.setTextSize(36f);
        canvas.drawText("Tap to restart", getWidth() / 2f, getHeight() / 2f + 35f, textPaint);
        textPaint.setColor(Color.rgb(25, 30, 40));
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

            if (x >= piece.previewLeft && x <= piece.previewLeft + width && y >= piece.previewTop && y <= piece.previewTop + height) {
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
        draggedPieceIndex = -1;
        dealPieces();
        invalidate();
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
                Color.rgb(63, 81, 181),
                Color.rgb(0, 150, 136),
                Color.rgb(255, 152, 0),
                Color.rgb(233, 30, 99),
                Color.rgb(76, 175, 80),
                Color.rgb(156, 39, 176)
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
        boolean isUsed = false;
        float previewLeft = 0f;
        float previewTop = 0f;
        float previewCellSize = 0f;

        Piece(int[][] cells, int color) {
            this.cells = cells;
            this.color = color;
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
