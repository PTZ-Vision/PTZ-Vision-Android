package it.mobile.bisax.ptzvision.controller;

import android.util.Pair;

public interface PTZController {
    Result move(float pan, float tilt);
    Result zoom(float zoom);
    Result focus(float focus);
    Result setAutoFocus(boolean autoFocus);
    Result setAutoTracking(boolean autoTracking);

    Result setScene(int scene);
    Result callScene(int scene);

    Pair<Result, Boolean> getAutoFocus();
    Pair<Result, Boolean> getAutoTracking();
    Pair<Result, Double> getZoom();

    enum Result {
        SUCCESS,
        FAILURE,
        NOT_SUPPORTED
    }
}
