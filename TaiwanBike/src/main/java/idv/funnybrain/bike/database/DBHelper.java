package idv.funnybrain.bike.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by Freeman on 2014/2/16.
 */
public class DBHelper extends SQLiteOpenHelper implements IDBHelper{
    private static final String TAG = "DBHelper";
    private static final boolean D = false;

    //private static final String DB_PATH = "/data/data/idv.funnybrain.bike/databases/";
    private static final String DB_NAME = "station.db";
    private static final int DB_VERSION = 1;
    public static final String DB_TABLE = "bike";
    //public static final String DB_COL_ID = "_id";
    //public static final String DB_COL_STATION_ID = "station_id";

    private SQLiteDatabase db;

    private Context mContext = null;

    private static final String DB_CREATE = "CREATE TABLE " + DB_TABLE + " (" +
                                            DB_COL_ID + " INTEGER PRIMARY KEY," +
                                            DB_COL_STATION_ID + " TEXT NOT NULL UNIQUE)";

    private SQLiteDatabase sqLiteDatabase = null;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        if(D) {
            Log.d(TAG, "constructor");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if(D) Log.d(TAG, "onCreate");
        this.db = db;
        db.execSQL(DB_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
        onCreate(db);
    }

    public long insert(ContentValues values) {
        db = getWritableDatabase();
        long result = 0L;
        try {
            result = db.insert(DB_TABLE, null, values);
        } catch(SQLiteConstraintException sqlce) {
            return result;
        }finally {

        }
        //db.close();
        return result;
    }

    public Cursor query() {
        db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE, new String[] {DB_COL_STATION_ID}, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            //db.close();
            return cursor;
        }
        //db.close();
        return null;
    }

    public boolean query(String id) {
        if(D) Log.d(TAG, "query id: " + id);
        db = getReadableDatabase();
        Cursor cursor = db.query(DB_TABLE, new String[] {DB_COL_STATION_ID}, DB_COL_STATION_ID + "=?", new String[]{id}, null, null, null);
        int index = cursor.getColumnIndexOrThrow(DB_COL_STATION_ID);
        if(cursor != null) {
            if(D) Log.d(TAG, "query(id) : cursor not null, count: " + cursor.getCount());
            if(cursor.moveToFirst()) {
                if(D) Log.d(TAG, "return true");
                //db.close();
                return true;
            } else {
                if(D) Log.d(TAG, "return false");
                db.close();
                return false;
            }
        } else {
            if(D) Log.d(TAG, "query(id) : cursor null");
            //db.close();
            return false;
        }
    }

    public int delete(String id) {
        if(db == null) {
            db = getWritableDatabase();
        }
        int result = db.delete(DB_TABLE, DB_COL_STATION_ID + "=?", new String[]{id});
        //db.close();
        return result;
    }

    public void close() {
        if(db != null) {
            db.close();
        }
    }
}
