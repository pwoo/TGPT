package pw.com.tgpt;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pw.com.tgpt.PushUpdateService;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener, TimePicker.OnTimeChangedListener {
    private static final String TAG = "MAIN";
    ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        City.init(getResources());
        ArrayAdapter<City> cities = new ArrayAdapter<City>(this, R.layout.city_spinner_view);
        cities.addAll(City.getCitiesArray());

        Spinner spin = (Spinner) findViewById(R.id.cities);
        if (spin != null) {
            spin.setAdapter(cities);
            SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
            spin.setSelection(settings.getInt(getResources().getString(R.string.pref_selected_city_spin), 0));
            spin.setOnItemSelectedListener(this);
        }

        TimePicker timePicker = (TimePicker) findViewById(R.id.time_picker);
        if (timePicker != null) {
            timePicker.setOnTimeChangedListener(this);
        }
    }

    private void createAlarm() {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PushUpdateService.class);

        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR, 10);
        calendar.set(Calendar.MINUTE, 33);

//        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
//                1000, alarmIntent);
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        final City city = (City) parent.getItemAtPosition(pos);
        Runnable updateCity = new Runnable() {
            @Override
            public void run() {
                if (city.updateTGPTData(getApplicationContext())) {
                    updatePrices(city);
                }
                else {
                    mExecutor.execute(this);
                }
            }
        };

        mExecutor.execute(updateCity);
    }

    private void updatePrices(City city) {
        final City c = city;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView view = (TextView) findViewById(R.id.regular_price);
                view.setText("Regular Price: "+ c.getRegularPrice());
            }
        });
    }

    protected void onPause() {
        super.onPause();

        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        Spinner spin = (Spinner) findViewById(R.id.cities);
        City currentCity = (City) spin.getSelectedItem();

        editor.putInt(getResources().getString(R.string.pref_selected_city_spin), spin.getSelectedItemPosition());
        editor.putInt(getResources().getString(R.string.pref_city_tgpt_id), currentCity.getID());
        editor.putString(getResources().getString(R.string.pref_city_name), currentCity.getName());

        editor.commit();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(PushUpdateService.ACTION_UPDATE_NOTIFICATION);

        PendingIntent alarmIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);

        alarmMgr.setInexactRepeating(AlarmManager.RTC, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, alarmIntent);

        Log.v(TAG, "Update notification set to " + hourOfDay + ":" + minute);
    }
}
