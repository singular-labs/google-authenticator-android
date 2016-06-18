package net.singular.authenticator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
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
    private AccountDb mAccountDb;
    private OtpSource mOtpProvider;

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
        mOtpProvider = DependencyInjector.getOtpProvider();
        try {
            String code = mOtpProvider.getNextCode(getUsername(id));

            SingularFCMProxyProtocol p = new SingularFCMProxyProtocol(FirebaseInstanceId.getInstance().getToken(), from);
            p.sendCode(id, code);

            Log.d(TAG, "handleGetCode: code = " + code);
        } catch (OtpSourceException ignored) {
            ignored.printStackTrace();
        }

    }

    private String getUsername(int id){
        mAccountDb = DependencyInjector.getAccountDb();
        ArrayList<String> usernames = new ArrayList<String>();
        mAccountDb.getNames(usernames);
        return usernames.get(id);
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
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, AuthenticatorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("FCM Message")
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}