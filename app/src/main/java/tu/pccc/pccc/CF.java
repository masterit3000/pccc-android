package tu.pccc.pccc;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Base64;
import android.util.Log;

/**
 * Created by ThanhND on 2/15/17.
 */

public class CF {
    public static void sendSMS(String phoneNo, String msg) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Log.d("SAC", "TEST GUI SMS: " + phoneNo + " - " + msg);
    }

    public static String getBasicAuthUsernameAndPassword() {
        String basicUserNameAndPass = SysConstraints.BASIC_AUTH_USERNAME + ": " + SysConstraints.BASIC_AUTH_PASSWORD;
        return "Basic " + new String(Base64.encode(basicUserNameAndPass.getBytes(), Base64.NO_WRAP));
    }

    public static void write(Context c, String key, String value) {

        SharedPreferences sharedPref = c.getSharedPreferences("PCCC", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String read(Context c, String key, String defaultValue) {
        SharedPreferences sharedPref = c.getSharedPreferences("PCCC", Context.MODE_PRIVATE);
        return sharedPref.getString(key, defaultValue);
    }
}
