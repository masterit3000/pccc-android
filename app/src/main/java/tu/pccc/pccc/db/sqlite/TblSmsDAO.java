package tu.pccc.pccc.db.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ThanhND on 3/1/17.
 */

public class TblSmsDAO {
    private Context c;

    public TblSmsDAO(Context c) {
        this.c = c;
    }

    public List<String> getAllSMSToSend() {
        List<String> lstSms = new ArrayList<String>();

        Db d = new Db(c);
        SQLiteDatabase readableDatabase = d.getWritableDatabase();
        Cursor rawQuery = readableDatabase.rawQuery("select * from TBL_SMS", null);

        while (rawQuery.moveToNext()) {
            lstSms.add(rawQuery.getString(1));
        }
        rawQuery.close();
        d.close();

        return lstSms;
    }

    public void insertSms(String smsJson) {
        JSONArray ja = null;
        try {
            ja = new JSONArray(smsJson);
            Db d = new Db(c);
            for (int i = 0; i < ja.length(); i++) {
                JSONObject jo = ja.getJSONObject(i);
                if (!isExistsPhoneNumber(d, jo.getString("phoneNo"))) {
                    SQLiteDatabase writableDatabase = d.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("PHONE_NO", jo.getString("phoneNo"));
                    values.put("NAME", jo.getString("name"));
                    writableDatabase.insert("TBL_SMS", null, values);
                }
            }
            d.close();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private boolean isExistsPhoneNumber(Db d, String phoneNo) {
        boolean retVal = false;

        SQLiteDatabase readableDatabase = d.getWritableDatabase();
        Cursor rawQuery = readableDatabase.rawQuery("select count(PHONE_NO) from TBL_SMS where PHONE_NO = '" + phoneNo + "';", null);
        int count = 0;
        while (rawQuery.moveToNext()) {
            count = rawQuery.getInt(0);
        }
        rawQuery.close();

        if (count != 0) {
            retVal = true;
        }
        return retVal;
    }
}
