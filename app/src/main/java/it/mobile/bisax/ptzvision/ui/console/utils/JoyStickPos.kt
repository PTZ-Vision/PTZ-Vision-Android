package it.mobile.bisax.ptzvision.ui.console.utils

enum class JoyStickDir {
    NONE,
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UP_LEFT,
    UP_RIGHT,
    DOWN_LEFT,
    DOWN_RIGHT
}

// pair of x and y coordinates
data class JoyStickPos(val direction: JoyStickDir, val intensity: Float) {
    override fun toString(): String {
        return "JoyStickPos(direction=$direction, intensity=$intensity)"
    }
}