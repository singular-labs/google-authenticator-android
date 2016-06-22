package net.singular.authenticator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.singular.authenticator.testability.DependencyInjector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SingularFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SingularFMS";
    public static final int GET_CODE_REQ_CODE = 1000;
    public static final int REJECT_CODE_REQ_CODE = 1001;
    private AccountDb mAccountDb;


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Map<String, String> msg_data = remoteMessage.getData();
        String src = msg_data.get("src");

        // todo: verify that src matches the paired src
        try {
            JSONObject value = new JSONObject(msg_data.get("value"));
            String command = value.getString("command");
            Log.d(TAG, "command: " + command);
            switch(command){
                case "getAccounts":
                    handleGetAccounts(src);
                    break;
                case "getCode":
                    int id = value.getInt("id");
                    handleGetCode(src, id);
                    break;
                default:
                    Log.e(TAG, "unknown command: " + command);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }


    private void handleGetCode(String from, int id) {
        Log.d(TAG, "handleGetCode: " + Integer.toString(id));
        sendNotification(id, from);
    }

    private void handleGetAccounts(String from) {
        Log.d(TAG, "handleGetAccounts");
        mAccountDb = DependencyInjector.getAccountDb();
        ArrayList<String> usernames = new ArrayList<>();
        mAccountDb.getNames(usernames);

        List<HashMap<String, Object>> accountList = new ArrayList<>();

        for(int i = 0; i < usernames.size(); ++i){
            HashMap<String, Object> account = new HashMap<>();

            account.put("name", usernames.get(i));
            account.put("id", i);

            accountList.add(account);
        }

        // need to prepare a looper in the GCM thread so that the AsyncHttpClient works
        Looper.prepare();

        SingularFCMProxyProtocol p = new SingularFCMProxyProtocol(FirebaseInstanceId.getInstance().getToken(), from);
        p.sendAccountList(accountList);

        Log.d(TAG, "handleGetAccounts: response = " + accountList.toString());
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param id
     * @param from
     */
    private void sendNotification(int id, String from) {
        Intent intent = new Intent(this, AuthenticatorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        SingularCodeUtils singularCodeUtils = DependencyInjector.getSingularCodeUtils();
        String accountName = singularCodeUtils.getUsername(id);
        Spanned notificationText = Html.fromHtml(String.format("For account <b><i>%s</i></b>.", accountName));

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Code Request")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_btn_next)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .addAction(R.drawable.ic_btn_back, "approve",
                        PendingIntent.getBroadcast(this, GET_CODE_REQ_CODE,
                                getPendingIntent(id, from, true),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_btn_back, "reject",
                        PendingIntent.getBroadcast(this, REJECT_CODE_REQ_CODE,
                                getPendingIntent(id, from, false),
                                PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentIntent(pendingIntent);


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    @NonNull
    private Intent getPendingIntent(int id, String from, boolean approve) {
        Intent approveIntent = new Intent(this, SingularBroadcastReceiver.class);
//        approveIntent.setAction("pasten");
        approveIntent.putExtra("from", from);
        approveIntent.putExtra("accountId", id);
        approveIntent.putExtra("approve", approve);
        return approveIntent;
    }
}