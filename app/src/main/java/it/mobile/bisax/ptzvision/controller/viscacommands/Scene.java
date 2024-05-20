package it.mobile.bisax.ptzvision.controller.viscacommands;

import java.nio.ByteBuffer;

import it.mobile.bisax.ptzvision.controller.utils.ByteParam;
import it.mobile.bisax.ptzvision.controller.utils.HexConverter;

public class Scene {

    private static final ByteParam PARAM_SCENE = new ByteParam("scene", 0, 254);
    private static final byte[] HEX_SCENE_PREFIX = HexConverter.parseHex("8101043F");

    public static byte[] set(int scene) {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.put(HEX_SCENE_PREFIX);
        buffer.put(HexConverter.parseHex("01"));
        buffer.put(PARAM_SCENE.toByte(scene));
        buffer.put(HexConverter.parseHex("FF"));
        return buffer.array();
    }

    public static byte[] call(int scene) {
        ByteBuffer buffer = ByteBuffer.allocate(7);
        buffer.put(HEX_SCENE_PREFIX);
        buffer.put(HexConverter.parseHex("02"));
        buffer.put(PARAM_SCENE.toByte(scene));
        buffer.put(HexConverter.parseHex("FF"));
        return buffer.array();
    }
}
