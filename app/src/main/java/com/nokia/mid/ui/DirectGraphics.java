package com.nokia.mid.ui;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class DirectGraphics {
    public static final int FLIP_HORIZONTAL = 8192;
    public static final int FLIP_VERTICAL = 16384;
    public static final int ROTATE_90 = 90;
    public static final int ROTATE_180 = 180;
    public static final int ROTATE_270 = 270;

    public static final int TYPE_BYTE_1_GRAY = 1;
    public static final int TYPE_BYTE_1_GRAY_VERTICAL = 2;
    public static final int TYPE_BYTE_2_GRAY = 4;
    public static final int TYPE_BYTE_4_GRAY = 8;
    public static final int TYPE_BYTE_8_GRAY = 16;
    public static final int TYPE_BYTE_332_RGB = 32;
    public static final int TYPE_USHORT_4444_ARGB = 64;
    public static final int TYPE_USHORT_555_RGB = 128;
    public static final int TYPE_USHORT_565_RGB = 256;
    public static final int TYPE_INT_888_RGB = 512;
    public static final int TYPE_INT_8888_ARGB = 1024;

    private Graphics g;

    public DirectGraphics(Graphics g) {
        this.g = g;
    }

    public void setARGBColor(int argbColor) {
        g.setColor(argbColor);
    }

    public void drawImage(Image img, int x, int y, int anchor, int manipulation) {
        if (img == null || img.getBitmap() == null) return;
        Bitmap b = img.getBitmap();

        int posX = x + g.getTranslateX();
        int posY = y + g.getTranslateY();

        int w = b.getWidth();
        int h = b.getHeight();

        if ((anchor & Graphics.HCENTER) != 0) posX -= w / 2;
        else if ((anchor & Graphics.RIGHT) != 0) posX -= w;

        if ((anchor & Graphics.VCENTER) != 0) posY -= h / 2;
        else if ((anchor & Graphics.BOTTOM) != 0) posY -= h;

        if (manipulation == 0) {
            g.getCanvas().drawBitmap(b, posX, posY, null);
            return;
        }

        Matrix m = new Matrix();
        if ((manipulation & FLIP_HORIZONTAL) != 0) {
            m.postScale(-1, 1);
            m.postTranslate(w, 0);
        }
        if ((manipulation & FLIP_VERTICAL) != 0) {
            m.postScale(1, -1);
            m.postTranslate(0, h);
        }

        int rot = manipulation & (ROTATE_90 | ROTATE_180 | ROTATE_270);
        if (rot == ROTATE_90) {
            m.postRotate(90);
            m.postTranslate(h, 0);
        } else if (rot == ROTATE_180) {
            m.postRotate(180);
            m.postTranslate(w, h);
        } else if (rot == ROTATE_270) {
            m.postRotate(270);
            m.postTranslate(0, w);
        }

        m.postTranslate(posX, posY);
        g.getCanvas().drawBitmap(b, m, null);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        g.setColor(argbColor);
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x2, y2, x3, y3);
        g.drawLine(x3, y3, x1, y1);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int argbColor) {
        drawTriangle(x1, y1, x2, y2, x3, y3, argbColor);
    }

    public void drawPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) {}
    public void fillPolygon(int[] xPoints, int xOffset, int[] yPoints, int yOffset, int nPoints, int argbColor) {}

    public void drawPixels(int[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) {
        if (pixels == null || width <= 0 || height <= 0) return;
        Bitmap bmp = Bitmap.createBitmap(pixels, offset, scanlength, width, height, Bitmap.Config.ARGB_8888);
        Image img = new Image(bmp);
        drawImage(img, x, y, 0, manipulation);
    }

    public void drawPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) {}
    public void drawPixels(short[] pixels, boolean transparency, int offset, int scanlength, int x, int y, int width, int height, int manipulation, int format) {}
    public void getPixels(byte[] pixels, byte[] transparencyMask, int offset, int scanlength, int x, int y, int width, int height, int format) {}
    public void getPixels(short[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format) {}
    public void getPixels(int[] pixels, int offset, int scanlength, int x, int y, int width, int height, int format) {}
}
