package tu.pccc.pccc.db.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ThanhND on 3/1/17.
 */

public class Db extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "PCCC.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String TBL_SMS_CREATE = "CREATE TABLE if not exists TBL_SMS (ID integer primary key AUTOINCREMENT,PHONE_NO TEXT NULL, NAME TEXT NULL);";
    private static final String TBL_LOG_CREATE = "CREATE TABLE if not exists TBL_LOG (ID integer primary key AUTOINCREMENT,LOG_TYPE TEXT NULL, LOG_DESC TEXT NULL, LOG_AT TEXT NULL);";

    public Db(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {

        database.execSQL(TBL_SMS_CREATE);
        database.execSQL(TBL_LOG_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS TBL_SMS;");
        db.execSQL("DROP TABLE IF EXISTS TBL_LOG;");

        onCreate(db);
    }
}
