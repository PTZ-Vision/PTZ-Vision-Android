package it.mobile.bisax.ptzvision.controller;

public interface PTZController {
    Result move(float pan, float tilt);
    Result zoom(float zoom);
    Result focus(float focus);
    Result setAutoFocus(boolean autoFocus);
    Result setAutoTracking(boolean autoTracking);

    Result setScene(int scene);
    Result callScene(int scene);

    enum Result {
        SUCCESS,
        FAILURE,
        NOT_SUPPORTED
    }
}
