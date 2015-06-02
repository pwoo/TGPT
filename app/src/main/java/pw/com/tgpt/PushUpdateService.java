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

public class PushUpdateService extends IntentService {
    private static final String TAG = "PUSH";
    public static final String ACTION_CREATE_STATIC_NOTIFICATION = "pw.com.tgpt.action.CREATE_STATIC_NOTIFICATION";
    public static final String ACTION_STATIC_NOTIFICATION = "pw.com.tgpt.action.STATIC_NOTIFICATION";
    public static final String ACTION_CANCEL_STATIC_NOTIFICATION = "pw.com.tgpt.action.CANCEL_STATIC_NOTIFICATION";
    public static final String ACTION_CREATE_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.CREATE_DYNAMIC_NOTIFICATION";
    public static final String ACTION_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.DYNAMIC_NOTIFICATION";
    public static final String ACTION_CANCEL_DYNAMIC_NOTIFICATION = "pw.com.tgpt.action.CANCEL_DYNAMIC_NOTIFICATION";

    public static final String ALARM_TRIGGER_AT_MILLIS = "alarm.triggerAtMillis";
    public static final String ALARM_INTERVAL_MILLIS = "alarm.intervalMillis";

    private static final int NOTIFY_ID = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        City.init(getResources());
    }

    public PushUpdateService() {
        super("PushUpdateService");
        Log.v(TAG, "PushUpdateService started");
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "PushUpdateService destroyed!");
        super.onDestroy();
    }

    /**
     *
     * @param action
     * @param triggerAtMillis
     * @param intervalAtMillis
     */
    private void handleActionCreate(String action, long triggerAtMillis, long intervalAtMillis) {
        Log.v(TAG, "handleActionCreate(" + action + ")");

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(action);

        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, triggerAtMillis,
                AlarmManager.INTERVAL_HALF_HOUR, alarmIntent);
    }

    /**
     *
     * @param action
     */
    private void handleActionCancel(String action) {
        Log.v(TAG, "handleActionCancel(" + action + ")");

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(action);

        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     *
     * @param action
     */
    private void handleActionUpdate(String action) {
        Log.v(TAG, "handleActionUpdate(" + action + ")");

        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        int cityId = settings.getInt("pcityid", -1);
        final City savedCity = City.getCity(cityId);

        if (savedCity != null) {
            if (savedCity.updateTGPTData(this)) {
                boolean sendNotification = false;
                if (action.equals(ACTION_STATIC_NOTIFICATION))
                    sendNotification = true;
                else if (action.equals(ACTION_DYNAMIC_NOTIFICATION))
                {
                    if (savedCity.getLastUpdate() != null && savedCity.getLastUpdate().after(savedCity.getCurrentDate())) {
                        sendNotification = true;
                    }
                }

                if (sendNotification) {
                    Intent myIntent = new Intent(this, MainActivity.class);
                    PendingIntent pIntent = PendingIntent.getActivity(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT, null);

                    NotificationCompat.Builder n = new NotificationCompat.Builder(this);
                    n.setContentText(new Double(savedCity.getRegularPrice()).toString());
                    n.setContentTitle("Current gas price in " + savedCity.getName());
                    n.setContentIntent(pIntent);
                    n.setSmallIcon(R.mipmap.ic_launcher);

                    NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (notifyMgr != null) {
                        notifyMgr.notify(NOTIFY_ID, n.build());
                    }
                }
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");
        if (intent != null) {
            if (intent.getAction().equals(ACTION_DYNAMIC_NOTIFICATION) || intent.getAction().equals(ACTION_STATIC_NOTIFICATION)) {
                handleActionUpdate(intent.getAction());
            }
            else if (intent.getAction().equals(ACTION_CREATE_DYNAMIC_NOTIFICATION)) {
                long triggerAtMillis = intent.getLongExtra(ALARM_TRIGGER_AT_MILLIS, -1);
                long intervalMillis = intent.getLongExtra(ALARM_INTERVAL_MILLIS, -1);

                if (triggerAtMillis != -1 && intervalMillis != -1) {
                    handleActionCreate(ACTION_DYNAMIC_NOTIFICATION, triggerAtMillis, intervalMillis);
                }
            }
            else if (intent.getAction().equals(ACTION_CREATE_STATIC_NOTIFICATION)) {
                long triggerAtMillis = intent.getLongExtra(ALARM_TRIGGER_AT_MILLIS, -1);
                long intervalMillis = intent.getLongExtra(ALARM_INTERVAL_MILLIS, -1);

                if (triggerAtMillis != -1 && intervalMillis != -1) {
                    handleActionCreate(ACTION_STATIC_NOTIFICATION, triggerAtMillis, intervalMillis);
                }
            }
            else if (intent.getAction().equals(ACTION_CANCEL_DYNAMIC_NOTIFICATION)) {
                handleActionCancel(ACTION_DYNAMIC_NOTIFICATION);
            }
            else if (intent.getAction().equals(ACTION_CANCEL_STATIC_NOTIFICATION)) {
                handleActionCancel(ACTION_STATIC_NOTIFICATION);
            }
        }
    }
}
