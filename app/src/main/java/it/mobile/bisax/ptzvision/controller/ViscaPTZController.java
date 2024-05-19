package it.mobile.bisax.ptzvision.controller;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import it.mobile.bisax.ptzvision.controller.viscacommands.AutoTracking;
import it.mobile.bisax.ptzvision.controller.viscacommands.PanTilt;
import it.mobile.bisax.ptzvision.controller.viscacommands.Zoom;
import it.mobile.bisax.ptzvision.data.cam.Cam;

public class ViscaPTZController implements PTZController, Closeable {
    private final Socket socket;
    private final OutputStream output;


    public ViscaPTZController(Cam cam) throws IOException {
        this.socket = new Socket(cam.getIp(), cam.getPort());
        this.output = socket.getOutputStream();
    }
    @Override
    public Result move(float panf, float tiltf) {
        int pan = (int) (panf * 24);
        int tilt = (int) (tiltf * 20);

        Log.d("ViscaPTZController", "Move command: pan=" + pan + ", tilt=" + tilt);

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

        Log.d("ViscaPTZController", "Zoom command: zoom=" + zoom);

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
    public Result focus(int focus) {
        // TODO: implement this method
        return Result.NOT_SUPPORTED;
    }

    @Override
    public Result setAutoFocus(boolean autoFocus) {
        // TODO: implement this method
        return Result.NOT_SUPPORTED;
    }

    @Override
    public Result setAutoTracking(boolean autoTracking){
        if (autoTracking) {
            return runCommand(AutoTracking.on());
        } else {
            return runCommand(AutoTracking.off());
        }
    }

    private Result runCommand(byte[] command) {
        try {
            this.output.write(command);
        } catch (IOException e) {
            Log.d("ViscaPTZController", "Failed to send command: " + command.toString());
            return Result.FAILURE;
        }
        return Result.SUCCESS;
    }

    @Override
    public void close() throws IOException {
        this.output.close();
        this.socket.close();
    }
}
