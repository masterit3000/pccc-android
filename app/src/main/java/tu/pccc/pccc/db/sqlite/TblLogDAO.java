package tu.pccc.pccc.db.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Message;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import tu.pccc.pccc.CF;
import tu.pccc.pccc.SysConstraints;

/**
 * Created by ThanhND on 3/1/17.
 */

public class TblLogDAO {
    private Context c;

    public TblLogDAO(Context c) {
        this.c = c;
    }

    public void insertLog(String logType, String logDesc) {
        Db d = new Db(c);
        long currentTimeMillis = System.currentTimeMillis();

        SQLiteDatabase writableDatabase = d.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("LOG_TYPE", logType);
        values.put("LOG_DESC", logDesc);
        values.put("LOG_AT", currentTimeMillis);

        writableDatabase.insert("TBL_LOG", null, values);
        d.close();

        //Post log to server ..DeviceRoute/InsertDeviceLog
        JsonObject json = new JsonObject();
        json.addProperty("imei", CF.read(c, SysConstraints.KEY_IMEI, ""));
        json.addProperty("markerId", CF.read(c, SysConstraints.KEY_MARKER_ID, ""));
        json.addProperty("markerName", CF.read(c, SysConstraints.KEY_NAME, ""));
        json.addProperty("logType", logType);
        json.addProperty("logDesc", logDesc);
        json.addProperty("logDate", currentTimeMillis);

        Ion.with(c)
                .load(SysConstraints.SERVICE_URL + "/DeviceRoute/InsertDeviceLog")
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
