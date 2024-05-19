package it.mobile.bisax.ptzvision.controller.viscacommands;

import it.mobile.bisax.ptzvision.controller.utils.HexConverter;

public class AutoTracking {
    public static byte[] on() {
        return HexConverter.parseHex("810A115402FF");
    }
    public static byte[] off() {
        return HexConverter.parseHex("810A115403FF");
    }
}