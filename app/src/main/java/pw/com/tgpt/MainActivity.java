package pw.com.tgpt;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "TGPT";
    ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayAdapter<City> cities = new ArrayAdapter<City>(this, R.layout.city_spinner_view);
        try {
            XmlResourceParser parser = getResources().getXml(R.xml.cities);
            ArrayList<City> cityList = City.GenerateCities(parser);
            cities.addAll(cityList);
        }
        catch (Resources.NotFoundException e) {
            // TODO: Kill process / activity
        }

        Spinner spin = (Spinner) findViewById(R.id.cities);
        if (spin != null) {
            spin.setAdapter(cities);
            SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
            spin.setSelection(settings.getInt(getResources().getString(R.string.pref_selected_city_spin), 0));
            spin.setOnItemSelectedListener(this);
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        final City city = (City) parent.getItemAtPosition(pos);
        mExecutor.execute(new Runnable() {
                              @Override
                              public void run() {
                                  city.updateTGPTData(getApplicationContext());
                                  updatePrices(city);
                              }
                          }
        );
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

        SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
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
}
