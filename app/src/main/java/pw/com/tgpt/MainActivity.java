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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends Activity implements AdapterView.OnItemClickListener, TimePicker.OnTimeChangedListener {
    private static final String TAG = "MAIN";
    private City selectedCity = null;
    ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        City.init(getResources());
        ArrayAdapter<City> cities = new ArrayAdapter<City>(this, R.layout.city_spinner_view);
        cities.addAll(City.getCitiesArray());

        AutoCompleteTextView citySelect = (AutoCompleteTextView) findViewById(R.id.cities);
        if (citySelect != null) {
            citySelect.setAdapter(cities);
            citySelect.setThreshold(1);
            citySelect.setOnItemClickListener(this);

            SharedPreferences settings = getSharedPreferences(getResources().getString(R.string.app_name), MODE_PRIVATE);
            int cityId = settings.getInt(getResources().getString(R.string.pref_city_tgpt_id), -1);

            selectedCity = City.getCity(cityId);
            if (selectedCity != null) {
                citySelect.setText(selectedCity.getName());
                final City city = selectedCity;
                Runnable updateCity = new Runnable() {
                    @Override
                    public void run() {
                        if (city.updateTGPTData(getApplicationContext())) {
                            updatePrices(city);
                            selectedCity = city;
                        }
                        else {
                            mExecutor.execute(this);
                        }
                    }
                };
                mExecutor.execute(updateCity);
            }
        }

        TimePicker timePicker = (TimePicker) findViewById(R.id.time_picker);
        if (timePicker != null) {
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
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);

        Intent i = new Intent(getApplicationContext(), PushUpdateService.class);
        i.setAction(PushUpdateService.ACTION_CREATE_STATIC_NOTIFICATION);
        i.putExtra(PushUpdateService.ALARM_TRIGGER_AT_MILLIS, calendar.getTimeInMillis());
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
                if (city.updateTGPTData(getApplicationContext())) {
                    updatePrices(city);
                    selectedCity = city;
                }
                else {
                    mExecutor.execute(this);
                }
            }
        };

        mExecutor.execute(updateCity);
    }
}
