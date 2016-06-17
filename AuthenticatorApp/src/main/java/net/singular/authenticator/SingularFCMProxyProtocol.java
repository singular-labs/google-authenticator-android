package net.singular.authenticator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.json.*;
import com.loopj.android.http.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class SingularFCMProxyProtocol {
    private AsyncHttpClient client = new AsyncHttpClient();
    private static final String FCMProxyEndpoint = "http://10.0.2.2:1119/send";

    private String mLocalRegistrationID;
    private String mRemoteRegistrationID;

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
                    System.out.println(response);
                }
            });
        } catch (JSONException e)
        {

        }
        catch (UnsupportedEncodingException e)
        {

        }
    }

    public void sendAccountList(ArrayList accountList)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "accountList");
            params.put("accounts", accountList);

            this.postJSON(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    System.out.println(response);
                }
            });
        } catch (JSONException e)
        {

        }
        catch (UnsupportedEncodingException e)
        {

        }
    }

    public void sendCode(String value, String id)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "code");
            params.put("code", value);
            params.put("id", id);

            this.postJSON(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    System.out.println(response);
                }
            });
        } catch (JSONException e)
        {

        }
        catch (UnsupportedEncodingException e)
        {

        }
    }

    public void reject(String id)
    {
        try {
            JSONObject params = new JSONObject();

            params.put("command", "reject");
            params.put("id", id);

            this.postJSON(params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    System.out.println(response);
                }
            });
        } catch (JSONException e)
        {

        }
        catch (UnsupportedEncodingException e)
        {

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
