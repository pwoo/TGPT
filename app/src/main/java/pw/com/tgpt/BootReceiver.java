package pw.com.tgpt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "RECV";

    public BootReceiver() {
        super();
        Log.d(TAG, "BootReceiver");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: Read Serialized values, place in Parcelable and put into Intent
        Log.d(TAG, "Boot message received!");
        // http://stackoverflow.com/a/5228494 (What exactly does using the Application Context mean?)
    }
}
