package pw.com.tgpt;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PushUpdateService extends IntentService {
    private static final String TAG = "PUSH";
    private static final String ACTION_UPDATE_NOTIFICATION = "pw.com.tgpt.action.SET_NOTIFICATION";
    private static final String ACTION_CANCEL_NOTIFICATION = "pw.com.tgpt.action.CANCEL_NOTIFICATION";
    private static final int NOTIFY_ID = 0;

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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

    private void handleActionUpdate() {
        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        final Context appContext = this.getApplicationContext();
        int cityId = settings.getInt("pcityid", -1);
        final City savedCity = City.getCity(cityId);

        NotificationCompat.Builder n = new NotificationCompat.Builder(appContext);
        n.setSmallIcon(R.mipmap.ic_launcher);
        if (savedCity != null) {
            if (savedCity.updateTGPTData(appContext)) {
                n.setContentText(new Double(savedCity.getRegularPrice()).toString());
                n.setContentTitle("Current gas price in " + savedCity.getName());
                NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notifyMgr.notify(NOTIFY_ID, n.build());
            }
        }
    }

    // TODO
    private void handleActionCancel() {

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent");
        if (intent != null) {
            if (intent.getAction().equals(ACTION_UPDATE_NOTIFICATION)) {
                handleActionUpdate();
            }
            else if (intent.getAction().equals(ACTION_CANCEL_NOTIFICATION)) {
                handleActionCancel();
            }
        }
    }
}
