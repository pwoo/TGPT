package pw.com.tgpt;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ProgressBar;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, TimePicker.OnTimeChangedListener {
    private static final String TAG = "MAIN";
    private City selectedCity = null;
    private Calendar selectedTime = Calendar.getInstance();

    ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        City.init(getResources());

        ArrayAdapter<City> cities = new ArrayAdapter<City>(this, R.layout.city_spinner_view);
        cities.addAll(City.getCitiesArray());

        AutoCompleteTextView citySelect = (AutoCompleteTextView) findViewById(R.id.cities);
        if (citySelect != null) {
            citySelect.setThreshold(1);
            citySelect.setOnItemClickListener(this);

            int cityId = settings.getInt(getResources().getString(R.string.pref_city_tgpt_id), -1);

            selectedCity = City.getCity(cityId);
            if (selectedCity != null) {
                citySelect.setText(selectedCity.getName());
                final City city = selectedCity;
                showProgressBar(true);
                Runnable updateCity = new Runnable() {
                    @Override
                    public void run() {
                        if (city.updateTGPTData(getApplicationContext())) {
                            updatePrices(city);
                            selectedCity = city;
                            showProgressBar(false);
                        }
                        else {
                            mExecutor.execute(this);
                        }
                    }
                };
                mExecutor.execute(updateCity);
            }
            citySelect.setAdapter(cities);
        }

        TimePicker timePicker = (TimePicker) findViewById(R.id.time_picker);
        if (timePicker != null) {
            int hour = settings.getInt(getResources().getString(R.string.pref_time_trigger_hour), -1);
            int minute = settings.getInt(getResources().getString(R.string.pref_time_trigger_minute), -1);
            if (hour != -1 && minute != -1) {
                timePicker.setCurrentHour(hour);
                timePicker.setCurrentMinute(minute);
            }
            timePicker.setOnTimeChangedListener(this);
        }
    }

    private void updateDynamicAlarm(boolean enable) {
        Intent i = new Intent();
        if (enable) {
            i.setAction(PushUpdateService.ACTION_CREATE_DYNAMIC_NOTIFICATION);
            i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES);
            i.putExtra(PushUpdateService.ALARM_INTERVAL_MILLIS, AlarmManager.INTERVAL_FIFTEEN_MINUTES);
        }
        else {
            i.setAction(PushUpdateService.ACTION_CANCEL_DYNAMIC_NOTIFICATION);
        }
        startService(i);
    }

    private void updatePrices(City city) {
        final City c = city;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView view = (TextView) findViewById(R.id.regular_price);
                view.setText("Regular Price: " + c.getRegularPrice());
            }
        });
    }

    protected void onPause() {
        super.onPause();

        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        AutoCompleteTextView citySelect = (AutoCompleteTextView) findViewById(R.id.cities);
        if (selectedCity != null) {
            editor.putInt(getResources().getString(R.string.pref_city_tgpt_id), selectedCity.getID());
            editor.putString(getResources().getString(R.string.pref_city_name), selectedCity.getName());
        }

        if (selectedTime != null /* && dailyNotifications*/) {
            editor.putInt(getResources().getString(R.string.pref_time_trigger_hour), selectedTime.get(Calendar.HOUR_OF_DAY));
            editor.putInt(getResources().getString(R.string.pref_time_trigger_minute), selectedTime.get(Calendar.MINUTE));
        }
        editor.commit();
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

        selectedTime.setTimeInMillis(System.currentTimeMillis());
        selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
        selectedTime.set(Calendar.MINUTE, minute);

        Intent i = new Intent(getApplicationContext(), PushUpdateService.class);
        i.setAction(PushUpdateService.ACTION_CREATE_STATIC_NOTIFICATION);
        i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, selectedTime.getTimeInMillis());
        i.putExtra(PushUpdateService.ALARM_INTERVAL_MILLIS, AlarmManager.INTERVAL_DAY);

        startService(i);
        Log.v(TAG, "Update notification set to " + hourOfDay + ":" + minute);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        final City city = (City) parent.getAdapter().getItem(pos);
        Runnable updateCity = new Runnable() {
            @Override
            public void run() {
                showProgressBar(true);

                if (city.updateTGPTData(getApplicationContext())) {
                    updatePrices(city);
                    selectedCity = city;
                    showProgressBar(false);
                }
                else {
                    mExecutor.execute(this);
                }
            }
        };

        mExecutor.execute(updateCity);

        InputMethodManager in = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        // getCurrentFocus() must come from activity: http://stackoverflow.com/a/17789187
        View focus = getCurrentFocus();
        if (focus != null) {
            in.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    private void showProgressBar(boolean enable) {
        final ProgressBar pBar = (ProgressBar) findViewById(R.id.progressBar);
        final boolean en = enable;
        if (pBar != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pBar.setVisibility(en? ProgressBar.VISIBLE : ProgressBar.GONE);
                }
            });
        }
    }
}
