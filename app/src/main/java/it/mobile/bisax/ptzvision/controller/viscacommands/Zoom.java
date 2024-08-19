package it.mobile.bisax.ptzvision.controller.viscacommands;

import java.nio.ByteBuffer;

import it.mobile.bisax.ptzvision.controller.utils.ByteParam;
import it.mobile.bisax.ptzvision.controller.utils.HexConverter;
import it.mobile.bisax.ptzvision.controller.utils.MultiByteParam;

public class Zoom {

    private static final ByteParam PARAM_ZOOM_SPEED = new ByteParam("zoomSpeed", 0, 7);
    private static final MultiByteParam PARAM_ZOOM = new MultiByteParam("zoom", 4, 0, 16384);

    private Zoom() {
    }

    public static byte[] stop() {
        return HexConverter.parseHex("8101040700FF");
    }

    public static byte[] tele() {
        return HexConverter.parseHex("8101040702FF");
    }

    public static byte[] wide() {
        return HexConverter.parseHex("8101040703FF");
    }

    // zoomSpeed should be 0…7
    public static byte[] tele(int zoomSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(HexConverter.parseHex("81010407"));
        buffer.put((byte) (PARAM_ZOOM_SPEED.toByte(zoomSpeed) | (byte) 0x20));
        buffer.put(HexConverter.parseHex("FF"));
        return buffer.array();
    }

    // zoomSpeed should be 0…7
    public static byte[] wide(int zoomSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.put(HexConverter.parseHex("81010407"));
        buffer.put((byte) (PARAM_ZOOM_SPEED.toByte(zoomSpeed) | (byte) 0x30));
        buffer.put(HexConverter.parseHex("FF"));
        return buffer.array();
    }

    public static byte[] direct(int zoom) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HexConverter.parseHex("81010447"));
        buffer.put(PARAM_ZOOM.toByte(zoom));
        buffer.put(HexConverter.parseHex("FF"));
        return buffer.array();
    }

    public static byte[] get() {
        return HexConverter.parseHex("81090447FF");
    }

}
