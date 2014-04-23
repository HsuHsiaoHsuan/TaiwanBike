package idv.funnybrain.bike.database;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Created by Freeman on 2014/4/23.
 */
public interface IDBHelper {
    public static final String DB_COL_ID = "_id";
    public static final String DB_COL_STATION_ID = "station_id";

    public long insert(ContentValues cv);
    public Cursor query();
    public boolean query(String id);
    public int delete(String id);
    public void close();
}
