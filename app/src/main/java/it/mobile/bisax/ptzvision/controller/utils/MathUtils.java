package it.mobile.bisax.ptzvision.controller.utils;

public class MathUtils {
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clampUnit(float value) {
        return clamp(value, -1f, 1f);
    }
}
