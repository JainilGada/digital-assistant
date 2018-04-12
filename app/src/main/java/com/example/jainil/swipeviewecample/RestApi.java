package com.example.jainil.swipeviewecample; /**
 * Created by jainil on 4/2/18.
 */
import com.loopj.android.http.*;

public class RestApi {
    private static final String BASE_URL = "http://172.16.31.66:8000";

    private static AsyncHttpClient client = new AsyncHttpClient();
    final static int DEFAULT_TIMEOUT = 40 * 1000;

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.setTimeout(DEFAULT_TIMEOUT);
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.setTimeout(DEFAULT_TIMEOUT);
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
