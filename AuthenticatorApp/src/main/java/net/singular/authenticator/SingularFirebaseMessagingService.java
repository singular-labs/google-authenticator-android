package net.singular.authenticator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import net.singular.authenticator.testability.DependencyInjector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class SingularFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "SingularFMS";
    public static final int GET_CODE_REQ_CODE = 1000;
    public static final int REJECT_CODE_REQ_CODE = 1001;
    private final SingularPreferences singularPreferences;


    public SingularFirebaseMessagingService() {
        singularPreferences = new SingularPreferences(this);
    }

    private void requestUpdate(){
        Intent intent = new Intent(this, AuthenticatorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Push Authenticator")
                .setContentText("Your version is outdated, please update to continue using it")
                .setSmallIcon(R.drawable.ic_btn_next);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

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
        String remoteFCMId = singularPreferences.getRemoteFCMId();
        SingularCodeUtils singularCodeUtils = DependencyInjector.getSingularCodeUtils();
        String from = remoteMessage.getFrom();
        Log.d(TAG, "From: " + from);
        Map<String, String> msgData = remoteMessage.getData();
        String src = msgData.get("src");
        int version = Integer.parseInt(msgData.get("version"));
        if(version > SingularCodeUtils.PROTOCOL_VERSION){
            Log.e(TAG, String.format("got message with protocol version > supported version, " +
                    "got version = %d, we support version = %d",
                    version,
                    SingularCodeUtils.PROTOCOL_VERSION));
            requestUpdate();
            return;
        }

        if(!src.equals(remoteFCMId)){
            Log.e(TAG, String.format("got message from an unknown FCM id = '%s' " +
                    "while paired with = '%s'", src, remoteFCMId));
            return;
        }

        // todo: verify that src matches the paired src
        try {
            String encryptedValue = msgData.get("value");
            String decryptedValue = SingularCodeUtils.decrypt(
                    singularPreferences.getPSK(), encryptedValue);
            JSONObject value = new JSONObject(decryptedValue);
            String command = value.getString("command");
            Log.d(TAG, "command: " + command);
            switch(command){
                case "getAccounts":
                    singularCodeUtils.sendAccounts(this);
                    break;
                case "getCode":
                    int id = value.getInt("id");
                    handleGetCode(src, id);
                    break;
                default:
                    Log.e(TAG, "unknown command: " + command);
            }
        } catch (Exception e) {
            Log.e(TAG, "onMessageReceived failed", e);
            return;
        }
    }


    private void handleGetCode(String from, int id) {
        Log.d(TAG, "handleGetCode: " + Integer.toString(id));
        sendNotification(id, from);
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


        NotificationCompat.Action approveAction = getAction(id, from, "Approve", R.drawable.ic_btn_back, true);
        NotificationCompat.Action rejectAction = getAction(id, from, "Reject", R.drawable.ic_btn_back, false);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("Code Request")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_btn_next)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new  long[] {1})
                .addAction(approveAction)
                .addAction(rejectAction)
                .setContentIntent(pendingIntent);

        notificationBuilder.extend(new NotificationCompat.WearableExtender().
                addAction(approveAction).
                addAction(rejectAction));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notificationBuilder.setPriority(Notification.PRIORITY_MAX);
        }


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }

    @NonNull
    private NotificationCompat.Action getAction(int id, String from, String title, int icon, boolean approve) {
        return new NotificationCompat.Action.Builder(
                    icon, title,
                    PendingIntent.getBroadcast(this, GET_CODE_REQ_CODE,
                            getIntent(id, from, approve),
                            PendingIntent.FLAG_UPDATE_CURRENT)).build();
    }

    @NonNull
    private Intent getIntent(int id, String from, boolean approve) {
        Intent approveIntent = new Intent(this, SingularBroadcastReceiver.class);
        if(approve){
            approveIntent.setAction("approve");
        }else{
            approveIntent.setAction("reject");
        }
        approveIntent.putExtra("from", from);
        approveIntent.putExtra("accountId", id);
        approveIntent.putExtra("approve", approve);
        return approveIntent;
    }
}