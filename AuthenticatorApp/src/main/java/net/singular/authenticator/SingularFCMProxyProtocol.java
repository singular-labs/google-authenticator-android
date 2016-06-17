package net.singular.authenticator;

import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.*;
import com.loopj.android.http.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class SingularFCMProxyProtocol {
    private AsyncHttpClient client = new AsyncHttpClient();
    private static final String FCMProxyEndpoint = "http://192.168.1.6:8080/send";

    private String mLocalRegistrationID;
    private String mRemoteRegistrationID;

    private static final String TAG = "SingularFCMProxy";

    SingularFCMProxyProtocol(String localRegistrationID, String remoteRegistrationID)
    {
        this.mLocalRegistrationID = localRegistrationID;
        this.mRemoteRegistrationID = remoteRegistrationID;
    }

    public void sendHello()
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "Hello");

            this.postJSON(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d(TAG, response.toString());
                }
            });
        } catch (JSONException e)
        {
            Log.e(TAG, "JSONException");
        }
        catch (UnsupportedEncodingException e)
        {
            Log.e(TAG, "UnsupportedEncodingException");
        }
    }

    public void sendAccountList(List accountList)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "accountList");
            params.put("accounts", new JSONArray(accountList));

            this.postJSON(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d(TAG, response.toString());
                }
            });
        } catch (JSONException e)
        {
            Log.e(TAG, "JSONException");
        }
        catch (UnsupportedEncodingException e)
        {
            Log.e(TAG, "UnsupportedEncodingException");
        }
    }

    public void sendCode(int id, String value)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "code");
            params.put("code", value);
            params.put("id", id);

            this.postJSON(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d(TAG, response.toString());
                }
            });
        } catch (JSONException e)
        {
            Log.e(TAG, "JSONException");
        }
        catch (UnsupportedEncodingException e)
        {
            Log.e(TAG, "UnsupportedEncodingException");
        }
    }

    public void reject(int id)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "reject");
            params.put("id", id);

            this.postJSON(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d(TAG, response.toString());
                }
            });
        } catch (JSONException e)
        {
            Log.e(TAG, "JSONException");
        }
        catch (UnsupportedEncodingException e)
        {
            Log.e(TAG, "UnsupportedEncodingException");
        }
    }

    public void postJSON(JSONObject params, JsonHttpResponseHandler handler) throws JSONException, UnsupportedEncodingException
    {
        JSONObject proxy_packet = new JSONObject();

        proxy_packet.put("to", this.mRemoteRegistrationID);
        proxy_packet.put("from", this.mLocalRegistrationID);
        proxy_packet.put("message", params.toString());

        StringEntity se = new StringEntity(proxy_packet.toString());

        this.client.post(null, SingularFCMProxyProtocol.FCMProxyEndpoint, se, "application/json", handler);
    }
}
