package pw.com.tgpt;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PushUpdateService extends IntentService {
    // CHANGE: DYNAMIC_NOTIFICATION pulls all city ids from a parcelable, updates and fires notifications
    // as a group
    private static final String TAG = "PUSH";
    public static final String ACTION_CREATE_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.CREATE_DYNAMIC_NOTIFICATION";
    public static final String ACTION_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.DYNAMIC_NOTIFICATION";
    public static final String ACTION_CANCEL_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.CANCEL_DYNAMIC_NOTIFICATION";
    public static final String EXTRA_CITY_ID = "pw.com.tgpt.action.NOTIFICATION_CITY_ID";
    private static final String GROUP_KEY_NOTIFICATIONS = "pw.com.tgpt.notification";

    @Override
    public void onCreate() {
        super.onCreate();

        City.init(this);
    }

    public PushUpdateService() {
        super("PushUpdateService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     *
     * @param anAction
     * @param aCity
     */
    private void handleActionUpdateAlarm(String anAction, City aCity) {
        Log.v(TAG, "handleActionUpdateAlarm(" + anAction + ")");

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(anAction);
        intent.putExtra(EXTRA_CITY_ID, aCity.getID());

        PendingIntent alarmIntent = PendingIntent.getService(this, aCity.getID(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar triggerTime = Calendar.getInstance();
        triggerTime.add(Calendar.MINUTE, 10);
        alarmMgr.setInexactRepeating(AlarmManager.RTC, triggerTime.getTimeInMillis(),
                AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
    }

    /**
     *
     * @param action
     */
    private void handleActionCancelAlarm(String action, City city) {
        Log.v(TAG, "handleActionCancelAlarm(" + action + ")");

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(action);

        PendingIntent alarmIntent = PendingIntent.getService(this, city.getID(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(alarmIntent);
        alarmIntent.cancel();
    }

    /**
     *
     * @param intent
     */
    private void handleActionNotification(Intent intent) {
        String action = intent.getAction();
        Log.v(TAG, "handleActionNotification(" + action + ")");

        final City savedCity = City.getCity(intent.getIntExtra(EXTRA_CITY_ID, -1));

        if (savedCity != null) {
            Notification notification = savedCity.getDynamicNotification();
            if (savedCity.updateTGPTData(this)) {
                boolean sendNotification = false;

                    // Check if TGPT JSON data has been updated with tomorrow's price.
                Calendar lastNotify = notification.getLastNotify();
                if (lastNotify == null || savedCity.getLastUpdate().after(lastNotify)) {
                    sendNotification = true;

                    Log.v(TAG, "savedCity: " + savedCity.getLastUpdate().toString());
                    if (lastNotify != null)
                        Log.v(TAG, "lastDate: " + lastNotify.toString());
                }

                if (sendNotification) {
                    Intent myIntent = new Intent(this, pw.com.tgpt.MainActivity.class);
                    PendingIntent pIntent = PendingIntent.getActivity(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT, null);

                    NotificationCompat.Builder n = new NotificationCompat.Builder(this);
                    // Create title
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE", Locale.CANADA);
                    StringBuilder title = new StringBuilder(dateFormatter.format(savedCity.getLastUpdate().getTime()))
                            .append("'s gas price in ")
                            .append(savedCity.getName());

                    n.setContentTitle(title.toString());
                    n.setColor(getResources().getColor(R.color.dodger_blue));

                    // Create content text
                    StringBuilder text = new StringBuilder("Price is ");
                    text.append(savedCity.getRegularPrice());
                    text.append(", ");
                    if (savedCity.getDirection() != City.Direction.NO_CHANGE) {
                        text.append("going ")
                        .append(savedCity.getDirection().toString().toLowerCase())
                        .append(" ")
                        .append(savedCity.getRegularDiff())
                        .append(" cents");
                    }
                    else
                        text.append(savedCity.getDirection().toString().toLowerCase());

                    n.setContentText(text.toString());

                    n.setContentIntent(pIntent);
                    n.setSmallIcon(R.drawable.fuel);

                    NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notifyMgr != null) {
                        notifyMgr.notify(savedCity.getID(), n.build());
                        notification.setLastNotify(savedCity.getLastUpdate());
                        Log.v(TAG, "Notification sent!");
                    }
                    savedCity.saveToDB(this);
                }
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");
        if (intent != null) {
            City c = City.getCity(intent.getIntExtra(EXTRA_CITY_ID, -1));
            switch (intent.getAction()) {
                case ACTION_DYNAMIC_NOTIFICATION:
                    handleActionNotification(intent);
                    break;
                case ACTION_CREATE_DYNAMIC_NOTIFICATION: {
                    if (c != null)
                        handleActionUpdateAlarm(ACTION_DYNAMIC_NOTIFICATION, c);
                    break;
                }
                case ACTION_CANCEL_DYNAMIC_NOTIFICATION:
                    if (c != null)
                        handleActionCancelAlarm(ACTION_DYNAMIC_NOTIFICATION, c);
                    break;
            }
        }
    }
}
