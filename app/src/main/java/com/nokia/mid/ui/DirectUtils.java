package com.nokia.mid.ui;

import android.graphics.Bitmap;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class DirectUtils {
    public static DirectGraphics getDirectGraphics(Graphics g) {
        return new DirectGraphics(g);
    }

    public static Image createImage(int width, int height, int ARGBcolor) {
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        b.eraseColor(ARGBcolor);
        return new Image(b);
    }

    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
        return Image.createImage(imageData, imageOffset, imageLength);
    }
}
