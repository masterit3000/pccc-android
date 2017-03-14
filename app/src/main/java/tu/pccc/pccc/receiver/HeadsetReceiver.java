package tu.pccc.pccc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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

public class HeadsetReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            int status = intent.getIntExtra("state", -1);

            JsonObject json = new JsonObject();
            String imei = CF.read(context, SysConstraints.KEY_IMEI, "");
            if (imei.length() > 0) {
                json.addProperty("imei", imei);
                json.addProperty("status", status);
                TblLogDAO tblLogDAO = new TblLogDAO(context);
                tblLogDAO.insertLog("Dây tai nghe", status == 1 ? "Đã cắm" : "Đã rút");
                Ion.with(context)
                        .load(SysConstraints.SERVICE_URL + "/DeviceRoute/DeviceHeadSetPlug")
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
}
