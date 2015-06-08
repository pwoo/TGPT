package pw.com.tgpt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by PW on 2015-04-26.
 */
public class City {
    private static final String TAG = "CITY";
    private static ArrayList<City> mCityList;

    private int mID;
    private String mName;
    private double mRegularPrice;
    private double mRegularDiff;
    private double mLastWeekRegular;
    private double mLastMonthRegular;
    private double mLastYearRegular;
    private Direction mDirection;
    private Calendar mCurrentDate;
    private Calendar mLastUpdate;

    public enum Direction {
        UP("up"),
        DOWN("down"),
        NO_CHANGE("no change");

        private final String desc;
        private Direction(String value) {
            desc = value;
        }

        public String toString() {
            return desc;
        }
    }

    public static void init(Context context) {
        Resources r = context.getResources();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int previousVersion = prefs.getInt("version", -1);
        if(previousVersion < BuildConfig.VERSION_CODE) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("version", BuildConfig.VERSION_CODE);
            editor.commit();

            parseXML(context);
        }

        mCityList = DBHelper.getInstance(context).getCities();
    }

    public static ArrayList<City> getCitiesArray() {
        return mCityList;
    }

    public static City getCity(int id) {
        if (mCityList.isEmpty()) {
            Log.w(TAG, "City class uninitialized!");
        }

        for (City c : mCityList) {
            if (c.getID() == id) {
                return c;
            }
        }
        return null;
    }

    private City() {
    }

    protected City(int mID, String mName) {
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
            // TODO
        }
        finally {
            if (parser != null) {
                parser.close();
            }
        }
    }

    public boolean updateTGPTData(Context appContext) {
        boolean res = false;
        InputStream in = null;

        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            try {
                String spec = appContext.getString(R.string.json_url) + getID();
                URL url = new URL(spec);
                URLConnection urlConnection = url.openConnection();
                in = new BufferedInputStream(urlConnection.getInputStream());

                readJSON(in, appContext);
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
        setLastWeekRegular(parser.getDouble(appContext.getString(R.string.tgpt_last_week_regular)));

        String temp = parser.getString("regulardirection");
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
        Log.v(TAG, getName() + ", old mRegularPrice: " + this.mRegularPrice
                + ", new mRegularPrice: " + regularPrice);
        this.mRegularPrice = regularPrice;
    }

    public double getRegularDiff() {
        return mRegularDiff;
    }

    public void setRegularDiff(double mRegularDiff) {
        Log.v(TAG, getName() + ", old mRegularDiff: " + this.mRegularDiff
                + ", new mRegularDiff: " + mRegularDiff);
        this.mRegularDiff = mRegularDiff;
    }

    public double getLastWeekRegular() {
        return mLastWeekRegular;
    }

    public void setLastWeekRegular(double mLastWeekRegular) {
        Log.v(TAG, getName() + ", old mLastWeekRegular: " + this.mLastWeekRegular
                + ", new mLastWeekRegular: " + mLastWeekRegular);
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
}
