package net.singular.authenticator;

import android.test.AndroidTestCase;

import org.json.JSONObject;

/**
 * Created by rubyf on 15/07/16.
 */
public class SingularCodeUtilsTest extends AndroidTestCase {

    public static final String DUMMY_PASSWORD = "password";

    public void testDecrypt() throws Exception {
        String key = DUMMY_PASSWORD;
        String data = "{\"iv\":\"7MIi02W6D3qFm6lY\",\"v\":1,\"iter\":1000,\"ks\":128,\"ts\":64,\"mode\":\"ccm\",\"adata\":\"\",\"cipher\":\"aes\",\"salt\":\"MQ0P0qofxTI=\",\"ct\":\"MgP5Uj3dsqPL0IWX\"}";
        String result = SingularCodeUtils.decrypt(key, data);
        String expected = "data";
        assertEquals(expected, result);
    }

    public void testEncrypt() throws Exception {
        String plain = "data2";
        String resultEncoded = SingularCodeUtils.encrypt(DUMMY_PASSWORD, plain);
        assertEquals(plain, SingularCodeUtils.decrypt(DUMMY_PASSWORD, resultEncoded));
    }

}
