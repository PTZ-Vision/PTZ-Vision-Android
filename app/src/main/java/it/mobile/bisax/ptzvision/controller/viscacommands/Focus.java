package it.mobile.bisax.ptzvision.controller.viscacommands;

import java.nio.ByteBuffer;

import it.mobile.bisax.ptzvision.controller.utils.ByteParam;
import it.mobile.bisax.ptzvision.controller.utils.HexConverter;
import it.mobile.bisax.ptzvision.controller.utils.MultiByteParam;

public class Focus {
    private static final ByteParam PARAM_FOCUS_SPEED = new ByteParam("focusSpeed", 0, 7);
    private static final byte[] HEX_FOCUS_PREFIX = HexConverter.parseHex("81010408");

    public static byte[] far() {
        return HexConverter.parseHex("8101040802FF");
    }
    public static byte[] far(int focusSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(HEX_FOCUS_PREFIX);
        buffer.put((byte) (PARAM_FOCUS_SPEED.toByte(focusSpeed) | (byte) 0x20));
        buffer.put(HexConverter.parseHex("FF"));
        return buffer.array();
    }

    public static byte[] near() {
        return HexConverter.parseHex("8101040803FF");
    }

    public static byte[] near(int focusSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(HEX_FOCUS_PREFIX);
        buffer.put((byte) (PARAM_FOCUS_SPEED.toByte(focusSpeed) | (byte) 0x30));
        buffer.put(HexConverter.parseHex("FF"));
        return buffer.array();
    }

    public static byte[] auto() {
        return HexConverter.parseHex("8101043802FF");
    }
    public static byte[] manual() {
        return HexConverter.parseHex("8101043803FF");
    }

    public static byte[] stop() {
        return HexConverter.parseHex("8101040800FF");
    }

    public static byte[] getAutoFocus() {
        return HexConverter.parseHex("81090438FF");
    }

}
