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
        // TODO: Start service that sends current price to notifications
        City.init(context.getResources());
        Context appContext = context.getApplicationContext();
        Intent i = new Intent(appContext, PushUpdateService.class);
        i.setAction(PushUpdateService.ACTION_UPDATE_NOTIFICATION);

        appContext.startService(i);
    }
}
