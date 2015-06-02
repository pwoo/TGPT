package pw.com.tgpt;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "RECV";

    public BootReceiver() {
        super();
        Log.d(TAG, "BootReceiver");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot message received!");
        // http://stackoverflow.com/a/5228494 (What exactly does using the Application Context mean?)
        SharedPreferences settings = context.getSharedPreferences(context.getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        Calendar calendar = Calendar.getInstance();
        int hour = settings.getInt(context.getResources().getString(R.string.pref_time_trigger_hour), -1);
        int minute = settings.getInt(context.getResources().getString(R.string.pref_time_trigger_minute), -1);

        boolean dynamicCheck = settings.getBoolean(context.getResources().getString(R.string.pref_dynamic_update), false);
        if (dynamicCheck) {
            Intent i = new Intent(context, PushUpdateService.class);
            i.setAction(PushUpdateService.ACTION_CREATE_DYNAMIC_NOTIFICATION);
            i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, calendar.getTimeInMillis() + 1000);
            i.putExtra(PushUpdateService.ALARM_INTERVAL_MILLIS, AlarmManager.INTERVAL_HALF_HOUR);
            context.startService(i);
        }

        // Daily check
        if (hour != -1 && minute != -1) {
            calendar.set(Calendar.HOUR, hour);
            calendar.set(Calendar.MINUTE, minute);

            Intent i = new Intent(context, PushUpdateService.class);
            i.setAction(PushUpdateService.ACTION_CREATE_STATIC_NOTIFICATION);
            i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, calendar.getTimeInMillis());
            i.putExtra(PushUpdateService.ALARM_INTERVAL_MILLIS, AlarmManager.INTERVAL_DAY);
            context.startService(i);
        }
    }
}
