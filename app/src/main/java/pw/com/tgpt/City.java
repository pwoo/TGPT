package pw.com.tgpt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by PW on 2015-04-26.
 */
public class City {
    private static final String TAG = "CITY";
    private static SparseArray<City> mCityList;

    private int mID;
    private String mName;
    private Notification mDynamicNotification;
    private double mRegularPrice;
    private double mRegularDiff;
    private double mLastWeekRegular;
    private double mLastMonthRegular;
    private double mLastYearRegular;
    private Direction mDirection = Direction.NO_CHANGE;
    private Calendar mCurrentDate;
    private Calendar mLastUpdate;
    private boolean mEnabled = true;
    private boolean mStarred = false;

    public enum Direction {
        UP("Up", 2),
        DOWN("Down", 1),
        NO_CHANGE("No change", 0);

        private final String desc;
        private final int id;
        Direction(String value, int id) {
            desc = value;
            this.id = id;
        }

        public String toString() {
            return desc;
        }
        public int toInt() { return id; }
        public static Direction toDirection(int id) {
            Direction dir = NO_CHANGE;
            switch (id) {
                case 0:
                    dir = NO_CHANGE;
                    break;
                case 1:
                    dir = DOWN;
                    break;
                case 2:
                    dir = UP;
                    break;
            }
            return dir;
        }
    }

    public static void init(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int previousVersion = prefs.getInt("version", 0);
        if(previousVersion < BuildConfig.VERSION_CODE) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("version", BuildConfig.VERSION_CODE);
            editor.apply();
            // First time install
            if (previousVersion == 0) {
                parseXML(context);
            }
        }

        if (mCityList == null)
            mCityList = DBHelper.getInstance(context).getCities();
    }

    public static SparseArray<City> getCitiesArray() {
        return mCityList;
    }

    public static City getCity(int id) {
        if (mCityList.size() == 0) {
            Log.w(TAG, "City class uninitialized!");
        }

        return mCityList.get(id);
    }

    private City() {
    }

    City(int mID, String mName) {
        this.mID = mID;
        this.mName = mName;
    }

    private static void parseXML(Context context) {
        Resources r = context.getResources();
        XmlResourceParser parser = r.getXml(R.xml.cities);
        int id = -1;
        String name = null;
        try {
            boolean cityFound = false;
            while (parser.getEventType() != XmlResourceParser.END_DOCUMENT) {
                switch (parser.getEventType()) {
                    case XmlResourceParser.START_TAG:
                        if (parser.getName().equals("city")) {
                            cityFound = true;
                            id = parser.getAttributeIntValue(0, -1);
                        }
                        break;
                    case XmlResourceParser.END_TAG:
                        if (cityFound) {
                            cityFound = false;
                            DBHelper.getInstance(context).insertCity(id, name);
                        }
                        id = -1;
                        name = null;
                        break;
                    case XmlResourceParser.TEXT:
                        if (cityFound) {
                            name = parser.getText();
                        }
                        break;
                }
                parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Error parsing cities");
        }
        finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public boolean updateTGPTData(Context context) {
        boolean res = false;
        InputStream in = null;

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            try {
                String spec = context.getString(R.string.json_url) + getID();
                URL url = new URL(spec);
                URLConnection urlConnection = url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());

                readJSON(in, context);
                mEnabled = true;
                mCurrentDate = Calendar.getInstance();
                res = true;
            } catch (NullPointerException | IOException | JSONException e) {
                res = false;
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO
                    }
                }
            }
        }
        return res;
    }

    private void readJSON(InputStream in, Context appContext) throws NullPointerException, IOException, JSONException {
        StringBuilder result = new StringBuilder();
        byte [] buf = new byte[128];
        while (in.read(buf) != -1) {
            result.append(new String(buf));
        }
        result.trimToSize();

        JSONObject parser = new JSONObject(result.toString());
        parser = parser.getJSONObject(appContext.getString(R.string.tgpt_channel)).getJSONObject(appContext.getString(R.string.tgpt_item));

        setRegularPrice(parser.getDouble(appContext.getString(R.string.tgpt_regular_price)));
        setRegularDiff(parser.getDouble(appContext.getString(R.string.tgpt_regular_diff)));
        String temp = parser.getString(appContext.getString(R.string.tgpt_direction));
        try {
            setLastWeekRegular(parser.getDouble(appContext.getString(R.string.tgpt_last_week_regular)));
            setLastMonthRegular(parser.getDouble(appContext.getString(R.string.tgpt_last_month_regular)));
            setLastYearRegular(parser.getDouble(appContext.getString(R.string.tgpt_last_year_regular)));
        }
        catch (JSONException e) {
            // Do nothing -- these are considered low priority to display to the end user
        }

        Direction direction = Direction.NO_CHANGE;
        if (temp.equals("+")) {
            direction = Direction.UP;
        }
        else if (temp.equals("-")) {
            direction = Direction.DOWN;
        }

        setDirection(direction);

        SimpleDateFormat formatter = new SimpleDateFormat("cccc, MMMM d, yyyy");
        temp = parser.getString("title");
        if (!temp.isEmpty()) {
            Date lastUpdateDate = formatter.parse(temp, new ParsePosition(0));
            if (lastUpdateDate != null) {
                if (mLastUpdate == null)
                    mLastUpdate = Calendar.getInstance();
                mLastUpdate.setTime(lastUpdateDate);
                Log.v(TAG, "Last update time: " + mLastUpdate.toString());
            }
        }
    }

    public void setID(int id) {
        this.mID = id;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public int getID() {
        return mID;
    }

    public String getName() {
        return mName;
    }

    public String toString()
    {
        return mName;
    }

    public double getRegularPrice() {
        return mRegularPrice;
    }

    public void setRegularPrice(double regularPrice) {
        this.mRegularPrice = regularPrice;
    }

    public double getRegularDiff() {
        return mRegularDiff;
    }

    public void setRegularDiff(double mRegularDiff) {
        this.mRegularDiff = mRegularDiff;
    }

    public double getLastWeekRegular() {
        return mLastWeekRegular;
    }

    public void setLastWeekRegular(double mLastWeekRegular) {
        this.mLastWeekRegular = mLastWeekRegular;
    }

    public double getLastMonthRegular() {
        return mLastMonthRegular;
    }

    public void setLastMonthRegular(double mLastMonthRegular) {
        this.mLastMonthRegular = mLastMonthRegular;
    }

    public double getLastYearRegular() {
        return mLastYearRegular;
    }

    public void setLastYearRegular(double lastYearRegular) {
        this.mLastYearRegular = lastYearRegular;
    }

    public Direction getDirection() {
        return mDirection;
    }

    public void setDirection(Direction direction) {
        this.mDirection = direction;
    }

    public void setLastUpdate(Calendar update) { mLastUpdate = update; }

    public Calendar getLastUpdate() { return mLastUpdate; }

    public Calendar getCurrentDate() { return mCurrentDate; }

    public void setCurrentDate(Calendar calendar) { mCurrentDate = calendar; }

    public boolean getEnabled() { return mEnabled; }

    public void setEnabled(boolean enabled) { mEnabled = enabled; }

    public boolean getStarred() { return mStarred; }

    public void setStarred(boolean star) {
        mStarred = star;
    }

    public void setDynamicNotification(Notification n) { mDynamicNotification = n;}
    public Notification getDynamicNotification() {
        if (mDynamicNotification == null) {
            mDynamicNotification = new Notification(this, false);
        }
        return mDynamicNotification;
    }

    public void saveToDB(Context context) {
        DBHelper.getInstance(context).updateCity(this);

        if (mDynamicNotification != null)
            DBHelper.getInstance(context).updateNotification(mDynamicNotification);
    }
}
