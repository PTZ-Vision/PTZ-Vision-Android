package it.mobile.bisax.ptzvision.controller.viscacommands;

import java.nio.ByteBuffer;

import it.mobile.bisax.ptzvision.controller.utils.ByteParam;
import it.mobile.bisax.ptzvision.controller.utils.HexConverter;

public class PanTilt {
    private static final ByteParam PARAM_TILT_SPEED = new ByteParam("tiltSpeed", 0, 20);
    private static final ByteParam PARAM_PAN_SPEED = new ByteParam("panSpeed", 0, 24);
    private static final byte[] HEX_MOVEMENT_PREFIX = HexConverter.parseHex("81010601");

    public static byte[] up(int tiltSpeed) {
        return up(24, tiltSpeed);
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] up(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        // 0301FF
        buffer.put(HexConverter.parseHex("0301FF"));
        return buffer.array();
    }

    // tiltSpeed should be 1-20
    // panSpeed is 24
    public static byte[] down(int tiltSpeed) {
        return down(24, tiltSpeed);
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] down(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0302FF"));
        return buffer.array();
    }


    // panSpeed should be 1-24
    // tiltSpeed is 20
    public static byte[] left(int panSpeed) {
        return left(panSpeed, (byte) 20);
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] left(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0103FF"));
        return buffer.array();
    }

    // panSpeed should be 1-24
    // tiltSpeed is 20
    public static byte[] right(int panSpeed) {
        return right(panSpeed, (byte) 20);
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] right(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0203FF"));
        return buffer.array();
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] upLeft(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0101FF"));
        return buffer.array();
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] upRight(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0201FF"));
        return buffer.array();
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] downLeft(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0102FF"));
        return buffer.array();
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] downRight(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0202FF"));
        return buffer.array();
    }

    // panSpeed is 24
    // tiltSpeed is 20
    public static byte[] stop() {
        return stop((byte) 24, (byte) 20);
    }

    // panSpeed should be 1-24
    // tiltSpeed should be 1-20
    public static byte[] stop(int panSpeed, int tiltSpeed) {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put(HEX_MOVEMENT_PREFIX);
        buffer.put(PARAM_PAN_SPEED.toByte(panSpeed));
        buffer.put(PARAM_TILT_SPEED.toByte(tiltSpeed));
        buffer.put(HexConverter.parseHex("0303FF"));
        return buffer.array();
    }

    public static byte[] home() {
        return HexConverter.parseHex("81010604FF");
    }

    public static byte[] reset() {
        return HexConverter.parseHex("81010605FF");
    }


}


