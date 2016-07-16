package net.singular.authenticator;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.singular.authenticator.testability.DependencyInjector;

public class SingularBroadcastReceiver extends BroadcastReceiver {
    private final String TAG = "SBR";
    @Override
    public void onReceive(Context context, Intent intent) {
        int accountId = intent.getIntExtra("accountId", -1);
        boolean approve = intent.getBooleanExtra("approve", false);
        String from = intent.getStringExtra("from");
        SingularPreferences singularPreferences = new SingularPreferences(context);
        SingularFCMProxyProtocol singularFCMProxyProtocol = new SingularFCMProxyProtocol(
                from,
                singularPreferences.getPSK());
        if(approve){
            sendCode(singularFCMProxyProtocol, accountId);
        }else{
            sendReject(singularFCMProxyProtocol, accountId);
        }


        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    private void sendReject(SingularFCMProxyProtocol singularFCMProxyProtocol, int accountId) {
        singularFCMProxyProtocol.reject(accountId);
        Log.d(TAG, "reject: accountId = " + accountId);
    }

    private void sendCode(SingularFCMProxyProtocol singularFCMProxyProtocol, int accountId){
        SingularCodeUtils singularCodeUtils = DependencyInjector.getSingularCodeUtils();
        String username = singularCodeUtils.getUsername(accountId);
        try {
            OtpSource mOtpProvider = DependencyInjector.getOtpProvider();
            String code = mOtpProvider.getNextCode(username);

            singularFCMProxyProtocol.sendCode(accountId, code);
            Log.d(TAG, "sendCode: code = " + code);
        } catch (OtpSourceException ignored) {
            ignored.printStackTrace();
        }
    }
}
