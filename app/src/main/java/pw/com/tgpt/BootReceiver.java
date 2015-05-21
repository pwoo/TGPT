package pw.com.tgpt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
        City.init(context.getResources());
        Context appContext = context.getApplicationContext();
        // TODO: Push alarms to manager if enabled via preferences
        Intent i = new Intent(PushUpdateService.ACTION_DYNAMIC_NOTIFICATION);

        appContext.startService(i);
    }
}
