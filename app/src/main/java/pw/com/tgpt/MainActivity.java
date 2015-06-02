package pw.com.tgpt;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, TimePicker.OnTimeChangedListener, CompoundButton.OnCheckedChangeListener {
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
                final Context activityContext = this;
                showProgressBar(true);
                Runnable updateCity = new Runnable() {
                    @Override
                    public void run() {
                        if (city.updateTGPTData(activityContext)) {
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

        Calendar tempCalendar = Calendar.getInstance();
        int hour = settings.getInt(getResources().getString(R.string.pref_time_trigger_hour), tempCalendar.get(Calendar.HOUR));
        int minute = settings.getInt(getResources().getString(R.string.pref_time_trigger_minute), tempCalendar.get(Calendar.MINUTE));
        TimePicker timePicker = (TimePicker) findViewById(R.id.time_picker);
        if (timePicker != null) {
            timePicker.setIs24HourView(false);
            timePicker.setCurrentHour(hour);
            timePicker.setCurrentMinute(minute);

            timePicker.setOnTimeChangedListener(this);
        }
        selectedTime.set(Calendar.HOUR, hour);
        selectedTime.set(Calendar.MINUTE, minute);

        CheckBox dynamicUpdateCB = (CheckBox) findViewById(R.id.dynamic_update);
        if (dynamicUpdateCB != null) {
            boolean checked = settings.getBoolean(getResources().getString(R.string.pref_dynamic_update), false);
            dynamicUpdateCB.setChecked(checked);
            dynamicUpdateCB.setOnCheckedChangeListener(this);
        }

        CheckBox dailyUpdateCB = (CheckBox) findViewById(R.id.daily_update);
        if (dailyUpdateCB != null) {
            boolean checked = settings.getBoolean(getResources().getString(R.string.pref_daily_update), false);
            dailyUpdateCB.setChecked(checked);
            dailyUpdateCB.setOnCheckedChangeListener(this);
            if (timePicker != null) {
                timePicker.setEnabled(dailyUpdateCB.isChecked());
            }
        }
    }

    private void updateDynamicAlarm(boolean enable) {
        Intent i = new Intent(this, PushUpdateService.class);
        if (enable) {
            i.setAction(PushUpdateService.ACTION_CREATE_DYNAMIC_NOTIFICATION);
            i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES);
            i.putExtra(PushUpdateService.ALARM_INTERVAL_MILLIS, AlarmManager.INTERVAL_HALF_HOUR);
        }
        else {
            i.setAction(PushUpdateService.ACTION_CANCEL_DYNAMIC_NOTIFICATION);
        }
        startService(i);
    }

    private void updateDailyAlarm(boolean enable) {
        Intent i = new Intent(this, PushUpdateService.class);
        if (enable) {
            i.setAction(PushUpdateService.ACTION_CREATE_STATIC_NOTIFICATION);
            i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, selectedTime.getTimeInMillis());
            i.putExtra(PushUpdateService.ALARM_INTERVAL_MILLIS, AlarmManager.INTERVAL_DAY);
        }
        else {
            i.setAction(PushUpdateService.ACTION_CANCEL_STATIC_NOTIFICATION);
        }
        startService(i);
    }

    private void updatePrices(City city) {
        final City c = city;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView view = (TextView) findViewById(R.id.regular_price);
                StringBuffer text = new StringBuffer();
                text.append("Regular Price: ");
                text.append(c.getRegularPrice());
                text.append(", ");
                if (c.getDirection() != City.Direction.NO_CHANGE) {
                    text.append("going ");
                }
                text.append(c.getDirection().toString().toUpperCase());
                text.append("\nLast update: ");
                text.append(DateFormat.getDateInstance().format(c.getLastUpdate()));

                view.setText(text.toString());
            }
        });
    }

    protected void onPause() {
        super.onPause();

        SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        AutoCompleteTextView citySelect = (AutoCompleteTextView) findViewById(R.id.cities);
        CheckBox dynamicUpdateCB = (CheckBox) findViewById(R.id.dynamic_update);
        CheckBox dailyUpdateCB = (CheckBox) findViewById(R.id.daily_update);

        if (selectedCity != null) {
            editor.putInt(getResources().getString(R.string.pref_city_tgpt_id), selectedCity.getID());
            editor.putString(getResources().getString(R.string.pref_city_name), selectedCity.getName());
        }

        if (selectedTime != null /* && dailyNotifications*/) {
            editor.putInt(getResources().getString(R.string.pref_time_trigger_hour), selectedTime.get(Calendar.HOUR));
            editor.putInt(getResources().getString(R.string.pref_time_trigger_minute), selectedTime.get(Calendar.MINUTE));
        }

        if (dynamicUpdateCB != null) {
            editor.putBoolean(getResources().getString(R.string.pref_dynamic_update), dynamicUpdateCB.isChecked());
        }

        if (dailyUpdateCB != null) {
            editor.putBoolean(getResources().getString(R.string.pref_daily_update), dailyUpdateCB.isChecked());
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
        selectedTime.set(Calendar.HOUR, hourOfDay);
        selectedTime.set(Calendar.MINUTE, minute);

        updateDailyAlarm(true);

        Log.v(TAG, "Update notification set to " + hourOfDay + ":" + minute);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        final Context activityContext = this;
        final City city = (City) parent.getAdapter().getItem(pos);

        Runnable updateCity = new Runnable() {
            @Override
            public void run() {
                showProgressBar(true);
                if (city.updateTGPTData(activityContext)) {
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
       switch (buttonView.getId()) {
           case R.id.daily_update:
               TimePicker timePicker = (TimePicker) findViewById(R.id.time_picker);
               if (timePicker != null) {
                   timePicker.setEnabled(isChecked);
               }
               updateDailyAlarm(isChecked);
               break;
           case R.id.dynamic_update:
               updateDynamicAlarm(isChecked);
               break;
       }
    }
}
