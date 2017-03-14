package tu.pccc.pccc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Message;
import android.util.Log;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import tu.pccc.pccc.CF;
import tu.pccc.pccc.SysConstraints;
import tu.pccc.pccc.db.sqlite.TblLogDAO;

/**
 * Created by ThanhND on 3/1/17.
 */

public class PlugInControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int status = 0;
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            // Do something when power connected
            status = 1;
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            // Do something when power disconnected
            status = 0;
        }

        JsonObject json = new JsonObject();
        String imei = CF.read(context, SysConstraints.KEY_IMEI, "");
        if (imei.length() > 0) {
            json.addProperty("imei", imei);
            json.addProperty("status", status);
            TblLogDAO tblLogDAO = new TblLogDAO(context);
            tblLogDAO.insertLog("Dây sạc", status == 1 ? "Đã cắm" : "Đã rút");
            Ion.with(context)
                    .load(SysConstraints.SERVICE_URL + "/DeviceRoute/DevicePowerCordStateChanged")
                    .basicAuthentication(SysConstraints.BASIC_AUTH_USERNAME, SysConstraints.BASIC_AUTH_PASSWORD)
                    .setJsonObjectBody(json)
                    .asJsonObject()

                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {

                        }

                    });
        }


    }
}
