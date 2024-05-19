package it.mobile.bisax.ptzvision.controller;

import android.util.Log;

import org.apache.http.HttpConnection;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.Closeable;
import java.io.IOException;

import it.mobile.bisax.ptzvision.data.cam.Cam;

public class HttpCgiPTZController implements PTZController, Closeable {
    private final String url;
    private final CloseableHttpClient connection;
    public HttpCgiPTZController(Cam cam) throws IOException {
        url = "http://" + cam.getIp() + "/cgi-bin/ptz.cgi";
        connection = HttpClients.createDefault();
    }
    @Override
    public Result move(float panf, float tiltf) {
        int pan = (int) (panf * 24);
        int tilt = (int) (tiltf * 20);

        Log.d("HttpCgiPTZController", "Move command: pan=" + pan + ", tilt=" + tilt);

        String params;
        if (pan < 0) {
            if (tilt < 0) {
                params = "?ptzcmd=leftdown&pan=" + -pan + "&tilt=" + -tilt;
            } else if (tilt > 0) {
                params = "?ptzcmd=leftup&pan=" + -pan + "&tilt=" + tilt;
            } else {
                params = "?ptzcmd=left&pan=" + -pan + "&tilt=20";
            }
        } else if (pan > 0) {
            if (tilt < 0) {
                params = "?ptzcmd=rightdown&pan=" + pan + "&tilt=" + -tilt;
            } else if (tilt > 0) {
                params = "?ptzcmd=rightup&pan=" + pan + "&tilt=" + tilt;
            } else {
                params = "?ptzcmd=right&pan=" + pan + "&tilt=20";
            }
        } else {
            if (tilt < 0) {
                params = "?ptzcmd=down&pan=24&tilt=" + -tilt;
            } else if (tilt > 0) {
                params = "?ptzcmd=up&pan=24&tilt=" + tilt;
            } else {
                params = "?ptzcmd=stop&pan=24&tilt=20";
            }
        }

        return sendCommand(params);
    }

    @Override
    public Result zoom(float zoomf) {
        int zoom = (int) (zoomf * 7);

        Log.d("HttpCgiPTZController", "Zoom command: zoom=" + zoom);
        if(zoom < 0){
            return sendCommand("?ptzcmd=zoomout&zoom=" + -zoom);
        } else {
            return sendCommand("?ptzcmd=zoomin&zoom=" + zoom);
        }
    }

    @Override
    public Result focus(int focus) {
        return Result.NOT_SUPPORTED;
    }

    @Override
    public Result setAutoFocus(boolean autoFocus) {
        return Result.NOT_SUPPORTED;
    }

    @Override
    public Result setAutoTracking(boolean autoTracking) {
        return Result.NOT_SUPPORTED;
    }

    private Result sendCommand(String command){
        return sendCommand(command, false);
    }
    private  Result sendCommand(String command, boolean isPost){
        HttpRequestBase request;
        if (isPost) {
            request = new HttpPost(url + command);
        } else {
            request = new HttpGet(url + command);
        }

        try {
            connection.execute(request);
        } catch (IOException e) {
            Log.e("HttpCgiPTZController", "Failed to send command", e);
            return Result.FAILURE;
        }

        return Result.SUCCESS;
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
