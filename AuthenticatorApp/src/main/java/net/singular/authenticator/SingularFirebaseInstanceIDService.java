package net.singular.authenticator;

import android.content.Intent;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class SingularFirebaseInstanceIDService extends FirebaseInstanceIdService {
    private String mRegistrationID = null;

    @Override
    public void onTokenRefresh() {
        mRegistrationID = FirebaseInstanceId.getInstance().getToken();

        Intent i = new Intent(getApplicationContext(), AuthenticatorActivity.class);
        i.putExtra("RegistrationID", mRegistrationID);

        startActivity(i);
    }
}
