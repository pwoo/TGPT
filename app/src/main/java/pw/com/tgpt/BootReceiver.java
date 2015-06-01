package pw.com.tgpt;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;

import pw.com.tgpt.PushUpdateService;

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

        City.init(context.getResources());

        int hour = settings.getInt(context.getResources().getString(R.string.pref_time_trigger_hour), -1);
        int minute = settings.getInt(context.getResources().getString(R.string.pref_time_trigger_minute), -1);
        if (hour != -1 && minute != -1) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);

            Intent i = new Intent(context, PushUpdateService.class);
            i.setAction(PushUpdateService.ACTION_CREATE_STATIC_NOTIFICATION);
            i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, calendar.getTimeInMillis());
            i.putExtra(PushUpdateService.ALARM_INTERVAL_MILLIS, AlarmManager.INTERVAL_DAY);
            context.startService(i);
        }
    }
}
