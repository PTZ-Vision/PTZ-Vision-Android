/*
    By Rainer Jung
    https://gitlab.com/lightingcontrol/experiments/visca/-/blob/main/src/main/java/io/gitlab/lightingcontrol/visca/command/util/ByteParam.java
 */

package it.mobile.bisax.ptzvision.controller.utils;

import java.text.MessageFormat;

/**
 * Define a byte parameter with its limits, but let the developer simply enter int values.
 * <p>
 * This is a helper class for the command classes to easily verify their parameters.
 */
public class ByteParam {

    private final String name;
    private final int min;
    private final int max;

    /**
     * Define a byte parameter by {@code name} (for error information) and their {@code min} and {@code max} values.
     *
     * @param name The name of the parameter (will only be used for error handling)
     * @param min  The minimum value of the parameter (&gt;= 0 &amp; &lt;= max)
     * @param max  The maximum value of the parameter (&lt;239 &amp; &gt;= min)
     */
    public ByteParam(String name, int min, int max) {
        if (min < 0 || min > 239) {
            throw new IllegalArgumentException("Parameter min needs to be within 0…239");
        }
        if (max < 0 || max > 239) {
            throw new IllegalArgumentException("Parameter max needs to be within 0…239");
        }
        if (min > max) {
            throw new IllegalArgumentException("Parameter min cannot be greater than max");
        }

        this.name = name;
        this.min = min;
        this.max = max;
    }

    /**
     * Retrieve the {@code byte} value if it's valid with the given {@code min} and {@code max} values.
     *
     * @param value An integer value which should fit within {@code min} and {@code max}
     * @return The byte value of the {@code value} parameter (if it's within the allowed range)
     * @throws IllegalArgumentException When the value parameter does exceed the range
     */
    public byte toByte(int value) throws IllegalArgumentException {
        if (value < min || value > max) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid value for {0} (needs to be within {1} and {2})", name, min, max));
        }
        return (byte) value;
    }

}
