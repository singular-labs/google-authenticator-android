package net.singular.authenticator;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import net.singular.authenticator.testability.DependencyInjector;

import org.json.JSONObject;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.digests.SHA256Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


class CryptoException extends Exception {
    public Exception wrapped_exception;
    //Parameterless Constructor
    public CryptoException() {}

    public CryptoException(String message){
        super(message);
    }

    //Constructor that accepts a message
    public CryptoException(String message, Exception e)
    {
        super(message);
        wrapped_exception = e;
    }
}

public class SingularCodeUtils {
    private static final String TAG = "SingularCodeUtils";
    public static final int PBKDF_ITER_COUNT = 1000;
    public static final int KEY_BIT_LEN = 128;
    public static final int PROTOCOL_VERSION = 1;

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
        SingularPreferences singularPreferences = new SingularPreferences(context);
        String remoteFCMId = singularPreferences.getRemoteFCMId();
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

        SingularFCMProxyProtocol p = new SingularFCMProxyProtocol(
                from, remoteFCMId, singularPreferences.getPSK());
        p.sendAccountList(accountList);

        Log.d(TAG, "sendAccounts: response = " + accountList.toString());
    }

    public String getMyFCMId(){
        return FirebaseInstanceId.getInstance().getToken();
    }

    private static byte[] innerDecrypt(SecretKey key, byte[] encrypted, byte[] iv) throws Exception{
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(encrypted);
    }

    public static SecretKey generateKey(char[] passphraseOrPin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Number of PBKDF2 hardening rounds to use. Larger values increase
        // computation time. You should select a value that causes computation
        // to take >100ms.
        final int iterations = PBKDF_ITER_COUNT;

        // Generate a 128-bit key
        final int outputKeyLength = KEY_BIT_LEN;

        PKCS5S2ParametersGenerator generator;
        generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
        generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(passphraseOrPin), salt, iterations);
        KeyParameter key = (KeyParameter) generator.generateDerivedMacParameters(outputKeyLength);
        return new SecretKeySpec(key.getKey(), "AES");
    }

    private static byte[] decodeBase64(String encoded){
        return Base64.decode(encoded, Base64.DEFAULT);
    }

    private static String encodeBase64(byte[] data){
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public static String decrypt(String key, String data) throws CryptoException {
        try{
            JSONObject decodedObject = new JSONObject(data);
            // Generate key from password
            String saltEncoded = decodedObject.getString("salt");
            byte[] salt = decodeBase64(saltEncoded);
            SecretKey sharedKey = getSaltedKey(key, salt);

            // Decrypt the ticket
            String ctEncoded = decodedObject.getString("ct");
            String ivEncoded = decodedObject.getString("iv");
            byte[] decryptedTicketBytes = innerDecrypt(sharedKey,
                    decodeBase64(ctEncoded),
                    decodeBase64(ivEncoded));
            return new String(decryptedTicketBytes);
        }catch (Exception e){
            Log.e(SingularCodeUtils.TAG, "decrypt exception", e);
            throw new CryptoException("wrapped", e);
        }

    }

    private static SecretKey getSaltedKey(String key, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] keyCharArray = key.toCharArray();
        return generateKey(keyCharArray, salt);
    }

    public static String encrypt(String key, String data) throws CryptoException {
        try {
            JSONObject result = new JSONObject();
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[8];
            random.nextBytes(salt);
            SecretKey sharedKey = getSaltedKey(key, salt);

            byte[] iv = new byte[12];
            random.nextBytes(iv);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, sharedKey, ivSpec);
            byte[] dataBytes = data.getBytes();
            byte[] encryptedBytes = cipher.doFinal(dataBytes);

            result.put("iv", encodeBase64(iv));
            result.put("v", 1);
            result.put("iter", PBKDF_ITER_COUNT);
            result.put("ks", KEY_BIT_LEN);
            result.put("ts", 64);
            result.put("mode", "ccm");
            result.put("adata", "");
            result.put("cipher", "aes");
            result.put("salt", encodeBase64(salt));
            result.put("ct", encodeBase64(encryptedBytes));

            return result.toString();
        }catch (Exception e){
            Log.e(SingularCodeUtils.TAG, "encrypt exception", e);
            throw new CryptoException("wrapped", e);
        }
    }

}