package net.singular.authenticator;

import android.app.Activity;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Callback;

public class SingularFCMProxyProtocol {
    private final String psk;
    private OkHttpClient client;
    private static final String FCMProxyEndpoint = "https://twofactor.singular.net/two_factor_router";

    private String mLocalRegistrationID;
    private String mRemoteRegistrationID;

    private static final String TAG = "SingularFCMProxy";
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");


    SingularFCMProxyProtocol(String localRegistrationID, String remoteRegistrationID, String psk)
    {
        this.client = new OkHttpClient();
        this.mLocalRegistrationID = localRegistrationID;
        this.mRemoteRegistrationID = remoteRegistrationID;
        this.psk = psk;
    }

    public void sendHello()
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "Hello");

            this.postJSON(params);
        } catch (Exception e)
        {
            Log.e(TAG, "sendHello failed", e);
        }
    }

    public void sendAccountList(List accountList)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "accountList");
            params.put("accounts", new JSONArray(accountList));

            this.postJSON(params);
        } catch (Exception e)
        {
            Log.e(TAG, "sendAccountsList failed", e);
        }
    }

    public void sendCode(int id, String value)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "code");
            params.put("value", value);
            params.put("id", id);

            this.postJSON(params);
        } catch (Exception e)
        {
            Log.e(TAG, "sendCode failed", e);
        }
    }

    public void reject(int id)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "reject");
            params.put("id", id);

            this.postJSON(params);
        } catch (Exception e)
        {
            Log.e(TAG, "reject exception", e);
        }
    }

    public void postJSON(JSONObject params) throws JSONException, CryptoException {
        JSONObject proxy_packet = new JSONObject();

        proxy_packet.put("to", this.mRemoteRegistrationID);
        proxy_packet.put("from", this.mLocalRegistrationID);
        String message = SingularCodeUtils.encrypt(this.psk, params.toString());
        proxy_packet.put("message", message);
        proxy_packet.put("version", 1);

        RequestBody body = RequestBody.create(JSON, proxy_packet.toString());
        Request request = new Request.Builder()
                .url(FCMProxyEndpoint)
                .post(body)
                .build();
        Callback callback = new Callback() {

            @Override public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override public void onResponse(Call call, Response response) {
                Log.d(TAG, response.toString());
                response.close();
            }
        };

        client.newCall(request).enqueue(callback);
    }
}
