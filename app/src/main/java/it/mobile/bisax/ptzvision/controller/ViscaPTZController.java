package it.mobile.bisax.ptzvision.controller;

import android.util.Log;
import android.util.Pair;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

import it.mobile.bisax.ptzvision.controller.utils.HexConverter;
import it.mobile.bisax.ptzvision.controller.viscacommands.AutoTracking;
import it.mobile.bisax.ptzvision.controller.viscacommands.Focus;
import it.mobile.bisax.ptzvision.controller.viscacommands.PanTilt;
import it.mobile.bisax.ptzvision.controller.viscacommands.Scene;
import it.mobile.bisax.ptzvision.controller.viscacommands.Zoom;
import it.mobile.bisax.ptzvision.data.cam.Cam;

public class ViscaPTZController implements PTZController, Closeable {
    private final Socket socket;
    private final OutputStream output;
    private final Cam cam;
    
    private final boolean DEBUG = false;


    public ViscaPTZController(Cam cam) throws IOException {
        this.cam = cam;
        this.socket = new Socket(cam.getIp(), cam.getControlPort());
        this.output = socket.getOutputStream();
    }
    @Override
    public Result move(float panf, float tiltf) {
        int pan = (int) (panf * 24);
        int tilt = (int) (tiltf * 20);

        if(DEBUG) Log.d("ViscaPTZController", "Move command: pan=" + pan + ", tilt=" + tilt);

        byte[] moveCommand;
        if (pan < 0) {
            if (tilt < 0) {
                moveCommand = PanTilt.downLeft(-pan, -tilt);
            } else if (tilt > 0) {
                moveCommand = PanTilt.upLeft(-pan, tilt);
            } else {
                moveCommand = PanTilt.left(-pan, 20);
            }
        } else if (pan > 0) {
            if (tilt < 0) {
                moveCommand = PanTilt.downRight(pan, -tilt);
            } else if (tilt > 0) {
                moveCommand = PanTilt.upRight(pan, tilt);
            } else {
                moveCommand = PanTilt.right(pan, 20);
            }
        } else {
            if (tilt < 0) {
                moveCommand = PanTilt.down(24, -tilt);
            } else if (tilt > 0) {
                moveCommand = PanTilt.up(24, tilt);
            } else {
                moveCommand = PanTilt.stop();
            }
        }

        return runCommand(moveCommand);
    }

    @Override
    public Result zoom(float zoomf) {
        int zoom = (int) (zoomf * 7);

        if(DEBUG) Log.d("ViscaPTZController", "Zoom command: zoom=" + zoom);

        byte[] zoomCommand;

        if (zoom < 0) {
            zoomCommand = Zoom.wide(-zoom);
        } else if (zoom > 0) {
            zoomCommand = Zoom.tele(zoom);
        } else {
            zoomCommand = Zoom.stop();
        }

        return runCommand(zoomCommand);
    }

    @Override
    public Result focus(float focusf) {
        int focus = (int) (focusf * 7);

        if(DEBUG) Log.d("ViscaPTZController", "Focus command: focus=" + focus);

        if (focus<0) {
            return runCommand(Focus.far(-focus));
        } else if (focus>0) {
            return runCommand(Focus.near(focus));
        } else {
            return runCommand(Focus.stop());
        }
    }

    @Override
    public Result setAutoFocus(boolean autoFocus) {
        if(DEBUG) Log.d("ViscaPTZController", "AutoFocus command: autoFocus=" + autoFocus);

        if (autoFocus) {
            return runCommand(Focus.auto());
        } else {
            return runCommand(Focus.manual());
        }
    }

    @Override
    public Result setAutoTracking(boolean autoTracking){
        if (autoTracking) {
            return runCommand(AutoTracking.on());
        } else {
            return runCommand(AutoTracking.off());
        }
    }

    @Override
    public Result setScene(int scene) {
        return runCommand(Scene.set(scene));
    }

    @Override
    public Result callScene(int scene) {
        return runCommand(Scene.call(scene));
    }

    @Override
    public Pair<Result, Boolean> getAutoFocus() {
        // HARD TO IMPLEMENT
        try (HttpCgiPTZController tmp = new HttpCgiPTZController(cam)) {
            return tmp.getAutoFocus();
        } catch (Exception e) {
            return new Pair<>(Result.NOT_SUPPORTED, null);
        }
    }

    @Override
    public Pair<Result, Boolean> getAutoTracking() {
        // NOT SUPPORTED
        try (HttpCgiPTZController tmp = new HttpCgiPTZController(cam)) {
            return tmp.getAutoTracking();
        } catch (Exception e) {
            return new Pair<>(Result.NOT_SUPPORTED, null);
        }
    }

    @Override
    public Pair<Result, Double> getZoom() {
        Pair<Result, Object> result = runCommandWithResponse(Zoom.get());
        if (result.first == Result.SUCCESS && result.second != null) {
            String response = (String) result.second;
            if (response.length() != 14) {
                return new Pair<>(Result.FAILURE, 0.0);
            }
            String zoom = ""+response.charAt(5)+response.charAt(7)+response.charAt(9)+response.charAt(11);
            int zoomHex = Integer.parseInt(zoom, 16);
            double zoomValue = zoomHex / 16384.0 * 29.0 + 1.0;
            zoomValue = Math.round(zoomValue * 100.0) / 100.0;
            return new Pair<>(Result.SUCCESS, zoomValue);
        } else {
            return new Pair<>(Result.FAILURE, 0.0);
        }
    }

    private Result runCommand(byte[] command) {
        try {
            this.output.write(command);
        } catch (IOException e) {
            Log.e("ViscaPTZController", "Failed to send command: " + HexConverter.parseHex(command));
            return Result.FAILURE;
        }
        return Result.SUCCESS;
    }

    private String readResponse() {
        try {
            InputStream input = this.socket.getInputStream();
            byte[] response = new byte[1024];
            int bytesRead = input.read(response);
            if (bytesRead == -1) return null;
            String responseString = HexConverter.parseHex(Arrays.copyOf(response, bytesRead));
            if(DEBUG) Log.d("ViscaPTZController", "Response: " + responseString);
            return responseString;
        } catch (IOException e) {
            return null;
        }
    }

    private Pair<Result, Object> runCommandWithResponse(byte[] command) {
        Result result = runCommand(command);

        if (result == Result.SUCCESS) {
            return new Pair<>(Result.SUCCESS, readResponse());
        } else {
            Log.e("ViscaPTZController", "Failed to run command with response: " + HexConverter.parseHex(command));
            return new Pair<>(Result.FAILURE, null);
        }
    }

    @Override
    public void close() throws IOException {
        this.output.close();
        this.socket.close();
    }
}
