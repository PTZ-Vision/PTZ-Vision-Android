package it.mobile.bisax.ptzvision.controller;

import android.util.Log;
import android.util.Pair;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.Closeable;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import it.mobile.bisax.ptzvision.data.cam.Cam;

public class HttpCgiPTZController implements PTZController, Closeable {
    private final String ptzUrl;
    private final String paramUrl;
    private final CloseableHttpClient connection;
    public HttpCgiPTZController(Cam cam) {
        ptzUrl = "http://" + cam.getIp() + "/cgi-bin/ptz.cgi";
        paramUrl = "http://" + cam.getIp() + "/cgi-bin/param.cgi";

        Log.d("HttpCgiPTZController", "PTZ URL: " + ptzUrl);
        Log.d("HttpCgiPTZController", "Param URL: " + paramUrl);

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
    public Result focus(float focusf) {
        int focus = (int) (focusf * 7);

        Log.d("HttpCgiPTZController", "Focus command: focus=" + focus);
        if(focus < 0){
            return sendCommand("?ptzcmd=focusout&" + -focus);
        } else if(focus > 0){
            return sendCommand("?ptzcmd=focusin&" + focus);
        } else {
            return sendCommand("?ptzcmd=focusstop&0");
        }
    }

    @Override
    public Result setAutoFocus(boolean autoFocus) {
        Log.d("HttpCgiPTZController", "AutoFocus command: autoFocus=" + autoFocus);

        if (autoFocus) {
            return sendCommand("?post_image_value&focusmode&2");
        } else {
            return sendCommand("?post_image_value&focusmode&0");
        }
    }

    @Override
    public Result setAutoTracking(boolean autoTracking) {
        return Result.NOT_SUPPORTED;
    }

    @Override
    public Result setScene(int scene) {
        Log.d("HttpCgiPTZController", "SetScene command: scene=" + scene);

        return sendCommand("?ptzcmd=posset&" + scene);
    }

    @Override
    public Result callScene(int scene) {
        Log.d("HttpCgiPTZController", "CallScene command: scene=" + scene);

        return sendCommand("?ptzcmd=poscall&" + scene);
    }

    @Override
    public Pair<Result, Boolean> getAutoFocus() {
        Log.d("HttpCgiPTZController", "GetAutoFocus command");

        Pair <Result, String> response = getResponse("?get_advance_image_conf", "focus_mode");
        if (response.first == Result.SUCCESS) {
            return Pair.create(Result.SUCCESS, response.second.equals("2"));
        } else {
            return Pair.create(Result.FAILURE, false);
        }
    }

    @Override
    public Pair<Result, Boolean> getAutoTracking() {
        Log.d("HttpCgiPTZController", "GetAutoTracking command");

        Pair <Result, String> response = getResponse("?get_image_conf", "autotrack");
        if (response.first == Result.SUCCESS) {
            return Pair.create(Result.SUCCESS, response.second.equals("1"));
        } else {
            return Pair.create(Result.FAILURE, false);
        }
    }

    @Override
    public Pair<Result, Double> getZoom() {
        return Pair.create(Result.NOT_SUPPORTED, null);
    }

    private Result sendCommand(String command){
        return sendCommand(command, false);
    }
    private  Result sendCommand(String command, boolean isPost){
        HttpRequestBase request;
        if (isPost) {
            request = new HttpPost(ptzUrl + command);
        } else {
            request = new HttpGet(ptzUrl + command);
        }

        try {
            connection.execute(request);
        } catch (IOException e) {
            Log.e("HttpCgiPTZController", "Failed to send command", e);
            return Result.FAILURE;
        }

        return Result.SUCCESS;
    }

    private Pair<Result, String> getResponse(String command, String key){
        try {
            CloseableHttpResponse response = connection.execute(new HttpGet(paramUrl + command));
            String responseString = response.getEntity().toString();
            return Pair.create(Result.SUCCESS, extractValue(responseString, key));
        } catch (IOException e) {
            return Pair.create(Result.FAILURE, null);
        }
    }

    private static String extractValue(String response, String key) {
        Pattern pattern = Pattern.compile(key + "=\"(\\d+)\"");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}