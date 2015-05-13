package pw.com.tgpt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "TGPT";

    public BootReceiver() {
        super();
        Log.d(TAG, "BootReceiver");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot message received!");
        // TODO: Start service that sends current price to notifications
        Context appContext = context.getApplicationContext();
        Intent i = new Intent(appContext, PushUpdateService.class);
        i.setAction("pw.com.tgpt.action.FOO");
        appContext.startService(i);
    }
}
