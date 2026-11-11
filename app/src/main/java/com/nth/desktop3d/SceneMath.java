package com.nth.desktop3d;

/** Pure scene calculations shared by rendering, input and unit tests. */
public final class SceneMath {
    private SceneMath() {}
    public static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
    public static float smooth(float from, float to, float seconds, float rate) {
        return from + (to - from) * (1f - (float) Math.exp(-Math.max(0, seconds) * rate));
    }
    public static int wrap(int page, int count) { return ((page % count) + count) % count; }
    public static float[] constrain(float x, float y, float vx, float vy, float width, float height, float size) {
        float maxX = Math.max(0, width - size), maxY = Math.max(0, height - size);
        if (x < 0) { x = 0; vx = Math.abs(vx) * .5f; }
        if (x > maxX) { x = maxX; vx = -Math.abs(vx) * .5f; }
        if (y < 0) { y = 0; vy = Math.abs(vy) * .5f; }
        if (y > maxY) { y = maxY; vy = -Math.abs(vy) * .5f; }
        return new float[]{x, y, vx, vy};
    }
    public static float tilt(float radians) { return clamp(radians / .45f, -1f, 1f); }
}
