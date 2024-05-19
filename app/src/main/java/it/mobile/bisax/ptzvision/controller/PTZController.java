package it.mobile.bisax.ptzvision.controller;

public interface PTZController {
    Result move(float pan, float tilt);
    Result zoom(float zoom);
    Result focus(int focus);
    Result setAutoFocus(boolean autoFocus);
    Result setAutoTracking(boolean autoTracking);

    enum Result {
        SUCCESS,
        FAILURE,
        NOT_SUPPORTED
    }
}
