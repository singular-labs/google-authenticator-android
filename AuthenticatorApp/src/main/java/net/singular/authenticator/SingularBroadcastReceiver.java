package net.singular.authenticator;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import net.singular.authenticator.testability.DependencyInjector;

public class SingularBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = "SBR";
    @Override
    public void onReceive(Context context, Intent intent) {
        int accountId = intent.getIntExtra("accountId", -1);
        boolean approve = intent.getBooleanExtra("approve", false);
        String from = intent.getStringExtra("from");

        if(approve){
            sendCode(accountId, from);
        }else{
            sendReject(accountId, from);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    private void sendReject(int accountId, String from) {
        SingularFCMProxyProtocol p = new SingularFCMProxyProtocol(FirebaseInstanceId.getInstance().getToken(), from);
        p.reject(accountId);
        Log.d(TAG, "reject: accountId = " + accountId);
    }

    private void sendCode(int accountId, String from){
        SingularCodeUtils singularCodeUtils = DependencyInjector.getSingularCodeUtils();
        String username = singularCodeUtils.getUsername(accountId);
        try {
            OtpSource mOtpProvider = DependencyInjector.getOtpProvider();
            String code = mOtpProvider.getNextCode(username);
            SingularFCMProxyProtocol p = new SingularFCMProxyProtocol(FirebaseInstanceId.getInstance().getToken(), from);
            p.sendCode(accountId, code);
            Log.d(TAG, "sendCode: code = " + code);
        } catch (OtpSourceException ignored) {
            ignored.printStackTrace();
        }
    }
}
