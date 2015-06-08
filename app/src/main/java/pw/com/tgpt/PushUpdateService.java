package pw.com.tgpt;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Calendar;

public class PushUpdateService extends IntentService {
    // CHANGE: DYNAMIC_NOTIFICATION pulls all city ids from a parcelable, updates and fires notifications
    // as a group
    private static final String TAG = "PUSH";
    public static final String ACTION_CREATE_STATIC_NOTIFICATION = "pw.com.tgpt.action.CREATE_STATIC_NOTIFICATION";
    public static final String ACTION_STATIC_NOTIFICATION = "pw.com.tgpt.action.STATIC_NOTIFICATION";
    public static final String ACTION_CANCEL_STATIC_NOTIFICATION = "pw.com.tgpt.action.CANCEL_STATIC_NOTIFICATION";
    public static final String ACTION_CREATE_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.CREATE_DYNAMIC_NOTIFICATION";
    public static final String ACTION_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.DYNAMIC_NOTIFICATION";
    public static final String ACTION_CANCEL_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.CANCEL_DYNAMIC_NOTIFICATION";

    public static final String ALARM_TRIGGER_AT_MILLIS = "alarm.triggerAtMillis";
    public static final String ALARM_INTERVAL_MILLIS = "alarm.intervalMillis";
    public static final String ALARM_LAST_NOTIFY = "alarm.lastNotify";

    private static final int NOTIFY_ID = 0;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public PushUpdateService() {
        super("PushUpdateService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void handleActionUpdateAlarm(String action, long triggerAtMillis, long intervalAtMillis) {
        handleActionUpdateAlarm(action, triggerAtMillis, intervalAtMillis, null);
    }

    /**
     *
     * @param action
     * @param triggerAtMillis
     * @param intervalAtMillis
     */
    private void handleActionUpdateAlarm(String action, long triggerAtMillis, long intervalAtMillis, Calendar lastNotify) {
        Log.v(TAG, "handleActionUpdateAlarm(" + action + ")");

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(action);
        intent.putExtra(ALARM_TRIGGER_AT_MILLIS, triggerAtMillis);
        intent.putExtra(ALARM_INTERVAL_MILLIS, intervalAtMillis);
        if (lastNotify != null) {
            intent.putExtra(ALARM_LAST_NOTIFY, lastNotify);
        }

        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.RTC, triggerAtMillis,
                intervalAtMillis, alarmIntent);
    }

    /**
     *
     * @param action
     */
    private void handleActionCancelAlarm(String action) {
        Log.v(TAG, "handleActionCancelAlarm(" + action + ")");

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(action);

        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(alarmIntent);
        alarmIntent.cancel();
    }

    /**
     *
     * @param intent
     */
    private void handleActionNotification(Intent intent) {
        String action = intent.getAction();
        String createAction = null;
        Log.v(TAG, "handleActionNotification(" + action + ")");

        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        int cityId = settings.getInt("pcityid", -1);
        final City savedCity = City.getCity(cityId);

        if (savedCity != null) {
            if (savedCity.updateTGPTData(this)) {
                boolean sendNotification = false;
                boolean isTomorrowsPrice = false;

                if (savedCity.getLastUpdate().after(Calendar.getInstance())) {
                    isTomorrowsPrice = true;
                }

                if (action.equals(ACTION_STATIC_NOTIFICATION)) {
                    sendNotification = true;
                    createAction = ACTION_CREATE_STATIC_NOTIFICATION;
                }
                else if (action.equals(ACTION_DYNAMIC_NOTIFICATION))
                {
                    createAction = ACTION_CREATE_DYNAMIC_NOTIFICATION;
                    // Check if TGPT JSON data has been updated with tomorrow's price.
                    Calendar lastDate = (Calendar) intent.getSerializableExtra(ALARM_LAST_NOTIFY);
                    if (lastDate != null) {
                        if (savedCity.getLastUpdate().after(lastDate)) {
                            Log.v(TAG, "savedCity: " + savedCity.getLastUpdate().toString());
                            Log.v(TAG, "lastDate: " + lastDate.toString());
                            sendNotification = true;
                        }

                    } else {
                        Log.v(TAG, "No prior notification detected");
                        sendNotification = true;
                    }
                }

                if (sendNotification) {
                    Intent myIntent = new Intent(this, pw.com.tgpt.MainActivity.class);
                    PendingIntent pIntent = PendingIntent.getActivity(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT, null);

                    NotificationCompat.Builder n = new NotificationCompat.Builder(this);
                    // Create title
                    StringBuilder title = new StringBuilder(isTomorrowsPrice? "Tomorrow's " : "Current ");
                    title.append("gas price in ").append(savedCity.getName());

                    n.setContentTitle(title.toString());
                    n.setColor(getResources().getColor(R.color.dodger_blue));

                    // Create content text
                    StringBuilder text = new StringBuilder("Price is ");
                    text.append(new Double(savedCity.getRegularPrice()).toString());
                    text.append(", ");
                    if (savedCity.getDirection() != City.Direction.NO_CHANGE) {
                        text.append("going ");
                        text.append(savedCity.getDirection().toString().toLowerCase());
                        text.append(" ");
                        text.append(savedCity.getRegularDiff());
                        text.append(" cents");
                    }
                    else
                        text.append(savedCity.getDirection().toString());


                    n.setContentText(text.toString());

                    n.setContentIntent(pIntent);
                    n.setSmallIcon(R.drawable.fuel);

                    NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notifyMgr != null) {
                        notifyMgr.notify(NOTIFY_ID, n.build());
                        Log.v(TAG, "Notification sent!");
                    }

                    // Recreate pending intent with last notification time

                    if (action.equals(ACTION_DYNAMIC_NOTIFICATION)) {
                        Intent updateIntent = new Intent(this, PushUpdateService.class);
                        updateIntent.setAction(createAction);
                        updateIntent.putExtras(intent.getExtras());
                        updateIntent.putExtra(ALARM_LAST_NOTIFY, savedCity.getLastUpdate());
                        startService(updateIntent);
                    }
                }
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");
        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_DYNAMIC_NOTIFICATION:
                case ACTION_STATIC_NOTIFICATION:
                    handleActionNotification(intent);
                    break;
                case ACTION_CREATE_DYNAMIC_NOTIFICATION: {
                    long triggerAtMillis = intent.getLongExtra(ALARM_TRIGGER_AT_MILLIS, -1);
                    long intervalMillis = intent.getLongExtra(ALARM_INTERVAL_MILLIS, -1);
                    Calendar lastNotify = (Calendar) intent.getSerializableExtra(ALARM_LAST_NOTIFY);

                    if (triggerAtMillis != -1 && intervalMillis != -1) {
                        handleActionUpdateAlarm(ACTION_DYNAMIC_NOTIFICATION, triggerAtMillis, intervalMillis, lastNotify);
                    }
                }
                    break;
                case ACTION_CREATE_STATIC_NOTIFICATION: {
                    long triggerAtMillis = intent.getLongExtra(ALARM_TRIGGER_AT_MILLIS, -1);
                    long intervalMillis = intent.getLongExtra(ALARM_INTERVAL_MILLIS, -1);
                    Calendar lastNotify = (Calendar) intent.getSerializableExtra(ALARM_LAST_NOTIFY);

                    if (triggerAtMillis != -1 && intervalMillis != -1) {
                        handleActionUpdateAlarm(ACTION_STATIC_NOTIFICATION, triggerAtMillis, intervalMillis, lastNotify);
                    }
                }
                    break;
                case ACTION_CANCEL_DYNAMIC_NOTIFICATION:
                    handleActionCancelAlarm(ACTION_DYNAMIC_NOTIFICATION);
                    break;
                case ACTION_CANCEL_STATIC_NOTIFICATION:
                    handleActionCancelAlarm(ACTION_STATIC_NOTIFICATION);
                    break;
            }
        }
    }
}
