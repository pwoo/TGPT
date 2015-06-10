package pw.com.tgpt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashSet;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "RECV";

    public BootReceiver() {
        super();
        Log.d(TAG, "BootReceiver");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Boot message received!");
        City.init(context);
        HashSet<Notification> dynamicNotifications = Notification.getDynamicNotifications();
        if (!dynamicNotifications.isEmpty()) {
            for (Notification n : dynamicNotifications) {
                Intent i = new Intent(context, PushUpdateService.class);
                i.setAction(PushUpdateService.ACTION_CREATE_DYNAMIC_NOTIFICATION);
                i.putExtra(PushUpdateService.EXTRA_CITY_ID, n.getCity().getID());
                context.startService(i);
            }
        }
    }
}
