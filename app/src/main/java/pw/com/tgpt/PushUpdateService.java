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
import java.util.concurrent.Semaphore;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PushUpdateService extends IntentService {
    private static final String TAG = "TGPT";
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

//    private static final String ACTION_FOO = "pw.com.tgpt.action.FOO";
//    private static final String ACTION_BAZ = "pw.com.tgpt.action.BAZ";
//
//    // TODO: Rename parameters
//    private static final String EXTRA_PARAM1 = "pw.com.tgpt.extra.PARAM1";
//    private static final String EXTRA_PARAM2 = "pw.com.tgpt.extra.PARAM2";

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
//    // TODO: Customize helper method
//    public static void startActionFoo(Context context, String param1, String param2) {
//        Intent intent = new Intent(context, PushUpdateService.class);
//        intent.setAction(ACTION_FOO);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
//        context.startService(intent);
//    }

    @Override
    public void onCreate() {
        super.onCreate();

        City.init(getResources());
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
//    // TODO: Customize helper method
//    public static void startActionBaz(Context context, String param1, String param2) {
//        Intent intent = new Intent(context, PushUpdateService.class);
//        intent.setAction(ACTION_BAZ);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
//        context.startService(intent);
//    }

    public PushUpdateService() {
        super("PushUpdateService");
        Log.v(TAG, "PushUpdateService started");
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "PushUpdateService destroyed!");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
//                    final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                    final String param2 = intent.getStringExtra(EXTRA_PARAM2);
            SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
            final Context appContext = this.getApplicationContext();
            int cityId = settings.getInt("pcityid", -1);
            final City savedCity = City.getCity(cityId);
            final Runnable notification = new Runnable() {
                @Override
                public void run() {
                    Log.v(TAG, "onHandleIntent");
                    NotificationCompat.Builder n = new NotificationCompat.Builder(appContext);
                    n.setSmallIcon(R.mipmap.ic_launcher);
                    if (savedCity != null) {
                        if (savedCity.updateTGPTData(appContext)) {
                            n.setContentText(new Double(savedCity.getRegularPrice()).toString());
                            n.setContentTitle("Current gas price in " + savedCity.getName());
                            NotificationManager notifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            notifyMgr.notify(123, n.build());
                        }
                    }
                }
            };
            notification.run();
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
