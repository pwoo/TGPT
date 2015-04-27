package pw.com.tgpt;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private static final String TAG = "TGPT";
    ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private String JSONLink = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JSONLink = getResources().getString(R.string.json_url);
        generateCities();
    }

    private void generateCities() {
        int id = -1;
        String name = null;
        ArrayAdapter<City> cities = new ArrayAdapter<City>(this, R.layout.city_spinner_view);
        try {
            XmlResourceParser parser = getResources().getXml(R.xml.cities);
            try {
                boolean cityFound = false;
                while (parser.getEventType() != XmlResourceParser.END_DOCUMENT) {
                    switch (parser.getEventType()) {
                        case XmlResourceParser.START_TAG:
                            if (parser.getName().equals("city")) {
                                cityFound = true;
                                id = parser.getAttributeIntValue(0, -1);
                            }
                            break;
                        case XmlResourceParser.END_TAG:
                            if (cityFound) {
                                cityFound = false;
                                City city = new City(id, name);
                                cities.add(city);
//                                Log.d(TAG, "City: " + city.GetName() + "@" + city.GetID());
                            }
                            id = -1;
                            name = null;
                            break;
                        case XmlResourceParser.TEXT:
                            if (cityFound) {
                                name = parser.getText();
                            }
                            break;
                    }
                    parser.next();
                }
            } catch (XmlPullParserException | IOException e) {

            }
            finally {
                Spinner spin = (Spinner) findViewById(R.id.cities);
                if (spin != null) {
                    spin.setAdapter(cities);
                    SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
                    spin.setSelection(settings.getInt(getResources().getString(R.string.pref_city), 0));
                    spin.setOnItemSelectedListener(this);

                }

                if (parser != null) {
                    parser.close();
                }
            }
        }
        catch (Resources.NotFoundException e) {
            // TODO
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        final City city = (City) parent.getItemAtPosition(pos);
        mExecutor.execute(new Runnable() {
                              @Override
                              public void run() {
                                  try {
                                      URL url = new URL(GetJSONURL(city));
                                      URLConnection urlConnection = url.openConnection();
                                      InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                                      ReadJSON(in, city);
                                  } catch (NullPointerException | IOException e) {

                                  }
                              }
                          }
        );
    }

    private void ReadJSON(InputStream in, City city) throws NullPointerException, IOException {
        Double regularPrice = 0.0;
        StringBuilder result = new StringBuilder();
        byte [] buf = new byte[128];
        while (in.read(buf) != -1) {
            result.append(new String(buf));
        }
        result.trimToSize();
        try {
            JSONObject parser = new JSONObject(result.toString());
            parser = parser.getJSONObject("channel");
            parser = parser.getJSONObject("item");

            city.setRegularPrice(parser.getDouble(getResources().getString(R.string.regular_price)));
            UpdatePrices(city);

            Log.d(TAG, "RP: " + regularPrice);
        }
        catch (JSONException e) {
            // TODO
        }
    }

    public void UpdatePrices(City city) {
        final City c = city;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView view = (TextView) findViewById(R.id.news);
                view.setText("Regular Price: "+ c.getRegularPrice());
            }
        });
    }

    private String GetJSONURL(City c) {
        return JSONLink + c.GetID();
    }

    protected void onPause() {
        super.onPause();

        SharedPreferences settings = getSharedPreferences(TAG, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        Spinner spin = (Spinner) findViewById(R.id.cities);

        editor.putInt(getResources().getString(R.string.pref_city), spin.getSelectedItemPosition());
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
