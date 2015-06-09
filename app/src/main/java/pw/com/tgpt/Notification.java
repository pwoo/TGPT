package pw.com.tgpt;

import android.content.Context;

import java.util.Calendar;

/**
 * Created by pwoo on 09/06/15.
 */
public class Notification {
    private long mID = -1;
    private City mCity;
    private boolean mDynamic;

    public Notification(long id, City city, boolean isDynamic) {
        mID = id;
        mCity = city;
        mDynamic = isDynamic;
    }

    public Notification(City city, boolean isDynamic) {
        mCity = city;
        mDynamic = isDynamic;
    }

    public City getCity() { return mCity; }
    public void setCity(City c) { mCity = c; }

    public void setDynamic(boolean isDynamic) { mDynamic = isDynamic; }
    public boolean getDynamic() { return mDynamic; }
    public void setID(long id) { mID = id; }
    public long getID() { return mID; }

    public void saveToDB(Context context) {
        if (mID == -1)
            DBHelper.getInstance(context).insertNotification(this);
        else
            DBHelper.getInstance(context).updateNotification(this);
    }

    public void removeFromDB(Context context) {
        DBHelper.getInstance(context).deleteNotification(this);
    }
}
