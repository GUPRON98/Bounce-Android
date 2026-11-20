package com.nokia.mid.sound;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class Sound {
    public static final int SOUND_PLAYING = 0;
    public static final int SOUND_STOPPED = 1;
    public static final int SOUND_UNINITIALIZED = 2;
    public static final int FORMAT_TONE = 1;
    public static final int FORMAT_WAV = 2;

    private static ToneGenerator toneGen;
    private int state = SOUND_STOPPED;
    private int freq = 800;
    private long duration = 100;

    static {
        try {
            toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
        } catch (Throwable ignored) {}
    }

    public Sound(byte[] data, int type) {
        init(data, type);
    }

    public Sound(int freq, long duration) {
        init(freq, duration);
    }

    public void init(byte[] data, int type) {
        this.state = SOUND_STOPPED;
        if (data != null && data.length > 0) {
            this.freq = 700 + (data.length * 50) % 800;
            this.duration = 80;
        }
    }

    public void init(int freq, long duration) {
        this.freq = freq;
        this.duration = duration;
        this.state = SOUND_STOPPED;
    }

    public void play(int loop) {
        this.state = SOUND_PLAYING;
        if (toneGen != null) {
            try {
                int toneType = ToneGenerator.TONE_PROP_BEEP;
                if (freq > 1000) toneType = ToneGenerator.TONE_PROP_PROMPT;
                else if (freq < 600) toneType = ToneGenerator.TONE_PROP_ACK;
                toneGen.startTone(toneType, (int) Math.min(duration, 300));
            } catch (Throwable ignored) {}
        }
    }

    public void stop() {
        this.state = SOUND_STOPPED;
        if (toneGen != null) {
            try {
                toneGen.stopTone();
            } catch (Throwable ignored) {}
        }
    }

    public void release() {
        stop();
    }

    public void resume() {}
    public int getState() { return state; }
    public void setGain(int gain) {}
}
