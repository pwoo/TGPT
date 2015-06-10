package pw.com.tgpt;

import android.content.Context;
import android.util.Log;

import java.util.Calendar;
import java.util.HashSet;

/**
 * Created by pwoo on 09/06/15.
 */
public class Notification {
    private static final String TAG = "NTF";
    private static final HashSet<Notification> mDynamicNotifications = new HashSet<Notification>();
    private long mID = -1;
    private City mCity;
    private boolean mDynamic;
    private Calendar mLastNotify;

    public static HashSet<Notification> getDynamicNotifications() { return mDynamicNotifications; }

    public Notification(long id, City city, boolean isDynamic) {
        mID = id;
        mCity = city;
        mDynamic = isDynamic;
        if (mDynamic) {
            Log.v(TAG, "Dynamic notification added for " + city.getName());
            mDynamicNotifications.add(this);
        }
    }

    public Notification(City city, boolean isDynamic) {
        mCity = city;
        mDynamic = isDynamic;
    }

    public City getCity() { return mCity; }
    public void setCity(City c) { mCity = c; }

    public void setDynamic(boolean isDynamic) {
        mDynamic = isDynamic;
        if (mDynamic)
            mDynamicNotifications.add(this);
        else
            mDynamicNotifications.remove(this);
    }
    public boolean getDynamic() { return mDynamic; }
    public void setID(long id) { mID = id; }
    public long getID() { return mID; }
    public Calendar getLastNotify() { return mLastNotify; }
    public void setLastNotify(Calendar notify) { mLastNotify = notify; }

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
