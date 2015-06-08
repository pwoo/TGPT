package pw.com.tgpt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by PW on 2015-06-07.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static final String TAG = "DB";
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "TGPT.db";
    public static DBHelper mInstance;
    public static final String mDateFormat = "yyyy-MM-dd HH:mm:ss.SSSZ";

    public static abstract class CityEntry implements BaseColumns {
        public static final String TABLE_NAME = "CITY";
        public static final String COLUMN_NAME_CITY_ID = "CITYID"; // Secondary key
        public static final String COLUMN_NAME_CITY_NAME = "CITYNAME";
        public static final String COLUMN_NAME_REGULAR_PRICE = "REGPRICE";
        public static final String COLUMN_NAME_REGULAR_DIFF = "REGDIFF";
        public static final String COLUMN_NAME_LAST_WEEK_REGULAR = "LASTWKREG";
        public static final String COLUMN_NAME_LAST_MONTH_REGULAR = "LASTMTHREG";
        public static final String COLUMN_NAME_LAST_YR_REGULAR = "LASTYRREG";
        public static final String COLUMN_NAME_LAST_UPDATE = "LASTUPDATE";
        public static final String COLUMN_NAME_CURRENT_DATE = "CURRENTDATE";

        public static final String COLUMN_NAME_ENABLED = "VISIBLE";
    }

    public static abstract class StarredEntry implements BaseColumns {
        public static final String TABLE_NAME = "STARRED";
        public static final String COLUMN_NAME_CITY_ID = "CITYID"; // Secondary key

        public static final String COLUMN_NAME_ENABLED = "VISIBLE";
    }

    public static abstract class Notifications implements BaseColumns {
        public static final String TABLE_NAME = "STARRED";
        public static final String COLUMN_NAME_CITY_ID = "CITYID"; // Secondary key

        public static final String COLUMN_NAME_DYNAMIC = "DYNAMIC";
        public static final String COLUMN_NAME_TRIGGER_TIME = "TRIGGERTIME";
    }

    private static String CREATE_TABLE_CITIES =
            "CREATE TABLE " + CityEntry.TABLE_NAME + "("
                    + CityEntry.COLUMN_NAME_CITY_ID + " INTEGER PRIMARY KEY,"
                    + CityEntry.COLUMN_NAME_CITY_NAME + " TEXT NOT NULL,"
                    + CityEntry.COLUMN_NAME_REGULAR_PRICE + " REAL DEFAULT 0,"
                    + CityEntry.COLUMN_NAME_REGULAR_DIFF + " REAL DEFAULT 0,"
                    + CityEntry.COLUMN_NAME_LAST_WEEK_REGULAR + " REAL DEFAULT 0,"
                    + CityEntry.COLUMN_NAME_LAST_MONTH_REGULAR + " REAL DEFAULT 0,"
                    + CityEntry.COLUMN_NAME_LAST_YR_REGULAR + " REAL DEFAULT 0,"
                    + CityEntry.COLUMN_NAME_LAST_UPDATE + " DATETIME,"
                    + CityEntry.COLUMN_NAME_CURRENT_DATE + " DATETIME,"
                    + CityEntry.COLUMN_NAME_ENABLED + " BOOLEAN DEFAULT 1" + ")";


    public static DBHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DBHelper(context);
        }
        return mInstance;
    }

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            db.execSQL(CREATE_TABLE_CITIES);
            db.setTransactionSuccessful();
        }
        finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }

    public void insertCity(int id, String name) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(CityEntry.COLUMN_NAME_CITY_ID, id);
            values.put(CityEntry.COLUMN_NAME_CITY_NAME, name);
            if (db.insert(CityEntry.TABLE_NAME, null, values) != -1) {
                Log.v(TAG, "City " + name + " inserted");
                db.setTransactionSuccessful();
            }
        }
        finally {
            db.endTransaction();
        }
    }

    public void updateCity(City city) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(mDateFormat);
            StringBuffer lastUpdate = new StringBuffer();
            StringBuffer currentDate = new StringBuffer();
            dateFormatter.format(city.getLastUpdate().getTime(), lastUpdate, new FieldPosition(0));
            dateFormatter.format(city.getCurrentDate().getTime(), currentDate, new FieldPosition(0));

            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(CityEntry.COLUMN_NAME_REGULAR_PRICE, city.getRegularPrice());
            values.put(CityEntry.COLUMN_NAME_REGULAR_DIFF, city.getRegularDiff());
            values.put(CityEntry.COLUMN_NAME_LAST_WEEK_REGULAR, city.getLastWeekRegular());
            values.put(CityEntry.COLUMN_NAME_LAST_MONTH_REGULAR, city.getLastMonthRegular());
            values.put(CityEntry.COLUMN_NAME_LAST_YR_REGULAR, city.getLastYearRegular());
            values.put(CityEntry.COLUMN_NAME_CURRENT_DATE, currentDate.toString());
            values.put(CityEntry.COLUMN_NAME_LAST_UPDATE, lastUpdate.toString());
            values.put(CityEntry.COLUMN_NAME_ENABLED, city.getEnabled());

            if (db.update(CityEntry.TABLE_NAME, values, CityEntry.COLUMN_NAME_CITY_ID + "=" + city.getID(), null) == 1) {
                db.setTransactionSuccessful();
            }
        }
        finally {
            db.endTransaction();
        }
    }

    public ArrayList<City> getCities() {
        ArrayList<City> cities = new ArrayList<City>();
        SimpleDateFormat dateFormatter = new SimpleDateFormat(mDateFormat);
        String query = "SELECT * from " + CityEntry.TABLE_NAME;

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(query, null);
        if (c.moveToFirst()) {
            do {
                City city = new City(
                        c.getInt(c.getColumnIndex(CityEntry.COLUMN_NAME_CITY_ID)),
                        c.getString(c.getColumnIndex(CityEntry.COLUMN_NAME_CITY_NAME)));

                city.setRegularPrice(c.getDouble(c.getColumnIndex(CityEntry.COLUMN_NAME_REGULAR_PRICE)));
                city.setRegularDiff(c.getDouble(c.getColumnIndex(CityEntry.COLUMN_NAME_REGULAR_DIFF)));
                city.setLastWeekRegular(c.getDouble(c.getColumnIndex(CityEntry.COLUMN_NAME_LAST_WEEK_REGULAR)));
                city.setLastMonthRegular(c.getDouble(c.getColumnIndex(CityEntry.COLUMN_NAME_LAST_MONTH_REGULAR)));
                city.setLastYearRegular(c.getDouble(c.getColumnIndex(CityEntry.COLUMN_NAME_LAST_YR_REGULAR)));
                city.setEnabled(c.getInt(c.getColumnIndex(CityEntry.COLUMN_NAME_ENABLED)) != 0);

                String lastUpdateString = c.getString(c.getColumnIndex(CityEntry.COLUMN_NAME_LAST_UPDATE));
                if (lastUpdateString != null) {
                    Calendar lastUpdate = Calendar.getInstance();
                    lastUpdate.setTime(dateFormatter.parse(lastUpdateString, new ParsePosition(0)));
                    city.setLastUpdate(lastUpdate);
                }

                String currentDateString = c.getString(c.getColumnIndex(CityEntry.COLUMN_NAME_CURRENT_DATE));
                if (currentDateString != null) {
                    Calendar currentDate = Calendar.getInstance();
                    currentDate.setTime(dateFormatter.parse(currentDateString, new ParsePosition(0)));
                    city.setCurrentDate(currentDate);
                }
                cities.add(city);
            } while (c.moveToNext());
        }

        return cities;
    }
}
