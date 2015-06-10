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
import java.util.HashMap;

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
        public static final String COLUMN_NAME_STARRED = "STARRED";
        public static final String COLUMN_NAME_ENABLED = "VISIBLE";
    }

    public static abstract class Notifications implements BaseColumns {
        public static final String TABLE_NAME = "NOTIFICATIONS";
        public static final String COLUMN_NAME_ID = "ID";
        public static final String COLUMN_NAME_CITY_ID = "CITYID"; // Secondary key
        public static final String COLUMN_NAME_DYNAMIC = "DYNAMIC";
        public static final String COLUMN_NAME_LAST_NOTIFY = "LASTNOTIFY";
    }

    private static StringBuilder CREATE_TABLE_CITIES = new StringBuilder()
            .append("CREATE TABLE ")
            .append(CityEntry.TABLE_NAME)
            .append('(')
            .append(CityEntry.COLUMN_NAME_CITY_ID).append(" INTEGER PRIMARY KEY,")
            .append(CityEntry.COLUMN_NAME_CITY_NAME).append(" TEXT NOT NULL,")
            .append(CityEntry.COLUMN_NAME_REGULAR_PRICE).append(" REAL DEFAULT 0,")
            .append(CityEntry.COLUMN_NAME_REGULAR_DIFF).append(" REAL DEFAULT 0,")
            .append(CityEntry.COLUMN_NAME_LAST_WEEK_REGULAR).append(" REAL DEFAULT 0,")
            .append(CityEntry.COLUMN_NAME_LAST_MONTH_REGULAR).append(" REAL DEFAULT 0,")
            .append(CityEntry.COLUMN_NAME_LAST_YR_REGULAR).append(" REAL DEFAULT 0,")
            .append(CityEntry.COLUMN_NAME_LAST_UPDATE).append(" DATETIME,")
            .append(CityEntry.COLUMN_NAME_CURRENT_DATE).append(" DATETIME,")
            .append(CityEntry.COLUMN_NAME_STARRED).append(" BOOLEAN DEFAULT 0,")
            .append(CityEntry.COLUMN_NAME_ENABLED).append(" BOOLEAN DEFAULT 1")
            .append(')');

    private static StringBuilder CREATE_TABLE_NOTIFICATIONS = new StringBuilder()
            .append("CREATE TABLE ")
            .append(Notifications.TABLE_NAME)
            .append('(')
            .append(Notifications.COLUMN_NAME_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT,")
            .append(Notifications.COLUMN_NAME_CITY_ID).append(" INTEGER NOT NULL,")
            .append(Notifications.COLUMN_NAME_DYNAMIC).append(" BOOLEAN DEFAULT 0,")
            .append(Notifications.COLUMN_NAME_LAST_NOTIFY).append(" DATETIME")
            .append(')');

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
            db.execSQL(CREATE_TABLE_CITIES.toString());
            db.execSQL(CREATE_TABLE_NOTIFICATIONS.toString());
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

    public void insertNotification(Notification n) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            long key = -1;
            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(Notifications.COLUMN_NAME_CITY_ID, n.getCity().getID());
            values.put(Notifications.COLUMN_NAME_DYNAMIC, n.getDynamic());
            if (n.getLastNotify() != null)
                values.put(Notifications.COLUMN_NAME_LAST_NOTIFY, formatDate(n.getLastNotify()));

            if ((key = db.insert(Notifications.TABLE_NAME, null, values)) != -1) {
                db.setTransactionSuccessful();
                n.setID(key);
            }
        }
        finally {
            db.endTransaction();
        }
    }

    public void updateNotification(Notification n) {
        if (n.getID() >= 0) {
            SQLiteDatabase db = getWritableDatabase();
            try {
                Log.v(TAG, "Notification " + n.getID() + " beginning update");
                db.beginTransaction();
                ContentValues values = new ContentValues();
                values.put(Notifications.COLUMN_NAME_DYNAMIC, n.getDynamic());
                if (n.getLastNotify() != null)
                    values.put(Notifications.COLUMN_NAME_LAST_NOTIFY, formatDate(n.getLastNotify()));
                if (db.update(Notifications.TABLE_NAME, values, Notifications.COLUMN_NAME_ID + "=" + n.getID(), null) == 1) {
                    db.setTransactionSuccessful();
                    Log.v(TAG, "Notification " + n.getID() + " update successful");
                }
            } finally {
                db.endTransaction();
            }
        }
        else
            insertNotification(n);
    }
    public void deleteNotification(Notification n) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            Log.v(TAG, "Notification " + n.getID() + " beginning delete");
            db.beginTransaction();
            if (db.delete(Notifications.TABLE_NAME, Notifications.COLUMN_NAME_ID + "=" + n.getID(), null) == 1) {
                db.setTransactionSuccessful();
                Log.v(TAG, "Notification " + n.getID() + " delete successful");
            }
        }
        finally {
            db.endTransaction();
        }
    }

    public void updateCity(City city) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            Log.v(TAG, "City " + city.getName() + " beginning update");

            db.beginTransaction();
            ContentValues values = new ContentValues();
            values.put(CityEntry.COLUMN_NAME_REGULAR_PRICE, city.getRegularPrice());
            values.put(CityEntry.COLUMN_NAME_REGULAR_DIFF, city.getRegularDiff());
            values.put(CityEntry.COLUMN_NAME_LAST_WEEK_REGULAR, city.getLastWeekRegular());
            values.put(CityEntry.COLUMN_NAME_LAST_MONTH_REGULAR, city.getLastMonthRegular());
            values.put(CityEntry.COLUMN_NAME_LAST_YR_REGULAR, city.getLastYearRegular());
            if (city.getCurrentDate() != null)
                values.put(CityEntry.COLUMN_NAME_CURRENT_DATE, formatDate(city.getCurrentDate()));
            if (city.getLastUpdate() != null)
                values.put(CityEntry.COLUMN_NAME_LAST_UPDATE, formatDate(city.getLastUpdate()));
            values.put(CityEntry.COLUMN_NAME_ENABLED, city.getEnabled());
            values.put(CityEntry.COLUMN_NAME_STARRED, city.getStarred());

            if (db.update(CityEntry.TABLE_NAME, values, CityEntry.COLUMN_NAME_CITY_ID + "=" + city.getID(), null) == 1) {
                db.setTransactionSuccessful();
                Log.v(TAG, "City " + city.getName() + " update successful");
            }
        }
        finally {
            db.endTransaction();
        }
    }

    public HashMap<Integer, City> getCities() {
        HashMap<Integer, City> cities = new HashMap<Integer, City>();
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
                city.setStarred(c.getInt(c.getColumnIndex(CityEntry.COLUMN_NAME_STARRED)) != 0);

                String lastUpdateString = c.getString(c.getColumnIndex(CityEntry.COLUMN_NAME_LAST_UPDATE));
                if (lastUpdateString != null) {
                    Calendar lastUpdate = formatDate(lastUpdateString);
                    city.setLastUpdate(lastUpdate);
                }

                String currentDateString = c.getString(c.getColumnIndex(CityEntry.COLUMN_NAME_CURRENT_DATE));
                if (currentDateString != null) {
                    Calendar currentDate = formatDate(currentDateString);
                    city.setCurrentDate(currentDate);
                }

                ArrayList<Notification> notifications = getNotifications(city);
                if (!notifications.isEmpty()) {
                    Notification n = notifications.get(0);
                    city.setDynamicNotification(n);
                }

                cities.put(city.getID(), city);
            } while (c.moveToNext());
        }

        return cities;
    }

    public ArrayList<Notification> getNotifications(City city) {
        ArrayList<Notification> notifications = new ArrayList<Notification>();

        StringBuilder query = new StringBuilder()
                .append("SELECT * from ")
                .append(Notifications.TABLE_NAME)
                .append(" WHERE ")
                .append(Notifications.COLUMN_NAME_CITY_ID)
                .append("=")
                .append(city.getID());

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(query.toString(), null);
        if (c.moveToFirst()) {
            do {
                long id = c.getInt(c.getColumnIndex(Notifications.COLUMN_NAME_ID));
                int cityId = c.getInt(c.getColumnIndex(Notifications.COLUMN_NAME_CITY_ID));
                boolean isDynamic = c.getInt(c.getColumnIndex(Notifications.COLUMN_NAME_DYNAMIC)) != 0;

                Notification n = new Notification(id, city, isDynamic);
                Log.v(TAG, "Notification " + id + " for " + city.getName() + " found");
                notifications.add(n);
            } while (c.moveToNext());
        }

        return notifications;
    }

    private String formatDate(Calendar aCalendar) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(mDateFormat);
        StringBuffer buffer = new StringBuffer();
        dateFormatter.format(aCalendar.getTime(), buffer, new FieldPosition(0));

        return buffer.toString();
    }

    private Calendar formatDate(String aString) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(mDateFormat);
        Calendar currentDate = Calendar.getInstance();
        currentDate.setTime(dateFormatter.parse(aString, new ParsePosition(0)));
        return currentDate;
    }
}
