package net.singular.authenticator;

import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import net.singular.authenticator.testability.DependencyInjector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SingularCodeUtils {
    private final String TAG = "SingularCodeUtils";
    public String getUsername(int id){
        AccountDb mAccountDb = DependencyInjector.getAccountDb();
        ArrayList<String> usernames = new ArrayList<String>();
        mAccountDb.getNames(usernames);
        return usernames.get(id);
    }

    public void sendAccounts(Context context) {
        String from = getMyFCMId();
        if(from == null){
            Log.d(TAG, "sendAccount: FirebaseCloudMessagingId = null, doing nothing");
            return;
        }
        String remoteFCMId = new SingularPreferences(context).getRemoteFCMId();
        AccountDb mAccountDb = DependencyInjector.getAccountDb();
        ArrayList<String> usernames = new ArrayList<>();
        mAccountDb.getNames(usernames);

        List<HashMap<String, Object>> accountList = new ArrayList<>();

        for(int i = 0; i < usernames.size(); ++i){
            HashMap<String, Object> account = new HashMap<>();

            account.put("name", usernames.get(i));
            account.put("id", i);

            accountList.add(account);
        }

        SingularFCMProxyProtocol p = new SingularFCMProxyProtocol(from, remoteFCMId);
        p.sendAccountList(accountList);

        Log.d(TAG, "sendAccounts: response = " + accountList.toString());
    }

    public String getMyFCMId(){
        return FirebaseInstanceId.getInstance().getToken();
    }

}