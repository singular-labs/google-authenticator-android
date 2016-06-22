package net.singular.authenticator;

import android.content.Context;
import android.preference.PreferenceManager;

public class SingularPreferences {
    public static final String PSK_KEY = "PSK_KEY";
    public static final String REMOTE_FCM_ID_KEY = "remoteFCMId";
    private final Context context;

    public SingularPreferences(Context context) {
        this.context = context;
    }

    private void setKey(String key, String value){
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }

    private String getKey(String key){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
    }

    public String getPSK(){
        return getKey(PSK_KEY);
    }

    public void setPSK(String psk){
        setKey(PSK_KEY, psk);

    }

    public String getRemoteFCMId(){
        return getKey(REMOTE_FCM_ID_KEY);
    }

    public void setRemoteFCMId(String remoteFCMId){
        setKey(REMOTE_FCM_ID_KEY, remoteFCMId);
    }
}
