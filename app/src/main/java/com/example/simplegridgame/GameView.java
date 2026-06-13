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
    private enum Screen {
        START,
        CUSTOMER,
        INGREDIENTS,
        CRAFT,
        BOTTLE,
        DECORATE,
        RESULT
    }

    private static final int MAX_MATERIALS = 4;
    private static final float DESIGN_WIDTH = 390f;
    private static final float DESIGN_HEIGHT = 820f;

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Random random = new Random();
    private final List<HitArea> hitAreas = new ArrayList<>();
    private final List<Material> selectedMaterials = new ArrayList<>();

    private final String[] categories = new String[] {"Flowers", "Oils", "Fresh", "Sweet"};
    private final Material[] materials = new Material[] {
            new Material("Rose Petals", "Flowers", "floral", Color.rgb(255, 112, 166)),
            new Material("Lavender", "Flowers", "calm", Color.rgb(170, 132, 255)),
            new Material("Jasmine", "Flowers", "luxury", Color.rgb(255, 236, 170)),
            new Material("Orange Blossom", "Flowers", "citrus", Color.rgb(255, 178, 91)),
            new Material("Sandalwood Oil", "Oils", "warm", Color.rgb(194, 130, 78)),
            new Material("Amber Oil", "Oils", "luxury", Color.rgb(219, 154, 75)),
            new Material("Vanilla Oil", "Oils", "sweet", Color.rgb(255, 207, 111)),
            new Material("Cedar Oil", "Oils", "warm", Color.rgb(143, 93, 61)),
            new Material("Lemon Peel", "Fresh", "citrus", Color.rgb(255, 232, 82)),
            new Material("Mint Leaves", "Fresh", "fresh", Color.rgb(92, 221, 172)),
            new Material("Sea Mist", "Fresh", "fresh", Color.rgb(110, 217, 245)),
            new Material("Green Tea", "Fresh", "calm", Color.rgb(140, 213, 122)),
            new Material("Honey Drop", "Sweet", "sweet", Color.rgb(255, 190, 73)),
            new Material("Candy Sugar", "Sweet", "sweet", Color.rgb(255, 132, 215)),
            new Material("Cocoa Dust", "Sweet", "warm", Color.rgb(151, 94, 72)),
            new Material("Peach Nectar", "Sweet", "citrus", Color.rgb(255, 166, 134))
    };

    private final BottleOption[] bottleOptions = new BottleOption[] {
            new BottleOption("Crystal Tall", Color.rgb(163, 225, 255), 0),
            new BottleOption("Pink Round", Color.rgb(255, 176, 210), 1),
            new BottleOption("Gold Square", Color.rgb(255, 214, 117), 2)
    };

    private final String[] decorations = new String[] {"Ribbon", "Gold Cap", "Flower Charm"};
    private final String[] boxes = new String[] {"Velvet Box", "Cloud Box", "Royal Box"};

    private Screen screen = Screen.START;
    private CustomerRequest request;
    private String selectedCategory = "Flowers";
    private String statusText = "Welcome to your perfume shop.";
    private int selectedBottle = 0;
    private int selectedDecoration = 0;
    private int selectedBox = 0;
    private boolean alcoholAdded = false;
    private boolean pounded = false;
    private boolean strained = false;
    private boolean funnelPlaced = false;
    private boolean poured = false;
    private boolean bottleClosed = false;
    private PerfumeResult result;
    private long animationStartMs = 0L;
    private String activeCraftAnimation = "";
    private float contentScale = 1f;

    public GameView(Context context) {
        super(context);
        backgroundPaint.setColor(Color.rgb(255, 246, 236));
        textPaint.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(6f);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        hitAreas.clear();
        contentScale = calculateContentScale();
        canvas.save();
        canvas.scale(contentScale, contentScale);
        try {
            drawBackground(canvas);
            switch (screen) {
                case START:
                    drawStart(canvas);
                    break;
                case CUSTOMER:
                    drawCustomer(canvas);
                    break;
                case INGREDIENTS:
                    drawIngredients(canvas);
                    break;
                case CRAFT:
                    drawCraft(canvas);
                    break;
                case BOTTLE:
                    drawBottle(canvas);
                    break;
                case DECORATE:
                    drawDecorate(canvas);
                    break;
                case RESULT:
                    drawResult(canvas);
                    break;
            }
        } finally {
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        float x = event.getX() / contentScale;
        float y = event.getY() / contentScale;
        for (int i = hitAreas.size() - 1; i >= 0; i--) {
            HitArea hitArea = hitAreas.get(i);
            if (hitArea.rect.contains(x, y)) {
                handleAction(hitArea.action, hitArea.index);
                invalidate();
                return true;
            }
        }
        return true;
    }

    private void handleAction(String action, int index) {
        if ("start".equals(action)) {
            startNewCustomer();
        } else if ("inactive".equals(action)) {
            statusText = "Coming later.";
        } else if ("toIngredients".equals(action)) {
            screen = Screen.INGREDIENTS;
            statusText = "Choose up to " + MAX_MATERIALS + " materials.";
        } else if ("category".equals(action)) {
            selectedCategory = categories[index];
        } else if ("material".equals(action)) {
            addMaterial(index);
        } else if ("removeMaterial".equals(action)) {
            if (index >= 0 && index < selectedMaterials.size()) {
                selectedMaterials.remove(index);
                statusText = "Material removed.";
            }
        } else if ("toCraft".equals(action)) {
            if (selectedMaterials.isEmpty()) {
                statusText = "Pick at least one material first.";
            } else {
                screen = Screen.CRAFT;
                statusText = "Put everything into the bowl.";
            }
        } else if ("addAlcohol".equals(action)) {
            alcoholAdded = true;
            statusText = "Alcohol base added.";
            startCraftAnimation("alcohol");
        } else if ("pound".equals(action)) {
            if (!alcoholAdded) {
                statusText = "Add alcohol first.";
            } else {
                pounded = true;
                statusText = "Materials released their scent.";
                startCraftAnimation("pound");
            }
        } else if ("strain".equals(action)) {
            if (!pounded) {
                statusText = "Pound the materials first.";
            } else {
                strained = true;
                statusText = "The liquid is clear and pretty.";
                startCraftAnimation("strain");
            }
        } else if ("toBottle".equals(action)) {
            if (!strained) {
                statusText = "Strain the perfume first.";
            } else {
                screen = Screen.BOTTLE;
                statusText = "Choose a bottle and pour carefully.";
            }
        } else if ("bottle".equals(action)) {
            selectedBottle = index;
        } else if ("funnel".equals(action)) {
            funnelPlaced = true;
            statusText = "Funnel is ready.";
        } else if ("pour".equals(action)) {
            if (!funnelPlaced) {
                statusText = "Place the funnel first.";
            } else {
                poured = true;
                statusText = "Perfume poured into the bottle.";
            }
        } else if ("close".equals(action)) {
            if (!poured) {
                statusText = "Pour the perfume first.";
            } else {
                bottleClosed = true;
                statusText = "Bottle closed.";
            }
        } else if ("toDecorate".equals(action)) {
            if (!bottleClosed) {
                statusText = "Close the bottle first.";
            } else {
                screen = Screen.DECORATE;
                statusText = "Pick a decoration and a box.";
            }
        } else if ("decoration".equals(action)) {
            selectedDecoration = index;
        } else if ("box".equals(action)) {
            selectedBox = index;
        } else if ("finish".equals(action)) {
            result = scorePerfume();
            screen = Screen.RESULT;
        } else if ("playAgain".equals(action)) {
            startNewCustomer();
        }
    }

    private void startNewCustomer() {
        request = createRequest();
        selectedMaterials.clear();
        selectedCategory = "Flowers";
        selectedBottle = 0;
        selectedDecoration = 0;
        selectedBox = 0;
        alcoholAdded = false;
        pounded = false;
        strained = false;
        funnelPlaced = false;
        poured = false;
        bottleClosed = false;
        result = null;
        activeCraftAnimation = "";
        animationStartMs = 0L;
        screen = Screen.CUSTOMER;
        statusText = "A new customer entered the shop.";
    }

    private void startCraftAnimation(String name) {
        activeCraftAnimation = name;
        animationStartMs = System.currentTimeMillis();
    }

    private CustomerRequest createRequest() {
        CustomerRequest[] requests = new CustomerRequest[] {
                new CustomerRequest("Mina", "floral", "calm", "I want something floral and calm."),
                new CustomerRequest("Sara", "sweet", "luxury", "Please make it sweet but fancy."),
                new CustomerRequest("Nora", "fresh", "citrus", "I want a fresh sunny perfume."),
                new CustomerRequest("Lili", "warm", "sweet", "Can you make a cozy warm scent?"),
                new CustomerRequest("Ava", "luxury", "floral", "I need an elegant flower perfume.")
        };
        return requests[random.nextInt(requests.length)];
    }

    private void addMaterial(int materialIndex) {
        Material material = filteredMaterials().get(materialIndex);
        if (selectedMaterials.size() >= MAX_MATERIALS) {
            statusText = "The bowl is full. Continue to crafting.";
            return;
        }
        selectedMaterials.add(material);
        statusText = material.name + " added.";
    }

    private List<Material> filteredMaterials() {
        List<Material> filtered = new ArrayList<>();
        for (Material material : materials) {
            if (selectedCategory.equals(material.category)) {
                filtered.add(material);
            }
        }
        return filtered;
    }

    private PerfumeResult scorePerfume() {
        int match = 0;
        for (Material material : selectedMaterials) {
            if (material.tag.equals(request.primaryTag)) {
                match += 28;
            }
            if (material.tag.equals(request.secondaryTag)) {
                match += 18;
            }
        }

        int craft = 0;
        if (alcoholAdded) craft += 12;
        if (pounded) craft += 14;
        if (strained) craft += 16;
        if (funnelPlaced) craft += 8;
        if (poured) craft += 10;
        if (bottleClosed) craft += 10;

        int style = 12 + selectedBottle * 4 + selectedDecoration * 5 + selectedBox * 5;
        int total = Math.min(100, match + craft + style);

        int roll = random.nextInt(100);
        String reaction;
        String customerLine;
        if (total >= 88 || roll < 20) {
            reaction = "Super Happy";
            customerLine = "This is beautiful. I love it.";
            total = Math.max(total, 92);
        } else if (total >= 55 || roll < 90) {
            reaction = "Happy";
            customerLine = "Thank you. This smells very nice.";
            total = Math.max(total, 65);
        } else {
            reaction = "Not Satisfied";
            customerLine = "Hmm, it is not exactly what I imagined.";
            total = Math.min(total, 54);
        }

        String name = selectedMaterials.isEmpty()
                ? "Empty Bottle"
                : selectedMaterials.get(0).shortName() + " " + bottleOptions[selectedBottle].label + " No. " + (20 + random.nextInt(80));
        return new PerfumeResult(name, total, reaction, customerLine);
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(backgroundPaint.getColor());
        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(Color.rgb(255, 225, 175));
        canvas.drawCircle(viewWidth() * 0.12f, viewHeight() * 0.12f, 70f, shapePaint);
        shapePaint.setColor(Color.rgb(224, 245, 255));
        canvas.drawCircle(viewWidth() * 0.86f, viewHeight() * 0.18f, 84f, shapePaint);
        shapePaint.setColor(Color.rgb(255, 211, 230));
        canvas.drawCircle(viewWidth() * 0.18f, viewHeight() * 0.86f, 92f, shapePaint);
        drawHeader(canvas);
    }

    private void drawHeader(Canvas canvas) {
        float top = safeTop() + 18f;
        drawCard(canvas, viewWidth() * 0.07f, top, viewWidth() * 0.93f, top + 92f, Color.WHITE);
        drawText(canvas, "Perfume Smell", viewWidth() / 2f, top + 58f, 44f, Color.rgb(104, 64, 158));
    }

    private void drawStart(Canvas canvas) {
        float centerY = viewHeight() * 0.36f;
        drawText(canvas, "Luxury Little Shop", viewWidth() / 2f, centerY - 30f, 34f, Color.rgb(188, 107, 143));
        drawPerfumeBottle(canvas, viewWidth() / 2f - 70f, centerY + 10f, 140f, 190f, Color.rgb(255, 180, 214), 1);
        drawButton(canvas, "Start", "start", 0, viewWidth() * 0.18f, viewHeight() * 0.66f, viewWidth() * 0.82f, viewHeight() * 0.66f + 82f, Color.rgb(255, 122, 170));
        drawButton(canvas, "Shop", "inactive", 0, viewWidth() * 0.18f, viewHeight() * 0.77f, viewWidth() * 0.48f, viewHeight() * 0.77f + 70f, Color.rgb(197, 173, 222));
        drawButton(canvas, "Settings", "inactive", 0, viewWidth() * 0.52f, viewHeight() * 0.77f, viewWidth() * 0.82f, viewHeight() * 0.77f + 70f, Color.rgb(197, 173, 222));
        drawText(canvas, statusText, viewWidth() / 2f, viewHeight() - 64f, 26f, Color.rgb(110, 78, 130));
    }

    private void drawCustomer(Canvas canvas) {
        drawCustomerPerson(canvas, viewWidth() / 2f, viewHeight() * 0.47f, result == null ? "neutral" : result.reaction);
        drawSpeechBubble(canvas, request.requestText, viewWidth() * 0.10f, safeTop() + 130f, viewWidth() * 0.90f, safeTop() + 255f);
        drawText(canvas, request.name + " is waiting", viewWidth() / 2f, viewHeight() * 0.67f, 30f, Color.rgb(104, 64, 158));
        drawButton(canvas, "Create Perfume", "toIngredients", 0, viewWidth() * 0.13f, viewHeight() * 0.76f, viewWidth() * 0.87f, viewHeight() * 0.76f + 82f, Color.rgb(255, 122, 170));
        drawText(canvas, statusText, viewWidth() / 2f, viewHeight() - 48f, 24f, Color.rgb(110, 78, 130));
    }

    private void drawIngredients(Canvas canvas) {
        drawText(canvas, "Pick materials", viewWidth() / 2f, safeTop() + 150f, 34f, Color.rgb(104, 64, 158));
        drawSelectedMaterials(canvas, safeTop() + 174f);

        float tabTop = safeTop() + 270f;
        float tabWidth = viewWidth() / categories.length;
        for (int i = 0; i < categories.length; i++) {
            int color = categories[i].equals(selectedCategory) ? Color.rgb(255, 122, 170) : Color.WHITE;
            drawButton(canvas, categories[i], "category", i, i * tabWidth + 8f, tabTop, (i + 1) * tabWidth - 8f, tabTop + 58f, color);
        }

        List<Material> shown = filteredMaterials();
        float rowTop = tabTop + 92f;
        float cardWidth = viewWidth() * 0.42f;
        for (int i = 0; i < shown.size(); i++) {
            Material material = shown.get(i);
            float left = i % 2 == 0 ? viewWidth() * 0.06f : viewWidth() * 0.52f;
            float top = rowTop + (i / 2) * 112f;
            drawMaterialCard(canvas, material, left, top, left + cardWidth, top + 86f, i);
        }

        drawButton(canvas, "To Bowl", "toCraft", 0, viewWidth() * 0.18f, viewHeight() - 118f, viewWidth() * 0.82f, viewHeight() - 42f, Color.rgb(74, 212, 184));
        drawText(canvas, statusText, viewWidth() / 2f, viewHeight() - 140f, 23f, Color.rgb(110, 78, 130));
    }

    private void drawCraft(Canvas canvas) {
        drawText(canvas, "Craft the scent", viewWidth() / 2f, safeTop() + 150f, 34f, Color.rgb(104, 64, 158));
        drawBowl(canvas, viewWidth() / 2f, viewHeight() * 0.36f);
        drawStepPill(canvas, "Alcohol", alcoholAdded, viewWidth() * 0.20f, viewHeight() * 0.53f);
        drawStepPill(canvas, "Pounded", pounded, viewWidth() * 0.50f, viewHeight() * 0.53f);
        drawStepPill(canvas, "Strained", strained, viewWidth() * 0.80f, viewHeight() * 0.53f);
        drawButton(canvas, "Add Alcohol", "addAlcohol", 0, viewWidth() * 0.10f, viewHeight() * 0.62f, viewWidth() * 0.90f, viewHeight() * 0.62f + 68f, Color.rgb(114, 209, 238));
        drawButton(canvas, "Pound Materials", "pound", 0, viewWidth() * 0.10f, viewHeight() * 0.72f, viewWidth() * 0.90f, viewHeight() * 0.72f + 68f, Color.rgb(255, 185, 94));
        drawButton(canvas, "Strain Liquid", "strain", 0, viewWidth() * 0.10f, viewHeight() * 0.82f, viewWidth() * 0.90f, viewHeight() * 0.82f + 68f, Color.rgb(190, 145, 255));
        drawButton(canvas, "Choose Bottle", "toBottle", 0, viewWidth() * 0.18f, viewHeight() - 90f, viewWidth() * 0.82f, viewHeight() - 26f, Color.rgb(255, 122, 170));
        drawText(canvas, statusText, viewWidth() / 2f, viewHeight() * 0.59f, 23f, Color.rgb(110, 78, 130));
    }

    private void drawBottle(Canvas canvas) {
        drawText(canvas, "Bottle the perfume", viewWidth() / 2f, safeTop() + 145f, 34f, Color.rgb(104, 64, 158));
        float top = safeTop() + 188f;
        for (int i = 0; i < bottleOptions.length; i++) {
            float left = viewWidth() * (0.10f + i * 0.30f);
            BottleOption option = bottleOptions[i];
            drawCard(canvas, left, top, left + viewWidth() * 0.22f, top + 145f, i == selectedBottle ? Color.rgb(255, 240, 250) : Color.WHITE);
            drawPerfumeBottle(canvas, left + 25f, top + 24f, viewWidth() * 0.12f, 76f, option.color, option.shape);
            drawText(canvas, option.label, left + viewWidth() * 0.11f, top + 124f, 20f, Color.rgb(104, 64, 158));
            addHit(left, top, left + viewWidth() * 0.22f, top + 145f, "bottle", i);
        }

        drawPerfumeBottle(canvas, viewWidth() / 2f - 75f, viewHeight() * 0.46f, 150f, 190f, bottleOptions[selectedBottle].color, bottleOptions[selectedBottle].shape);
        if (funnelPlaced) {
            drawFunnel(canvas, viewWidth() / 2f, viewHeight() * 0.43f);
        }
        drawButton(canvas, "Funnel", "funnel", 0, viewWidth() * 0.08f, viewHeight() * 0.72f, viewWidth() * 0.36f, viewHeight() * 0.72f + 64f, Color.rgb(114, 209, 238));
        drawButton(canvas, "Pour", "pour", 0, viewWidth() * 0.38f, viewHeight() * 0.72f, viewWidth() * 0.64f, viewHeight() * 0.72f + 64f, Color.rgb(255, 185, 94));
        drawButton(canvas, "Close", "close", 0, viewWidth() * 0.66f, viewHeight() * 0.72f, viewWidth() * 0.92f, viewHeight() * 0.72f + 64f, Color.rgb(190, 145, 255));
        drawButton(canvas, "Decorate", "toDecorate", 0, viewWidth() * 0.18f, viewHeight() - 92f, viewWidth() * 0.82f, viewHeight() - 28f, Color.rgb(255, 122, 170));
        drawText(canvas, statusText, viewWidth() / 2f, viewHeight() * 0.68f, 23f, Color.rgb(110, 78, 130));
    }

    private void drawDecorate(Canvas canvas) {
        drawText(canvas, "Decorate and box", viewWidth() / 2f, safeTop() + 145f, 34f, Color.rgb(104, 64, 158));
        drawPerfumeBottle(canvas, viewWidth() / 2f - 70f, safeTop() + 190f, 140f, 170f, bottleOptions[selectedBottle].color, bottleOptions[selectedBottle].shape);
        drawText(canvas, decorations[selectedDecoration], viewWidth() / 2f, safeTop() + 395f, 26f, Color.rgb(188, 107, 143));
        drawOptionRow(canvas, "Decoration", decorations, "decoration", selectedDecoration, viewHeight() * 0.53f);
        drawOptionRow(canvas, "Box", boxes, "box", selectedBox, viewHeight() * 0.70f);
        drawButton(canvas, "Hand to Customer", "finish", 0, viewWidth() * 0.13f, viewHeight() - 96f, viewWidth() * 0.87f, viewHeight() - 28f, Color.rgb(255, 122, 170));
    }

    private void drawResult(Canvas canvas) {
        if (result == null) {
            result = scorePerfume();
        }
        drawCustomerPerson(canvas, viewWidth() / 2f, viewHeight() * 0.32f, result.reaction);
        drawSpeechBubble(canvas, result.customerLine, viewWidth() * 0.10f, viewHeight() * 0.48f, viewWidth() * 0.90f, viewHeight() * 0.60f);
        drawCard(canvas, viewWidth() * 0.10f, viewHeight() * 0.64f, viewWidth() * 0.90f, viewHeight() * 0.82f, Color.WHITE);
        drawText(canvas, result.perfumeName, viewWidth() / 2f, viewHeight() * 0.69f, 30f, Color.rgb(104, 64, 158));
        drawText(canvas, result.reaction + "  Score " + result.score, viewWidth() / 2f, viewHeight() * 0.75f, 30f, Color.rgb(255, 122, 170));
        drawButton(canvas, "Play Again", "playAgain", 0, viewWidth() * 0.18f, viewHeight() - 104f, viewWidth() * 0.82f, viewHeight() - 32f, Color.rgb(74, 212, 184));
    }

    private void drawSelectedMaterials(Canvas canvas, float top) {
        drawText(canvas, "Bowl: " + selectedMaterials.size() + "/" + MAX_MATERIALS, viewWidth() / 2f, top + 28f, 25f, Color.rgb(110, 78, 130));
        float chipTop = top + 45f;
        float chipWidth = viewWidth() * 0.21f;
        for (int i = 0; i < selectedMaterials.size(); i++) {
            float left = viewWidth() * 0.05f + i * (chipWidth + 8f);
            Material material = selectedMaterials.get(i);
            drawCard(canvas, left, chipTop, left + chipWidth, chipTop + 44f, material.color);
            drawText(canvas, material.shortName(), left + chipWidth / 2f, chipTop + 30f, 18f, Color.WHITE);
            addHit(left, chipTop, left + chipWidth, chipTop + 44f, "removeMaterial", i);
        }
    }

    private void drawMaterialCard(Canvas canvas, Material material, float left, float top, float right, float bottom, int index) {
        drawCard(canvas, left, top, right, bottom, Color.WHITE);
        shapePaint.setColor(material.color);
        canvas.drawCircle(left + 34f, top + 43f, 24f, shapePaint);
        drawText(canvas, material.name, (left + right) / 2f + 22f, top + 36f, 21f, Color.rgb(86, 54, 130));
        drawText(canvas, material.tag, (left + right) / 2f + 22f, top + 64f, 18f, Color.rgb(188, 107, 143));
        addHit(left, top, right, bottom, "material", index);
    }

    private void drawOptionRow(Canvas canvas, String title, String[] options, String action, int selected, float top) {
        drawText(canvas, title, viewWidth() / 2f, top - 18f, 27f, Color.rgb(104, 64, 158));
        float width = viewWidth() / options.length;
        for (int i = 0; i < options.length; i++) {
            int color = i == selected ? Color.rgb(255, 240, 250) : Color.WHITE;
            drawButton(canvas, options[i], action, i, i * width + 8f, top, (i + 1) * width - 8f, top + 70f, color);
        }
    }

    private void drawCustomerPerson(Canvas canvas, float cx, float cy, String mood) {
        int shirt = "Super Happy".equals(mood) ? Color.rgb(255, 214, 117) : "Not Satisfied".equals(mood) ? Color.rgb(190, 190, 205) : Color.rgb(255, 176, 210);
        shapePaint.setColor(shirt);
        rect.set(cx - 70f, cy + 45f, cx + 70f, cy + 190f);
        canvas.drawRoundRect(rect, 44f, 44f, shapePaint);
        shapePaint.setColor(Color.rgb(250, 205, 172));
        canvas.drawCircle(cx, cy, 58f, shapePaint);
        shapePaint.setColor(Color.rgb(92, 55, 132));
        canvas.drawCircle(cx - 20f, cy - 10f, 6f, shapePaint);
        canvas.drawCircle(cx + 20f, cy - 10f, 6f, shapePaint);
        linePaint.setColor(Color.rgb(92, 55, 132));
        if ("Not Satisfied".equals(mood)) {
            canvas.drawLine(cx - 24f, cy + 26f, cx + 24f, cy + 18f, linePaint);
        } else {
            rect.set(cx - 28f, cy + 8f, cx + 28f, cy + 42f);
            canvas.drawArc(rect, 10f, 160f, false, linePaint);
        }
        shapePaint.setColor(Color.rgb(92, 55, 132));
        rect.set(cx - 55f, cy - 64f, cx + 55f, cy - 30f);
        canvas.drawRoundRect(rect, 24f, 24f, shapePaint);
    }

    private void drawSpeechBubble(Canvas canvas, String text, float left, float top, float right, float bottom) {
        drawCard(canvas, left, top, right, bottom, Color.WHITE);
        drawText(canvas, text, (left + right) / 2f, top + (bottom - top) / 2f + 10f, 27f, Color.rgb(86, 54, 130));
    }

    private void drawBowl(Canvas canvas, float cx, float cy) {
        float elapsed = animationStartMs == 0L ? 9999f : (System.currentTimeMillis() - animationStartMs) / 1000f;
        boolean alcoholAnimating = "alcohol".equals(activeCraftAnimation) && elapsed < 1.7f;
        boolean poundAnimating = "pound".equals(activeCraftAnimation) && elapsed < 2.2f;
        boolean strainAnimating = "strain".equals(activeCraftAnimation) && elapsed < 1.4f;

        if (screen == Screen.CRAFT && (alcoholAnimating || poundAnimating || strainAnimating)) {
            postInvalidateOnAnimation();
        } else if (elapsed >= 2.2f) {
            activeCraftAnimation = "";
        }

        float shake = poundAnimating ? (float) Math.sin(elapsed * 38f) * 10f : 0f;
        float bob = poundAnimating ? (float) Math.abs(Math.sin(elapsed * 18f)) * 16f : 0f;
        cx += shake;

        canvas.save();
        canvas.scale(1.18f, 1.18f, cx, cy + 42f);
        try {
        drawCraftSparkles(canvas, cx, cy, elapsed, alcoholAnimating || poundAnimating || strainAnimating);

        shapePaint.setColor(Color.WHITE);
        rect.set(cx - 140f, cy - 35f, cx + 140f, cy + 115f);
        canvas.drawRoundRect(rect, 70f, 70f, shapePaint);

        shapePaint.setColor(Color.rgb(255, 237, 246));
        rect.set(cx - 118f, cy + 16f, cx + 118f, cy + 92f);
        canvas.drawRoundRect(rect, 38f, 38f, shapePaint);

        if (alcoholAdded) {
            int liquidColor = blendSelectedMaterialColors(Color.rgb(132, 219, 242));
            int alpha = strained ? 170 : pounded ? 145 : 120;
            shapePaint.setColor(Color.argb(alpha, Color.red(liquidColor), Color.green(liquidColor), Color.blue(liquidColor)));
            rect.set(cx - 110f, cy + 35f, cx + 110f, cy + 88f);
            canvas.drawRoundRect(rect, 34f, 34f, shapePaint);

            drawSwirl(canvas, cx, cy + 62f, elapsed, liquidColor, poundAnimating || strainAnimating);
            drawBubbles(canvas, cx, cy, elapsed, poundAnimating || alcoholAnimating);
        }

        linePaint.setColor(Color.rgb(104, 64, 158));
        canvas.drawArc(rect, 0f, 180f, false, linePaint);
        for (int i = 0; i < selectedMaterials.size(); i++) {
            shapePaint.setColor(selectedMaterials.get(i).color);
            float orbit = poundAnimating ? (float) Math.sin(elapsed * 10f + i) * 15f : 0f;
            float size = pounded ? 13f : 20f;
            canvas.drawCircle(cx - 70f + i * 46f + orbit, cy + 35f + (i % 2) * 20f - bob * 0.25f, size, shapePaint);
        }

        drawPestle(canvas, cx, cy, bob, poundAnimating);

        if (alcoholAnimating) {
            drawAlcoholPour(canvas, cx, cy, Math.min(1f, elapsed / 1.7f));
        }
        } finally {
            canvas.restore();
        }
    }

    private void drawPestle(Canvas canvas, float cx, float cy, float bob, boolean active) {
        float angleOffset = active ? (float) Math.sin((System.currentTimeMillis() - animationStartMs) / 70f) * 16f : -8f;
        linePaint.setColor(Color.rgb(122, 92, 84));
        linePaint.setStrokeWidth(18f);
        canvas.drawLine(cx + 18f + angleOffset, cy - 92f - bob, cx - 18f - angleOffset, cy + 42f - bob, linePaint);
        linePaint.setStrokeWidth(6f);
        shapePaint.setColor(Color.rgb(166, 130, 112));
        canvas.drawCircle(cx - 18f - angleOffset, cy + 42f - bob, 18f, shapePaint);
    }

    private void drawAlcoholPour(Canvas canvas, float cx, float cy, float progress) {
        float startY = cy - 138f;
        float endY = cy + 40f;
        linePaint.setColor(Color.argb(190, 132, 219, 242));
        linePaint.setStrokeWidth(10f);
        canvas.drawLine(cx - 84f, startY, cx - 84f, startY + (endY - startY) * progress, linePaint);
        linePaint.setStrokeWidth(6f);

        shapePaint.setColor(Color.rgb(185, 232, 250));
        rect.set(cx - 122f, startY - 56f, cx - 52f, startY + 2f);
        canvas.drawRoundRect(rect, 20f, 20f, shapePaint);
        drawText(canvas, "Alcohol", cx - 87f, startY - 18f, 18f, Color.rgb(86, 54, 130));
    }

    private void drawSwirl(Canvas canvas, float cx, float cy, float elapsed, int color, boolean active) {
        linePaint.setColor(Color.argb(active ? 210 : 120, Color.red(color), Color.green(color), Color.blue(color)));
        linePaint.setStrokeWidth(7f);
        float phase = elapsed * 6f;
        for (int i = 0; i < 3; i++) {
            float y = cy - 18f + i * 17f;
            float start = cx - 82f + (float) Math.sin(phase + i) * 14f;
            rect.set(start, y - 16f, start + 160f, y + 16f);
            canvas.drawArc(rect, 15f, 150f, false, linePaint);
        }
        linePaint.setStrokeWidth(6f);
    }

    private void drawBubbles(Canvas canvas, float cx, float cy, float elapsed, boolean active) {
        shapePaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 7; i++) {
            float t = active ? (elapsed * 45f + i * 23f) % 70f : i * 7f;
            float x = cx - 80f + i * 27f + (float) Math.sin(elapsed * 4f + i) * 8f;
            float y = cy + 88f - t;
            shapePaint.setColor(Color.argb(135, 255, 255, 255));
            canvas.drawCircle(x, y, 5f + i % 3, shapePaint);
        }
    }

    private void drawCraftSparkles(Canvas canvas, float cx, float cy, float elapsed, boolean active) {
        if (!active) {
            return;
        }

        int[] colors = new int[] {
                Color.rgb(255, 214, 117),
                Color.rgb(255, 122, 170),
                Color.rgb(130, 235, 255),
                Color.rgb(190, 145, 255)
        };
        for (int i = 0; i < 10; i++) {
            float pulse = (float) Math.abs(Math.sin(elapsed * 5f + i));
            float x = cx - 150f + i * 34f;
            float y = cy - 105f + (i % 3) * 38f - pulse * 18f;
            shapePaint.setColor(Color.argb((int) (100 + pulse * 120), Color.red(colors[i % colors.length]), Color.green(colors[i % colors.length]), Color.blue(colors[i % colors.length])));
            canvas.drawCircle(x, y, 4f + pulse * 5f, shapePaint);
        }
    }

    private int blendSelectedMaterialColors(int fallbackColor) {
        if (selectedMaterials.isEmpty()) {
            return fallbackColor;
        }

        int red = Color.red(fallbackColor);
        int green = Color.green(fallbackColor);
        int blue = Color.blue(fallbackColor);
        for (Material material : selectedMaterials) {
            red += Color.red(material.color);
            green += Color.green(material.color);
            blue += Color.blue(material.color);
        }
        int count = selectedMaterials.size() + 1;
        return Color.rgb(red / count, green / count, blue / count);
    }

    private void drawStepPill(Canvas canvas, String label, boolean complete, float cx, float cy) {
        int color = complete ? Color.rgb(74, 212, 184) : Color.WHITE;
        drawCard(canvas, cx - 58f, cy - 28f, cx + 58f, cy + 28f, color);
        drawText(canvas, label, cx, cy + 9f, 18f, complete ? Color.WHITE : Color.rgb(104, 64, 158));
    }

    private void drawPerfumeBottle(Canvas canvas, float left, float top, float width, float height, int color, int shape) {
        shapePaint.setColor(color);
        if (shape == 1) {
            rect.set(left, top + height * 0.23f, left + width, top + height);
            canvas.drawRoundRect(rect, width / 2f, width / 2f, shapePaint);
        } else if (shape == 2) {
            rect.set(left, top + height * 0.25f, left + width, top + height);
            canvas.drawRoundRect(rect, 18f, 18f, shapePaint);
        } else {
            rect.set(left + width * 0.15f, top + height * 0.20f, left + width * 0.85f, top + height);
            canvas.drawRoundRect(rect, 32f, 32f, shapePaint);
        }
        shapePaint.setColor(Color.WHITE);
        rect.set(left + width * 0.36f, top, left + width * 0.64f, top + height * 0.32f);
        canvas.drawRoundRect(rect, 12f, 12f, shapePaint);
        linePaint.setColor(Color.rgb(104, 64, 158));
        canvas.drawRoundRect(rect, 12f, 12f, linePaint);
    }

    private void drawFunnel(Canvas canvas, float cx, float cy) {
        linePaint.setColor(Color.rgb(104, 64, 158));
        canvas.drawLine(cx - 46f, cy - 40f, cx + 46f, cy - 40f, linePaint);
        canvas.drawLine(cx - 46f, cy - 40f, cx - 12f, cy + 18f, linePaint);
        canvas.drawLine(cx + 46f, cy - 40f, cx + 12f, cy + 18f, linePaint);
        canvas.drawLine(cx - 12f, cy + 18f, cx + 12f, cy + 18f, linePaint);
    }

    private void drawButton(Canvas canvas, String label, String action, int index, float left, float top, float right, float bottom, int color) {
        shapePaint.setColor(darken(color));
        rect.set(left + 4f, top + 6f, right + 4f, bottom + 6f);
        canvas.drawRoundRect(rect, 30f, 30f, shapePaint);
        shapePaint.setColor(color);
        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, 30f, 30f, shapePaint);
        drawText(canvas, label, (left + right) / 2f, (top + bottom) / 2f + 11f, 25f, isLight(color) ? Color.rgb(104, 64, 158) : Color.WHITE);
        addHit(left, top, right, bottom, action, index);
    }

    private void drawCard(Canvas canvas, float left, float top, float right, float bottom, int color) {
        shapePaint.setStyle(Paint.Style.FILL);
        shapePaint.setColor(Color.argb(55, 104, 64, 158));
        rect.set(left + 4f, top + 6f, right + 4f, bottom + 6f);
        canvas.drawRoundRect(rect, 34f, 34f, shapePaint);
        shapePaint.setColor(color);
        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, 34f, 34f, shapePaint);
    }

    private void drawText(Canvas canvas, String text, float x, float y, float size, int color) {
        textPaint.setColor(color);
        textPaint.setTextSize(size);
        canvas.drawText(text, x, y, textPaint);
    }

    private float calculateContentScale() {
        float widthScale = getWidth() / DESIGN_WIDTH;
        float heightScale = getHeight() / DESIGN_HEIGHT;
        return Math.max(0.85f, Math.min(3.2f, Math.min(widthScale, heightScale)));
    }

    private int viewWidth() {
        return Math.round(getWidth() / contentScale);
    }

    private int viewHeight() {
        return Math.round(getHeight() / contentScale);
    }

    private int safeTop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return 0;
        }
        WindowInsets insets = getRootWindowInsets();
        if (insets == null) {
            return 0;
        }
        int top = insets.getSystemWindowInsetTop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                top = Math.max(top, cutout.getSafeInsetTop());
            }
        }
        return Math.round(top / contentScale);
    }

    private boolean isLight(int color) {
        return Color.red(color) + Color.green(color) + Color.blue(color) > 600;
    }

    private int darken(int color) {
        return Color.rgb(Color.red(color) * 4 / 5, Color.green(color) * 4 / 5, Color.blue(color) * 4 / 5);
    }

    private void addHit(float left, float top, float right, float bottom, String action, int index) {
        hitAreas.add(new HitArea(left, top, right, bottom, action, index));
    }

    private static class HitArea {
        final RectF rect;
        final String action;
        final int index;

        HitArea(float left, float top, float right, float bottom, String action, int index) {
            this.rect = new RectF(left, top, right, bottom);
            this.action = action;
            this.index = index;
        }
    }

    private static class CustomerRequest {
        final String name;
        final String primaryTag;
        final String secondaryTag;
        final String requestText;

        CustomerRequest(String name, String primaryTag, String secondaryTag, String requestText) {
            this.name = name;
            this.primaryTag = primaryTag;
            this.secondaryTag = secondaryTag;
            this.requestText = requestText;
        }
    }

    private static class Material {
        final String name;
        final String category;
        final String tag;
        final int color;

        Material(String name, String category, String tag, int color) {
            this.name = name;
            this.category = category;
            this.tag = tag;
            this.color = color;
        }

        String shortName() {
            int space = name.indexOf(' ');
            return space > 0 ? name.substring(0, space) : name;
        }
    }

    private static class BottleOption {
        final String label;
        final int color;
        final int shape;

        BottleOption(String label, int color, int shape) {
            this.label = label;
            this.color = color;
            this.shape = shape;
        }
    }

    private static class PerfumeResult {
        final String perfumeName;
        final int score;
        final String reaction;
        final String customerLine;

        PerfumeResult(String perfumeName, int score, String reaction, String customerLine) {
            this.perfumeName = perfumeName;
            this.score = score;
            this.reaction = reaction;
            this.customerLine = customerLine;
        }
    }
}
