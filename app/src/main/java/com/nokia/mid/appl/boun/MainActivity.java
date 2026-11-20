package com.nokia.mid.appl.boun;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ResourceLoader;

public class MainActivity extends Activity {
    private static final String TAG = "NokiaBounce";
    private GameView gameView;
    private Bounce midlet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen & Keep Screen On
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ResourceLoader.appContext = getApplicationContext();

        gameView = new GameView(this);
        setContentView(gameView);

        // Launch Game in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Initializing Nokia Bounce...");
                    midlet = new Bounce();
                    midlet.startApp();
                    Log.i(TAG, "Nokia Bounce started successfully.");
                } catch (Throwable t) {
                    Log.e(TAG, "Exception during Nokia Bounce startup", t);
                }
            }
        }, "BounceInitThread").start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (midlet != null) {
            midlet.pauseApp();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (midlet != null) {
            try {
                midlet.startApp();
            } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (gameView != null && gameView.handlePhysicalKey(keyCode, true)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (gameView != null && gameView.handlePhysicalKey(keyCode, false)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // Custom SurfaceView for rendering retro Nokia display & on-screen HUD
    public static class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
        private SurfaceHolder holder;
        private Thread renderThread;
        private volatile boolean running = false;

        // Paints
        private Paint bitmapPaint;
        private Paint bezelPaint;
        private Paint frameBorderPaint;
        private Paint panelBgPaint;
        private Paint btnNormalPaint;
        private Paint btnPressedPaint;
        private Paint btnActionPressedPaint;
        private Paint btnBorderPaint;
        private Paint btnBorderGlowPaint;
        private Paint textPaint;
        private Paint subTextPaint;
        private Paint iconPaint;
        private Paint ballPaint;
        private Paint menuTextPaint;
        private Paint menuSelectPaint;
        private Vibrator vibrator;

        // Double-buffer for 128x128 game frame
        private Bitmap gameScreenBmp;
        private Canvas gameScreenCanvas;
        private final Object bufferLock = new Object();

        // Control Mode: 0 = Full 3x4 Retro Numpad, 1 = D-Pad + Bounce Ball Action
        private int controlMode = 0;

        // Cheat Modal State
        private boolean showCheatModal = false;
        private String cheatToast = null;
        private long cheatToastTime = 0;

        // Bounding boxes
        private RectF rectGame = new RectF();
        private RectF rectBezel = new RectF();
        private RectF rectPanel = new RectF();
        private RectF rectModeSwitch = new RectF();
        private RectF rectCheatBtn = new RectF();

        // Cheat Modal Rects
        private RectF rectCheatGodMode = new RectF();
        private RectF rectCheatFlyMode = new RectF();
        private RectF rectCheatSuperSpeed = new RectF();
        private RectF rectCheatNextLevel = new RectF();
        private RectF rectCheatPrevLevel = new RectF();
        private RectF rectCheatClose = new RectF();
        private RectF rectCheatBox = new RectF();

        public static class HudButton {
            public int keyCode;
            public String label;
            public String subLabel;
            public RectF rect = new RectF();
            public boolean isPressed = false;
            public int iconType;

            public HudButton(int keyCode, String label, String subLabel, int iconType) {
                this.keyCode = keyCode;
                this.label = label;
                this.subLabel = subLabel;
                this.iconType = iconType;
            }
        }

        private List<HudButton> buttons = new ArrayList<>();
        private HudButton btnSoftLeft = new HudButton(-6, "SELECT", "", 0);
        private HudButton btnSoftRight = new HudButton(-7, "BACK", "", 0);

        private HudButton btn1 = new HudButton(49, "1", "PREV", 1);
        private HudButton btn2 = new HudButton(50, "2", "UP", 2);
        private HudButton btn3 = new HudButton(51, "3", "NEXT", 3);
        private HudButton btn4 = new HudButton(52, "4", "LEFT", 4);
        private HudButton btn5 = new HudButton(53, "5", "JUMP", 5);
        private HudButton btn6 = new HudButton(54, "6", "RIGHT", 6);
        private HudButton btn7 = new HudButton(55, "7", "CHEAT", 7);
        private HudButton btn8 = new HudButton(56, "8", "DOWN", 8);
        private HudButton btn9 = new HudButton(57, "9", "CHEAT", 9);
        private HudButton btnStar = new HudButton(42, "*", "", 10);
        private HudButton btn0 = new HudButton(48, "0", "", 11);
        private HudButton btnPound = new HudButton(35, "#", "SPEED", 12);

        public GameView(Context context) {
            super(context);
            holder = getHolder();
            holder.addCallback(this);

            gameScreenBmp = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
            gameScreenCanvas = new Canvas(gameScreenBmp);

            bitmapPaint = new Paint();
            bitmapPaint.setFilterBitmap(false); // Pixel-perfect crisp scaling

            bezelPaint = new Paint();
            bezelPaint.setColor(0xFF1E2838); // Retro Nokia Navy
            bezelPaint.setStyle(Paint.Style.FILL);

            frameBorderPaint = new Paint();
            frameBorderPaint.setColor(0xFFFFB300); // Amber gold trim
            frameBorderPaint.setStyle(Paint.Style.STROKE);
            frameBorderPaint.setStrokeWidth(4f);

            panelBgPaint = new Paint();
            panelBgPaint.setColor(0x55101622); // Semi-transparent glassmorphism
            panelBgPaint.setStyle(Paint.Style.FILL);

            btnNormalPaint = new Paint();
            btnNormalPaint.setColor(0x66243048);
            btnNormalPaint.setStyle(Paint.Style.FILL);

            btnPressedPaint = new Paint();
            btnPressedPaint.setColor(0x99FFA000); // Amber glow
            btnPressedPaint.setStyle(Paint.Style.FILL);

            btnActionPressedPaint = new Paint();
            btnActionPressedPaint.setColor(0xCCFF3D00); // Glowing Red Bounce Ball
            btnActionPressedPaint.setStyle(Paint.Style.FILL);

            btnBorderPaint = new Paint();
            btnBorderPaint.setColor(0xCC4A90E2); // Nokia cyan border
            btnBorderPaint.setStyle(Paint.Style.STROKE);
            btnBorderPaint.setStrokeWidth(3f);

            btnBorderGlowPaint = new Paint();
            btnBorderGlowPaint.setColor(0xFFFFD54F);
            btnBorderGlowPaint.setStyle(Paint.Style.STROKE);
            btnBorderGlowPaint.setStrokeWidth(5f);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);

            subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            subTextPaint.setColor(0xFFFFD54F);
            subTextPaint.setTextAlign(Paint.Align.CENTER);
            subTextPaint.setTextSize(18f);

            iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setColor(0xFFFFD54F);
            iconPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            iconPaint.setStrokeWidth(3f);

            ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ballPaint.setColor(0xFFFF2A2A); // Iconic Red Bounce Ball
            ballPaint.setStyle(Paint.Style.FILL);

            menuTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            menuTextPaint.setColor(0xFFFFFFFF);
            menuTextPaint.setTextSize(10f);

            menuSelectPaint = new Paint();
            menuSelectPaint.setColor(0xFF0055A5); // Nokia blue selection bar
            menuSelectPaint.setStyle(Paint.Style.FILL);

            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            buttons.add(btnSoftLeft);
            buttons.add(btnSoftRight);
            buttons.add(btn1);
            buttons.add(btn2);
            buttons.add(btn3);
            buttons.add(btn4);
            buttons.add(btn5);
            buttons.add(btn6);
            buttons.add(btn7);
            buttons.add(btn8);
            buttons.add(btn9);
            buttons.add(btnStar);
            buttons.add(btn0);
            buttons.add(btnPound);
        }

        public boolean handlePhysicalKey(int keyCode, boolean isDown) {
            int j2meKey = 0;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_W:
                case KeyEvent.KEYCODE_2:
                case KeyEvent.KEYCODE_NUMPAD_2:
                    j2meKey = 50; break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_8:
                case KeyEvent.KEYCODE_NUMPAD_8:
                    j2meKey = 56; break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_A:
                case KeyEvent.KEYCODE_4:
                case KeyEvent.KEYCODE_NUMPAD_4:
                    j2meKey = 52; break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_D:
                case KeyEvent.KEYCODE_6:
                case KeyEvent.KEYCODE_NUMPAD_6:
                    j2meKey = 54; break;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_SPACE:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_5:
                case KeyEvent.KEYCODE_NUMPAD_5:
                    j2meKey = 53; break;
                case KeyEvent.KEYCODE_1:
                case KeyEvent.KEYCODE_NUMPAD_1:
                    j2meKey = 49; break;
                case KeyEvent.KEYCODE_3:
                case KeyEvent.KEYCODE_NUMPAD_3:
                    j2meKey = 51; break;
                case KeyEvent.KEYCODE_7:
                case KeyEvent.KEYCODE_NUMPAD_7:
                    j2meKey = 55; break;
                case KeyEvent.KEYCODE_9:
                case KeyEvent.KEYCODE_NUMPAD_9:
                    j2meKey = 57; break;
                case KeyEvent.KEYCODE_0:
                case KeyEvent.KEYCODE_NUMPAD_0:
                    j2meKey = 48; break;
                case KeyEvent.KEYCODE_STAR:
                case KeyEvent.KEYCODE_NUMPAD_MULTIPLY:
                    j2meKey = 42; break;
                case KeyEvent.KEYCODE_POUND:
                    j2meKey = 35; break;
                case KeyEvent.KEYCODE_BUTTON_B:
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                case KeyEvent.KEYCODE_Q:
                    j2meKey = -6; break;
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_BUTTON_START:
                case KeyEvent.KEYCODE_E:
                    j2meKey = -7; break;
            }

            if (j2meKey != 0) {
                dispatchKey(j2meKey, isDown);
                return true;
            }
            return false;
        }

        private void dispatchKey(int keyCode, boolean isDown) {
            Display disp = Display.getInstance();
            if (disp == null) return;
            Displayable d = disp.getCurrent();

            if (d instanceof e) {
                e gameCanvas = (e) d;
                if (isDown) {
                    gameCanvas.keyPressed(keyCode);
                    triggerVibrate(20);
                } else {
                    gameCanvas.keyReleased(keyCode);
                }
            } else if (d instanceof javax.microedition.lcdui.List) {
                // Menu Navigation
                if (isDown) {
                    javax.microedition.lcdui.List list = (javax.microedition.lcdui.List) d;
                    CommandListener cl = list.getCommandListener();
                    if (keyCode == 50) { // UP
                        int idx = list.getSelectedIndex() - 1;
                        if (idx < 0) idx = list.size() - 1;
                        list.setSelectedIndex(idx, true);
                        triggerVibrate(20);
                    } else if (keyCode == 56) { // DOWN
                        int idx = list.getSelectedIndex() + 1;
                        if (idx >= list.size()) idx = 0;
                        list.setSelectedIndex(idx, true);
                        triggerVibrate(20);
                    } else if (keyCode == 53 || keyCode == -6) { // SELECT
                        if (cl != null) {
                            cl.commandAction(javax.microedition.lcdui.List.SELECT_COMMAND, list);
                            triggerVibrate(30);
                        }
                    } else if (keyCode == -7) { // BACK
                        if (cl != null && !list.getCommands().isEmpty()) {
                            cl.commandAction(list.getCommands().get(0), list);
                            triggerVibrate(30);
                        }
                    }
                }
            } else if (d instanceof Form) {
                if (isDown) {
                    Form form = (Form) d;
                    CommandListener cl = form.getCommandListener();
                    if ((keyCode == -6 || keyCode == -7 || keyCode == 53) && cl != null && !form.getCommands().isEmpty()) {
                        cl.commandAction(form.getCommands().get(0), form);
                        triggerVibrate(30);
                    }
                }
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder h) {
            running = true;
            renderThread = new Thread(this, "BounceRenderThread");
            renderThread.start();
        }

        @Override
        public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
            layoutControls(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder h) {
            running = false;
            try {
                renderThread.join();
            } catch (Exception ignored) {}
        }

        private void layoutControls(int w, int h) {
            if (w <= 0 || h <= 0) return;

            // Target aspect ratio: 1:1 Square (128x128)
            float maxGameH = h * 0.44f;
            float maxGameW = w * 0.88f;
            float gameSide = Math.min(maxGameW, maxGameH);

            float gameLeft = (w - gameSide) / 2f;
            float gameTop = 16f;
            rectGame.set(gameLeft, gameTop, gameLeft + gameSide, gameTop + gameSide);

            // Bezel casing around square LCD
            rectBezel.set(gameLeft - 8f, gameTop - 8f, gameLeft + gameSide + 8f, gameTop + gameSide + 8f);

            // Panel sits below LCD
            float panelTop = rectBezel.bottom + 8f;
            float panelBottom = h - 8f;
            rectPanel.set(8f, panelTop, w - 8f, panelBottom);

            float panelW = rectPanel.width();
            float panelH = rectPanel.height();

            // Header row: [ SELECT ]  [ ⚡ CHEAT ]  [ 🔄 MODE ]  [ BACK ]
            float headerH = Math.min(panelH * 0.125f, 62f);
            float gap = 6f;
            float colW = (panelW - (gap * 5)) / 4f;
            float headerY = panelTop + 6f;

            btnSoftLeft.rect.set(rectPanel.left + gap, headerY, rectPanel.left + gap + colW, headerY + headerH);
            rectCheatBtn.set(rectPanel.left + gap * 2 + colW, headerY, rectPanel.left + gap * 2 + colW * 2, headerY + headerH);
            rectModeSwitch.set(rectPanel.left + gap * 3 + colW * 2, headerY, rectPanel.left + gap * 3 + colW * 3, headerY + headerH);
            btnSoftRight.rect.set(rectPanel.left + gap * 4 + colW * 3, headerY, rectPanel.left + gap * 4 + colW * 4, headerY + headerH);

            float gridTop = headerY + headerH + 8f;
            float gridH = panelBottom - gridTop;
            float gridW = panelW;

            if (controlMode == 0) {
                // ==========================================
                // MODE 0: FULL 3x4 RETRO NOKIA NUMPAD
                // ==========================================
                float gapX = 8f;
                float gapY = 8f;
                float cellW = (gridW - (gapX * 4)) / 3f;
                float cellH = (gridH - (gapY * 5)) / 4f;

                HudButton[][] grid = new HudButton[][] {
                    { btn1, btn2, btn3 },
                    { btn4, btn5, btn6 },
                    { btn7, btn8, btn9 },
                    { btnStar, btn0, btnPound }
                };

                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 3; c++) {
                        float left = rectPanel.left + gapX + c * (cellW + gapX);
                        float top = gridTop + gapY + r * (cellH + gapY);
                        grid[r][c].rect.set(left, top, left + cellW, top + cellH);
                    }
                }
            } else {
                // ==========================================
                // MODE 1: RETRO D-PAD + RED BOUNCE BALL JUMP
                // ==========================================
                float dpadW = gridW * 0.52f;
                float btnSize = Math.min(dpadW / 3.1f, gridH / 3.2f);
                float dpadCenterX = rectPanel.left + dpadW * 0.48f;
                float dpadCenterY = gridTop + gridH * 0.52f;

                btn2.rect.set(dpadCenterX - btnSize / 2, dpadCenterY - btnSize * 1.5f,
                              dpadCenterX + btnSize / 2, dpadCenterY - btnSize * 0.5f);
                btn8.rect.set(dpadCenterX - btnSize / 2, dpadCenterY + btnSize * 0.5f,
                              dpadCenterX + btnSize / 2, dpadCenterY + btnSize * 1.5f);
                btn4.rect.set(dpadCenterX - btnSize * 1.5f, dpadCenterY - btnSize / 2,
                              dpadCenterX - btnSize * 0.5f, dpadCenterY + btnSize / 2);
                btn6.rect.set(dpadCenterX + btnSize * 0.5f, dpadCenterY - btnSize / 2,
                              dpadCenterX + btnSize * 1.5f, dpadCenterY + btnSize / 2);

                // Big Red Bounce Ball Jump on Right
                float rightW = gridW - dpadW;
                float actionCenterX = rectPanel.left + dpadW + rightW * 0.5f;
                float actionCenterY = dpadCenterY;
                float actionR = btnSize * 1.25f;
                btn5.rect.set(actionCenterX - actionR, actionCenterY - actionR,
                              actionCenterX + actionR, actionCenterY + actionR);

                // Quick Tools (Prev 1, Next 3, Speed #)
                float toolW = (rightW - 16f) / 3f;
                float toolH = btnSize * 0.65f;
                float toolY = gridTop + 4f;

                btn1.rect.set(actionCenterX - toolW * 1.5f - 4f, toolY, actionCenterX - toolW * 0.5f - 4f, toolY + toolH);
                btnPound.rect.set(actionCenterX - toolW * 0.5f, toolY, actionCenterX + toolW * 0.5f, toolY + toolH);
                btn3.rect.set(actionCenterX + toolW * 0.5f + 4f, toolY, actionCenterX + toolW * 1.5f + 4f, toolY + toolH);

                btn7.rect.setEmpty();
                btn9.rect.setEmpty();
                btn0.rect.setEmpty();
                btnStar.rect.setEmpty();
            }

            // Layout Cheat Modal
            float modalW = Math.min(w * 0.92f, 520f);
            float modalH = Math.min(h * 0.62f, 500f);
            float modalLeft = (w - modalW) / 2f;
            float modalTop = (h - modalH) / 2f;
            rectCheatBox.set(modalLeft, modalTop, modalLeft + modalW, modalTop + modalH);

            float itemH = (modalH - 95f) / 5f;
            float curY = modalTop + 50f;
            float padX = 20f;
            float curW = modalW - padX * 2;

            rectCheatGodMode.set(modalLeft + padX, curY, modalLeft + padX + curW, curY + itemH - 8f); curY += itemH;
            rectCheatFlyMode.set(modalLeft + padX, curY, modalLeft + padX + curW, curY + itemH - 8f); curY += itemH;
            rectCheatSuperSpeed.set(modalLeft + padX, curY, modalLeft + padX + curW, curY + itemH - 8f); curY += itemH;
            rectCheatNextLevel.set(modalLeft + padX, curY, modalLeft + padX + curW, curY + itemH - 8f); curY += itemH;
            rectCheatPrevLevel.set(modalLeft + padX, curY, modalLeft + padX + curW, curY + itemH - 8f); curY += itemH;
            rectCheatClose.set(modalLeft + padX * 2, modalTop + modalH - 40f, modalLeft + modalW - padX * 2, modalTop + modalH - 6f);
        }

        @Override
        public void run() {
            while (running) {
                Canvas c = null;
                try {
                    c = holder.lockCanvas();
                    if (c != null) {
                        renderFrame(c);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        try {
                            holder.unlockCanvasAndPost(c);
                        } catch (Exception ignored) {}
                    }
                }

                try {
                    Thread.sleep(16); // 60 FPS
                } catch (InterruptedException ignored) {}
            }
        }

        private void renderFrame(Canvas c) {
            c.drawColor(0xFF0C101A); // Dark retro backdrop

            int screenW = getWidth();
            int screenH = getHeight();
            if (rectGame.isEmpty() && screenW > 0 && screenH > 0) {
                layoutControls(screenW, screenH);
            }

            // Draw Bezel & Screen frame
            c.drawRoundRect(rectBezel, 14, 14, bezelPaint);
            c.drawRoundRect(rectBezel, 14, 14, frameBorderPaint);

            // Render current Displayable to 128x128 buffer
            renderDisplayableToBuffer();

            // Blit pixel-crisp 128x128 game buffer onto screen
            synchronized (bufferLock) {
                if (gameScreenBmp != null) {
                    Rect src = new Rect(0, 0, 128, 128);
                    c.drawBitmap(gameScreenBmp, src, rectGame, bitmapPaint);
                }
            }

            // Draw HUD Panel background
            c.drawRoundRect(rectPanel, 16, 16, panelBgPaint);

            // Draw Header Buttons
            drawHeaderButton(c, rectCheatBtn, "⚡ CHEAT", 0xFFFFD700);
            String modeStr = (controlMode == 0) ? "🔢 NUM" : "🎮 DPAD";
            drawHeaderButton(c, rectModeSwitch, modeStr, 0xFF00E5FF);

            // Draw All Touch Buttons
            for (HudButton b : buttons) {
                if (!b.rect.isEmpty()) {
                    drawHudButton(c, b);
                }
            }

            // Draw Cheat Toast
            if (cheatToast != null && System.currentTimeMillis() - cheatToastTime < 2500) {
                Paint toastBg = new Paint();
                toastBg.setColor(0xEEFF3D00);
                RectF toastRect = new RectF(rectGame.centerX() - 160, rectGame.top + 14, rectGame.centerX() + 160, rectGame.top + 60);
                c.drawRoundRect(toastRect, 10, 10, toastBg);
                Paint toastText = new Paint(Paint.ANTI_ALIAS_FLAG);
                toastText.setColor(0xFFFFFFFF);
                toastText.setTextSize(24);
                toastText.setFakeBoldText(true);
                toastText.setTextAlign(Paint.Align.CENTER);
                c.drawText(cheatToast, toastRect.centerX(), toastRect.centerY() + 8, toastText);
            }

            // Draw Cheat Modal
            if (showCheatModal) {
                drawCheatModal(c);
            }
        }

        private void renderDisplayableToBuffer() {
            Display disp = Display.getInstance();
            if (disp == null) return;
            Displayable d = disp.getCurrent();

            synchronized (bufferLock) {
                if (d instanceof e) {
                    e gameCanvas = (e) d;
                    if (gameCanvas.F != null && gameCanvas.F.getBitmap() != null) {
                        gameScreenCanvas.drawBitmap(gameCanvas.F.getBitmap(), 0, 0, null);
                    }
                } else if (d instanceof javax.microedition.lcdui.List) {
                    // Render Nokia Series 40 Menu List
                    javax.microedition.lcdui.List list = (javax.microedition.lcdui.List) d;
                    gameScreenCanvas.drawColor(0xFF002A54); // Nokia Deep Blue Menu

                    // Title Bar
                    Paint p = new Paint();
                    p.setColor(0xFF004488);
                    gameScreenCanvas.drawRect(0, 0, 128, 18, p);
                    Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tp.setColor(0xFFFFD700);
                    tp.setTextSize(10f);
                    tp.setFakeBoldText(true);
                    String title = list.getTitle() != null ? list.getTitle() : "Bounce";
                    gameScreenCanvas.drawText(title, 6, 13, tp);

                    // List items
                    int sel = list.getSelectedIndex();
                    int startY = 22;
                    int itemH = 16;
                    ArrayList<String> elems = list.getElements();

                    for (int i = 0; i < elems.size(); i++) {
                        int y = startY + i * itemH;
                        if (y > 110) break;
                        if (i == sel) {
                            gameScreenCanvas.drawRect(2, y - 1, 126, y + itemH - 3, menuSelectPaint);
                            tp.setColor(0xFFFFFFFF);
                        } else {
                            tp.setColor(0xFFB0C4DE);
                        }
                        gameScreenCanvas.drawText(elems.get(i), 8, y + 10, tp);
                    }

                    // Bottom softkey hints
                    p.setColor(0xFF001B36);
                    gameScreenCanvas.drawRect(0, 114, 128, 128, p);
                    tp.setColor(0xFFFFFFFF);
                    tp.setTextSize(8f);
                    gameScreenCanvas.drawText("Select", 4, 124, tp);
                    gameScreenCanvas.drawText("Back", 100, 124, tp);

                } else if (d instanceof Form) {
                    // Render Nokia Form
                    Form form = (Form) d;
                    gameScreenCanvas.drawColor(0xFF002244);
                    Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
                    tp.setColor(0xFFFFD700);
                    tp.setTextSize(10f);
                    tp.setFakeBoldText(true);
                    gameScreenCanvas.drawText(form.getTitle() != null ? form.getTitle() : "Info", 6, 14, tp);

                    tp.setColor(0xFFFFFFFF);
                    tp.setTextSize(9f);
                    int y = 30;
                    for (String line : form.getItems()) {
                        String[] split = line.split("\n");
                        for (String s : split) {
                            if (y > 110) break;
                            gameScreenCanvas.drawText(s, 6, y, tp);
                            y += 12;
                        }
                    }
                }
            }
        }

        private void drawHeaderButton(Canvas c, RectF r, String label, int accentColor) {
            c.drawRoundRect(r, 12, 12, btnNormalPaint);
            Paint border = new Paint(btnBorderPaint);
            border.setColor(accentColor);
            c.drawRoundRect(r, 12, 12, border);

            textPaint.setTextSize(Math.max(16f, r.height() * 0.38f));
            float ty = r.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
            c.drawText(label, r.centerX(), ty, textPaint);
        }

        private void drawHudButton(Canvas c, HudButton b) {
            RectF r = b.rect;
            if (r.isEmpty()) return;

            boolean isPressed = b.isPressed;
            boolean isJump = (b.iconType == 5);

            Paint fill = isPressed ? (isJump ? btnActionPressedPaint : btnPressedPaint) : btnNormalPaint;
            c.drawRoundRect(r, 14, 14, fill);
            c.drawRoundRect(r, 14, 14, isPressed ? btnBorderGlowPaint : btnBorderPaint);

            float cx = r.centerX();
            float cy = r.centerY();
            float size = Math.min(r.width(), r.height()) * 0.44f;

            if (isJump) {
                // Red Bounce Ball Icon
                c.drawCircle(cx, cy - 2f, size * 0.7f, ballPaint);
                Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
                highlight.setColor(0xAAFFFFFF);
                c.drawCircle(cx - size * 0.22f, cy - size * 0.35f, size * 0.22f, highlight);

                textPaint.setTextSize(Math.max(18f, size * 0.5f));
                c.drawText("JUMP", cx, cy + size * 0.95f, textPaint);
            } else {
                switch (b.iconType) {
                    case 2: drawArrow(c, cx, cy - 4f, size, 0); break;
                    case 8: drawArrow(c, cx, cy - 4f, size, 180); break;
                    case 4: drawArrow(c, cx, cy - 4f, size, 270); break;
                    case 6: drawArrow(c, cx, cy - 4f, size, 90); break;
                    default:
                        break;
                }

                // Number label
                textPaint.setTextSize(Math.max(18f, r.height() * 0.32f));
                float lx = r.right - 14f;
                float ly = r.bottom - 8f;
                textPaint.setTextAlign(Paint.Align.RIGHT);
                c.drawText(b.label, lx, ly, textPaint);
                textPaint.setTextAlign(Paint.Align.CENTER);

                if (b.subLabel != null && b.subLabel.length() > 0) {
                    subTextPaint.setTextSize(Math.max(14f, r.height() * 0.2f));
                    subTextPaint.setTextAlign(Paint.Align.LEFT);
                    c.drawText(b.subLabel, r.left + 12f, r.bottom - 8f, subTextPaint);
                    subTextPaint.setTextAlign(Paint.Align.CENTER);
                }
            }
        }

        private void drawArrow(Canvas c, float cx, float cy, float size, float angleDeg) {
            c.save();
            c.rotate(angleDeg, cx, cy);
            float s = size * 0.35f;
            Path pth = new Path();
            pth.moveTo(cx, cy - s * 1.1f);
            pth.lineTo(cx + s * 0.8f, cy + s * 0.6f);
            pth.lineTo(cx, cy + s * 0.15f);
            pth.lineTo(cx - s * 0.8f, cy + s * 0.6f);
            pth.close();
            iconPaint.setColor(0xFFFFD54F);
            c.drawPath(pth, iconPaint);
            c.restore();
        }

        private void drawCheatModal(Canvas c) {
            c.drawColor(0xBB000000);
            c.drawRoundRect(rectCheatBox, 18, 18, bezelPaint);
            c.drawRoundRect(rectCheatBox, 18, 18, frameBorderPaint);

            textPaint.setTextSize(30f);
            textPaint.setColor(0xFFFFD700);
            c.drawText("⚡ CHEAT MENU NOKIA BOUNCE", rectCheatBox.centerX(), rectCheatBox.top + 34f, textPaint);
            textPaint.setColor(0xFFFFFFFF);

            e game = getActiveGameCanvas();
            boolean isGod = (game != null && game.x);

            drawCheatOption(c, rectCheatGodMode, isGod ? "🛡️ GOD MODE: [ AKTIF ] (787898)" : "🛡️ GOD MODE: [ NONAKTIF ] (787898)", isGod);
            drawCheatOption(c, rectCheatFlyMode, "🚀 FLY & FLOAT MODE (787899)", false);
            drawCheatOption(c, rectCheatSuperSpeed, "⚡ SUPER SPEED WARP (#)", false);
            drawCheatOption(c, rectCheatNextLevel, "⏩ LEVEL SELANJUTNYA (3)", false);
            drawCheatOption(c, rectCheatPrevLevel, "⏪ LEVEL SEBELUMNYA (1)", false);

            drawHeaderButton(c, rectCheatClose, "✖️ TUTUP MENU", 0xFFFF5252);
        }

        private void drawCheatOption(Canvas c, RectF r, String label, boolean active) {
            Paint fill = new Paint(btnNormalPaint);
            if (active) fill.setColor(0xAA009688);
            c.drawRoundRect(r, 10, 10, fill);
            c.drawRoundRect(r, 10, 10, active ? btnBorderGlowPaint : btnBorderPaint);

            textPaint.setTextSize(r.height() * 0.44f);
            float ty = r.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f;
            c.drawText(label, r.centerX(), ty, textPaint);
        }

        private e getActiveGameCanvas() {
            Display disp = Display.getInstance();
            if (disp != null && disp.getCurrent() instanceof e) {
                return (e) disp.getCurrent();
            }
            return null;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            int pointerCount = event.getPointerCount();

            // Cheat modal touch handling
            if (showCheatModal) {
                if (action == MotionEvent.ACTION_DOWN) {
                    float tx = event.getX();
                    float ty = event.getY();
                    e game = getActiveGameCanvas();

                    if (rectCheatGodMode.contains(tx, ty)) {
                        if (game != null) {
                            game.x = !game.x;
                            showCheat(game.x ? "GOD MODE: AKTIF" : "GOD MODE: MATI");
                        }
                        return true;
                    } else if (rectCheatFlyMode.contains(tx, ty)) {
                        if (game != null) {
                            // Trigger cheat 787899
                            dispatchKey(55, true); dispatchKey(55, false);
                            dispatchKey(56, true); dispatchKey(56, false);
                            dispatchKey(55, true); dispatchKey(55, false);
                            dispatchKey(56, true); dispatchKey(56, false);
                            dispatchKey(57, true); dispatchKey(57, false);
                            dispatchKey(57, true); dispatchKey(57, false);
                            showCheat("FLY MODE AKTIF! (Tekan Jump / 5 untuk terbang)");
                        }
                        return true;
                    } else if (rectCheatSuperSpeed.contains(tx, ty)) {
                        dispatchKey(35, true);
                        dispatchKey(35, false);
                        showCheat("SUPER SPEED WARP (#)!");
                        return true;
                    } else if (rectCheatNextLevel.contains(tx, ty)) {
                        dispatchKey(51, true);
                        dispatchKey(51, false);
                        showCheat("LEVEL SELANJUTNYA (3)");
                        return true;
                    } else if (rectCheatPrevLevel.contains(tx, ty)) {
                        dispatchKey(49, true);
                        dispatchKey(49, false);
                        showCheat("LEVEL SEBELUMNYA (1)");
                        return true;
                    } else if (rectCheatClose.contains(tx, ty) || !rectCheatBox.contains(tx, ty)) {
                        showCheatModal = false;
                        triggerVibrate(25);
                        return true;
                    }
                }
                return true;
            }

            // Mode switch and cheat toggle on ACTION_DOWN
            if (action == MotionEvent.ACTION_DOWN) {
                float tx = event.getX();
                float ty = event.getY();
                if (rectCheatBtn.contains(tx, ty)) {
                    showCheatModal = true;
                    triggerVibrate(30);
                    return true;
                }
                if (rectModeSwitch.contains(tx, ty)) {
                    controlMode = (controlMode == 0) ? 1 : 0;
                    layoutControls(getWidth(), getHeight());
                    triggerVibrate(30);
                    return true;
                }
            }

            // Button multi-touch dispatch
            boolean[] nextPressed = new boolean[buttons.size()];

            if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
                for (int i = 0; i < pointerCount; i++) {
                    float px = event.getX(i);
                    float py = event.getY(i);

                    for (int bIdx = 0; bIdx < buttons.size(); bIdx++) {
                        HudButton btn = buttons.get(bIdx);
                        if (!btn.rect.isEmpty() && btn.rect.contains(px, py)) {
                            nextPressed[bIdx] = true;
                        }
                    }
                }
            }

            for (int bIdx = 0; bIdx < buttons.size(); bIdx++) {
                HudButton btn = buttons.get(bIdx);
                boolean wasPressed = btn.isPressed;
                boolean isNowPressed = nextPressed[bIdx];

                if (wasPressed != isNowPressed) {
                    btn.isPressed = isNowPressed;
                    dispatchKey(btn.keyCode, isNowPressed);
                }
            }

            return true;
        }

        private void showCheat(String msg) {
            cheatToast = msg;
            cheatToastTime = System.currentTimeMillis();
            triggerVibrate(40);
        }

        private void triggerVibrate(int ms) {
            if (vibrator != null) {
                try { vibrator.vibrate(ms); } catch (Exception ignored) {}
            }
        }
    }
}
