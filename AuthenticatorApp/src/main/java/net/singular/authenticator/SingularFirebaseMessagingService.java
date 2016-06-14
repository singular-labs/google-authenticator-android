package net.singular.authenticator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class SingularFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SingularFMS";

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
                    handleGetAccount();
                    break;
                case "getCode":
                    int id = value.getInt("id");
                    handleGetCode(id);
                    break;
                default:
                    Log.e(TAG, "unknown command: " + command);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
    }

    private void handleGetCode(int id) {
        Log.d(TAG, "handleGetCode: " + Integer.toString(id));
    }

    private void handleGetAccount() {
        Log.d(TAG, "handleGetAccount");
    }
    // [END receive_message]

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