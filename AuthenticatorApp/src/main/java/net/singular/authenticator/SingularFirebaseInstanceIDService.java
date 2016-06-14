package net.singular.authenticator;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class SingularFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "SingularFIIDService";
    private String mRegistrationID = null;

    @Override
    public void onTokenRefresh() {
        mRegistrationID = FirebaseInstanceId.getInstance().getToken();

        Log.d(TAG, "Refreshed token: " + mRegistrationID);

    }
}
