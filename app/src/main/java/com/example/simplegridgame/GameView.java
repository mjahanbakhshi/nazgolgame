package com.example.simplegridgame;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends View {
    private static final int MAX_INGREDIENTS = 5;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint decorationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bottlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final List<Ingredient> selectedIngredients = new ArrayList<>();

    private final Ingredient[] ingredients = new Ingredient[] {
            new Ingredient("Rose", "floral", Color.rgb(255, 104, 154)),
            new Ingredient("Vanilla", "sweet", Color.rgb(255, 200, 92)),
            new Ingredient("Lemon", "fresh", Color.rgb(255, 230, 86)),
            new Ingredient("Mint", "fresh", Color.rgb(83, 220, 172)),
            new Ingredient("Lavender", "floral", Color.rgb(172, 122, 255)),
            new Ingredient("Candy", "sweet", Color.rgb(255, 135, 213))
    };

    private final RectF mixButton = new RectF();
    private final RectF resetButton = new RectF();

    private String perfumeName = "";
    private String perfumeDescription = "Tap ingredients to fill the bottle.";
    private int perfumeScore = 0;
    private boolean hasResult = false;

    private float bottleLeft;
    private float bottleTop;
    private float bottleRight;
    private float bottleBottom;

    public GameView(Context context) {
        super(context);

        backgroundPaint.setColor(Color.rgb(255, 244, 232));
        textPaint.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        updateLayout();
        drawBackground(canvas);
        drawHeader(canvas);
        drawBottle(canvas);
        drawIngredients(canvas);
        drawButtons(canvas);
        drawResult(canvas);
    }

    private void updateLayout() {
        float width = getWidth();
        float height = getHeight();
        float safeTop = getTopSafeInset();

        float bottleWidth = width * 0.38f;
        float bottleHeight = height * 0.34f;
        bottleLeft = (width - bottleWidth) / 2f;
        bottleTop = safeTop + 165f;
        bottleRight = bottleLeft + bottleWidth;
        bottleBottom = bottleTop + bottleHeight;

        float buttonTop = bottleBottom + 34f;
        mixButton.set(width * 0.10f, buttonTop, width * 0.48f, buttonTop + 82f);
        resetButton.set(width * 0.52f, buttonTop, width * 0.90f, buttonTop + 82f);

        layoutIngredients(width, height);
    }

    private void layoutIngredients(float width, float height) {
        float startY = Math.min(height - 440f, resetButton.bottom + 88f);
        float rowGap = 118f;
        float leftX = width * 0.25f;
        float rightX = width * 0.75f;

        for (int i = 0; i < ingredients.length; i++) {
            ingredients[i].x = i % 2 == 0 ? leftX : rightX;
            ingredients[i].y = startY + (i / 2) * rowGap;
            ingredients[i].radius = 46f;
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(backgroundPaint.getColor());

        decorationPaint.setStyle(Paint.Style.FILL);
        decorationPaint.setColor(Color.rgb(255, 211, 118));
        canvas.drawCircle(getWidth() * 0.10f, getHeight() * 0.13f, 56f, decorationPaint);

        decorationPaint.setColor(Color.rgb(163, 235, 255));
        canvas.drawCircle(getWidth() * 0.88f, getHeight() * 0.18f, 70f, decorationPaint);

        decorationPaint.setColor(Color.rgb(255, 185, 222));
        canvas.drawCircle(getWidth() * 0.12f, getHeight() * 0.84f, 72f, decorationPaint);

        decorationPaint.setColor(Color.rgb(190, 238, 138));
        canvas.drawCircle(getWidth() * 0.88f, getHeight() * 0.78f, 52f, decorationPaint);
    }

    private void drawHeader(Canvas canvas) {
        float safeTop = getTopSafeInset();

        decorationPaint.setColor(Color.WHITE);
        rect.set(getWidth() * 0.07f, safeTop + 22f, getWidth() * 0.93f, safeTop + 132f);
        canvas.drawRoundRect(rect, 46f, 46f, decorationPaint);

        textPaint.setColor(Color.rgb(118, 72, 172));
        textPaint.setTextSize(52f);
        canvas.drawText("Perfume Smell", getWidth() / 2f, safeTop + 88f, textPaint);

        decorationPaint.setColor(Color.rgb(255, 122, 170));
        canvas.drawCircle(getWidth() * 0.16f, safeTop + 72f, 16f, decorationPaint);

        decorationPaint.setColor(Color.rgb(74, 212, 184));
        canvas.drawCircle(getWidth() * 0.84f, safeTop + 84f, 20f, decorationPaint);
    }

    private void drawBottle(Canvas canvas) {
        float centerX = getWidth() / 2f;
        float neckWidth = (bottleRight - bottleLeft) * 0.34f;
        float neckLeft = centerX - neckWidth / 2f;
        float neckRight = centerX + neckWidth / 2f;

        decorationPaint.setColor(Color.WHITE);
        rect.set(neckLeft, bottleTop - 48f, neckRight, bottleTop + 18f);
        canvas.drawRoundRect(rect, 18f, 18f, decorationPaint);

        rect.set(bottleLeft, bottleTop, bottleRight, bottleBottom);
        canvas.drawRoundRect(rect, 48f, 48f, decorationPaint);

        bottlePaint.setStyle(Paint.Style.STROKE);
        bottlePaint.setStrokeWidth(8f);
        bottlePaint.setColor(Color.rgb(126, 82, 175));
        canvas.drawRoundRect(rect, 48f, 48f, bottlePaint);

        drawBottleFill(canvas);

        textPaint.setColor(Color.rgb(126, 82, 175));
        textPaint.setTextSize(30f);
        canvas.drawText(selectedIngredients.size() + "/" + MAX_INGREDIENTS, centerX, bottleBottom - 28f, textPaint);
    }

    private void drawBottleFill(Canvas canvas) {
        if (selectedIngredients.isEmpty()) {
            textPaint.setColor(Color.rgb(172, 143, 196));
            textPaint.setTextSize(28f);
            canvas.drawText("empty bottle", getWidth() / 2f, bottleTop + (bottleBottom - bottleTop) / 2f, textPaint);
            return;
        }

        float innerLeft = bottleLeft + 18f;
        float innerRight = bottleRight - 18f;
        float innerBottom = bottleBottom - 18f;
        float fillHeight = ((bottleBottom - bottleTop) - 42f) * selectedIngredients.size() / MAX_INGREDIENTS;
        float layerHeight = fillHeight / selectedIngredients.size();

        for (int i = 0; i < selectedIngredients.size(); i++) {
            Ingredient ingredient = selectedIngredients.get(i);
            float bottom = innerBottom - i * layerHeight;
            float top = bottom - layerHeight + 2f;

            decorationPaint.setColor(ingredient.color);
            rect.set(innerLeft, top, innerRight, bottom);
            canvas.drawRoundRect(rect, 24f, 24f, decorationPaint);
        }

        decorationPaint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawCircle(bottleLeft + 38f, bottleTop + 58f, 16f, decorationPaint);
    }

    private void drawIngredients(Canvas canvas) {
        textPaint.setColor(Color.rgb(104, 64, 158));
        textPaint.setTextSize(34f);
        canvas.drawText("Choose scents", getWidth() / 2f, resetButton.bottom + 48f, textPaint);

        for (Ingredient ingredient : ingredients) {
            drawIngredient(canvas, ingredient);
        }
    }

    private void drawIngredient(Canvas canvas, Ingredient ingredient) {
        decorationPaint.setColor(Color.WHITE);
        canvas.drawCircle(ingredient.x, ingredient.y + 5f, ingredient.radius + 10f, decorationPaint);

        decorationPaint.setColor(darken(ingredient.color));
        canvas.drawCircle(ingredient.x + 4f, ingredient.y + 7f, ingredient.radius, decorationPaint);

        decorationPaint.setColor(ingredient.color);
        canvas.drawCircle(ingredient.x, ingredient.y, ingredient.radius, decorationPaint);

        decorationPaint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawCircle(ingredient.x - 15f, ingredient.y - 16f, 10f, decorationPaint);

        textPaint.setColor(Color.rgb(86, 54, 130));
        textPaint.setTextSize(26f);
        canvas.drawText(ingredient.name, ingredient.x, ingredient.y + ingredient.radius + 32f, textPaint);
    }

    private void drawButtons(Canvas canvas) {
        drawButton(canvas, mixButton, "Mix", Color.rgb(255, 118, 137));
        drawButton(canvas, resetButton, "New", Color.rgb(74, 212, 184));
    }

    private void drawButton(Canvas canvas, RectF button, String label, int color) {
        decorationPaint.setColor(darken(color));
        rect.set(button.left + 5f, button.top + 7f, button.right + 5f, button.bottom + 7f);
        canvas.drawRoundRect(rect, 34f, 34f, decorationPaint);

        decorationPaint.setColor(color);
        canvas.drawRoundRect(button, 34f, 34f, decorationPaint);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(38f);
        canvas.drawText(label, button.centerX(), button.centerY() + 14f, textPaint);
    }

    private void drawResult(Canvas canvas) {
        float top = getHeight() - 152f;
        decorationPaint.setColor(Color.WHITE);
        rect.set(getWidth() * 0.07f, top, getWidth() * 0.93f, getHeight() - 34f);
        canvas.drawRoundRect(rect, 42f, 42f, decorationPaint);

        textPaint.setColor(Color.rgb(118, 72, 172));
        textPaint.setTextSize(hasResult ? 30f : 28f);
        canvas.drawText(hasResult ? perfumeName : "Make your own magic perfume", getWidth() / 2f, top + 42f, textPaint);

        textPaint.setColor(Color.rgb(255, 118, 137));
        textPaint.setTextSize(26f);
        canvas.drawText(hasResult ? "Score " + perfumeScore : perfumeDescription, getWidth() / 2f, top + 78f, textPaint);

        if (hasResult) {
            textPaint.setColor(Color.rgb(74, 150, 137));
            textPaint.setTextSize(24f);
            canvas.drawText(perfumeDescription, getWidth() / 2f, top + 108f, textPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        if (mixButton.contains(x, y)) {
            mixPerfume();
            invalidate();
            return true;
        }

        if (resetButton.contains(x, y)) {
            resetPerfume();
            invalidate();
            return true;
        }

        for (Ingredient ingredient : ingredients) {
            float dx = x - ingredient.x;
            float dy = y - ingredient.y;
            if (dx * dx + dy * dy <= (ingredient.radius + 28f) * (ingredient.radius + 28f)) {
                addIngredient(ingredient);
                invalidate();
                return true;
            }
        }

        return true;
    }

    private void addIngredient(Ingredient ingredient) {
        if (selectedIngredients.size() >= MAX_INGREDIENTS) {
            perfumeDescription = "The bottle is full. Mix it.";
            return;
        }

        hasResult = false;
        selectedIngredients.add(ingredient);
        perfumeDescription = ingredient.name + " added.";
    }

    private void mixPerfume() {
        if (selectedIngredients.isEmpty()) {
            perfumeDescription = "Pick at least one scent first.";
            return;
        }

        int sweet = 0;
        int fresh = 0;
        int floral = 0;
        int colorSparkle = 0;

        for (Ingredient ingredient : selectedIngredients) {
            if ("sweet".equals(ingredient.note)) {
                sweet++;
            } else if ("fresh".equals(ingredient.note)) {
                fresh++;
            } else {
                floral++;
            }
            colorSparkle += Color.red(ingredient.color) + Color.green(ingredient.color) + Color.blue(ingredient.color);
        }

        Ingredient first = selectedIngredients.get(0);
        Ingredient last = selectedIngredients.get(selectedIngredients.size() - 1);
        String[] endings = new String[] {"Dream", "Sparkle", "Cloud", "Wish", "Bloom"};

        perfumeName = first.name + " " + last.name + " " + endings[random.nextInt(endings.length)];
        perfumeScore = selectedIngredients.size() * 18 + Math.abs(sweet - fresh) * 6 + floral * 7 + colorSparkle % 31;
        perfumeDescription = describePerfume(sweet, fresh, floral);
        hasResult = true;
    }

    private String describePerfume(int sweet, int fresh, int floral) {
        if (sweet >= fresh && sweet >= floral) {
            return "Sweet, cozy, and candy-like.";
        }
        if (fresh >= sweet && fresh >= floral) {
            return "Fresh, bright, and splashy.";
        }
        return "Flowery, soft, and magical.";
    }

    private void resetPerfume() {
        selectedIngredients.clear();
        hasResult = false;
        perfumeName = "";
        perfumeScore = 0;
        perfumeDescription = "Tap ingredients to fill the bottle.";
    }

    private int darken(int color) {
        return Color.rgb(Color.red(color) * 3 / 4, Color.green(color) * 3 / 4, Color.blue(color) * 3 / 4);
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

    private static class Ingredient {
        final String name;
        final String note;
        final int color;
        float x;
        float y;
        float radius;

        Ingredient(String name, String note, int color) {
            this.name = name;
            this.note = note;
            this.color = color;
        }
    }
}
