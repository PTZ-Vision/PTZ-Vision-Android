/*
    By Rainer Jung
    https://gitlab.com/lightingcontrol/experiments/visca/-/blob/main/src/main/java/io/gitlab/lightingcontrol/visca/command/util/MultiByteParam.java
 */
package it.mobile.bisax.ptzvision.controller.utils;

import java.text.MessageFormat;

/**
 * Define a multiple byte parameter with its limits, but let the developer simply enter int values.
 * <p>
 * This is a helper class for the command classes to easily verify their parameters.
 */
public class MultiByteParam {

    private final int bytes;
    private final String name;
    private final int min;
    private final int max;

    /**
     * Define a multibyte parameter by {@code name} (for error information), the number of {@code bytes} and their
     * {@code min} and {@code max} values.
     *
     * @param name  The name of the parameter (will only be used for error handling)
     * @param bytes The number of returning bytes the parameter (will only be used for error handling)
     * @param min   The minimum value of the parameter (&gt;= 0 &amp; &lt;= max)
     * @param max   The maximum value of the parameter (&lt;=16^bytes &amp; &gt;= min)
     */
    public MultiByteParam(String name, int bytes, int min, int max) {
        if (bytes < 2 || bytes > 4) {
            throw new IllegalArgumentException("Parameter bytes needs to be within 2…4");
        }
        int maxValue = (int) Math.pow(0x0f, bytes);
        if (min < 0 || min > maxValue) {
            throw new IllegalArgumentException(MessageFormat.format("Parameter min needs to be within 0…{0}", maxValue));
        }
        if (max < 0 || max > maxValue) {
            throw new IllegalArgumentException(MessageFormat.format("Parameter min needs to be within 0…{0}", maxValue));
        }
        if (min > max) {
            throw new IllegalArgumentException("Parameter min cannot be greater than max");
        }

        this.name = name;
        this.bytes = bytes;
        this.min = min;
        this.max = max;
    }

    /**
     * Retrieve the {@code bytes} value if it's valid with the given {@code min} and {@code max} values.
     * <p>
     * Visca encodes multibyte values with bytes only with values from {@code 0x00-0x0f}.
     *
     * @param value An integer value which should fit within {@code min} and {@code max}
     * @return The bytes value of the {@code value} parameter (if it's within the allowed range)
     * @throws IllegalArgumentException When the value parameter does exceed the range
     */
    public byte[] toByte(int value) throws IllegalArgumentException {
        if (value < min || value > max) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid value for {0} (needs to be within {1} and {2})", name, min, max));
        }

        byte[] result = new byte[this.bytes];

        for (int i = 0; i < this.bytes; i++) {
            result[bytes - i - 1] = (byte) (value & 0xf);
            value = value >> 4;
        }

        return result;
    }

}
