package pw.com.tgpt;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.util.Date;

/**
 * Created by PW on 2015-04-26.
 */
public class City {
    private static final String TAG = "CITY";
    private static ArrayList<City> cityList;
    private int id;
    private String name;
    private double regularPrice;
    private double regularDiff;
    private double lastWeekRegular;
    private double lastMonthRegular;
    private double lastYearRegular;
    private Direction direction;
    private Date currentDate;
    private Date lastUpdate;

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

    public static void init(Resources r) {
        try {
            if (cityList == null || cityList.isEmpty()) {
                XmlResourceParser parser = r.getXml(R.xml.cities);
                cityList = City.generateCities(parser);
            }
        }
        catch (Resources.NotFoundException e) {
            // TODO: Kill process / activity
        }
    }

    public static ArrayList<City> getCitiesArray() {
        return cityList;
    }

    public static City getCity(int id) {
        if (cityList.isEmpty()) {
            Log.w(TAG, "City class uninitialized!");
        }

        for (City c : cityList) {
            if (c.getID() == id) {
                return c;
            }
        }
        return null;
    }

    private City() {
    }

    protected City(int id, String name) {
        this.id = id;
        this.name = name;
        currentDate = new Date();
    }

    private static ArrayList<City> generateCities(XmlResourceParser parser) {
        int id = -1;
        String name = null;
        ArrayList<City> cityList = new ArrayList<City>();
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
                            City city = new City(id, name);
                            cityList.add(city);
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
        return cityList;
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
            Date lastUpdate = formatter.parse(temp, new ParsePosition(0));
            if (lastUpdate != null) {
                setLastUpdate(lastUpdate);
            }
        }
    }

    public void setID(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String toString()
    {
        return name;
    }

    public double getRegularPrice() {
        return regularPrice;
    }

    public void setRegularPrice(double regularPrice) {
        Log.v(TAG, getName() + ", old regularPrice: " + this.regularPrice
                + ", new regularPrice: " + regularPrice);
        this.regularPrice = regularPrice;
    }

    public double getRegularDiff() {
        return regularDiff;
    }

    public void setRegularDiff(double regularDiff) {
        Log.v(TAG, getName() + ", old regularDiff: " + this.regularDiff
                + ", new regularDiff: " + regularDiff);
        this.regularDiff = regularDiff;
    }

    public double getLastWeekRegular() {
        return lastWeekRegular;
    }

    public void setLastWeekRegular(double lastWeekRegular) {
        Log.v(TAG, getName() + ", old lastWeekRegular: " + this.lastWeekRegular
                + ", new lastWeekRegular: " + lastWeekRegular);
        this.lastWeekRegular = lastWeekRegular;
    }

    public double getLastMonthRegular() {
        return lastMonthRegular;
    }

    public void setLastMonthRegular(double lastMonthRegular) {
        this.lastMonthRegular = lastMonthRegular;
    }

    public double getLastYearRegular() {
        return lastYearRegular;
    }

    public void setLastYearRegular(double lastYearRegular) {
        this.lastYearRegular = lastYearRegular;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setLastUpdate(Date update) { lastUpdate = update; }

    public Date getLastUpdate() { return lastUpdate; }

    public Date getCurrentDate() { return currentDate; }
}
