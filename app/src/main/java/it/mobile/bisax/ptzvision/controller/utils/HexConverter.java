package it.mobile.bisax.ptzvision.controller.utils;

import java.util.Objects;

public class HexConverter {
    private static final byte[] EMPTY_BYTES = {};
    private static final byte[] DIGITS = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0,  1,  2,  3,  4,  5,  6,  7,  8,  9, -1, -1, -1, -1, -1, -1,
            -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };

    public static byte[] parseHex(CharSequence string) {
        Objects.requireNonNull(string, "string");

        if (string.length() == 0) return EMPTY_BYTES;

        if ((string.length() & 1) != 0)
            throw new IllegalArgumentException("string length not even: " +
                    string.length());

        byte[] bytes = new byte[string.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) fromHexDigits(string, i * 2);
        }

        return bytes;
    }

    private static int fromHexDigits(CharSequence string, int index) {
        int high = fromHexDigit(string.charAt(index));
        int low = fromHexDigit(string.charAt(index + 1));
        return (high << 4) | low;
    }

    public static int fromHexDigit(int ch) {
        int value;
        if ((ch >>> 8) == 0 && (value = DIGITS[ch]) >= 0) {
            return value;
        }
        throw new NumberFormatException("not a hexadecimal digit: \"" + (char) ch + "\" = " + ch);
    }

    public static String parseHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
