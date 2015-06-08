package pw.com.tgpt;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener {
    private static final String TAG = "MAIN";

    private DrawerLayout mDrawerLayout;
    private ActionBar mActionBar;
    private NavigationView mNavigationView;
    private AutoCompleteTextView mSearchView;
    private InitDataTask mInitDataTask;

    private class InitDataTask extends AsyncTask<Context, Void, Boolean> {
        private Context mContext;

        @Override
        protected Boolean doInBackground(Context... params) {
            Boolean result = Boolean.FALSE;
            if (params[0] != null) {
                mContext = params[0];
                DBHelper.getInstance(mContext).getReadableDatabase();
                City.init(mContext);
                result = Boolean.TRUE;
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInitDataTask = new InitDataTask();
        mInitDataTask.execute(this);
        initActionBar();
        initDrawerLayout();
        initNavigationView();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        mSearchView = (AutoCompleteTextView) menu.findItem(R.id.city_search).getActionView().findViewById(R.id.search_autocomplete);
        if (mSearchView != null) {
            mSearchView.setOnItemClickListener(this);
            mSearchView.setThreshold(1);
            try {
                mInitDataTask.get(5, TimeUnit.SECONDS);
                mInitDataTask = null;

                ArrayAdapter<City> adapter = new ArrayAdapter<City>(this, R.layout.city_list_item, City.getCitiesArray());
                mSearchView.setAdapter(adapter);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowTitleEnabled(true);
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);
        }
    }

    private void initDrawerLayout() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    }

    private void initNavigationView() {
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);

        switch (menuItem.getItemId()) {
            case R.id.prices:
                break;
            case R.id.settings:
                break;
        }

        mDrawerLayout.closeDrawer(mNavigationView);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(mNavigationView);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        City city = (City) parent.getItemAtPosition(position);

        CityFragment cityFragment = CityFragment.newInstance(city.getID());

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_frame, cityFragment).addToBackStack(null).commit();
    }

    public void handleIntent(Intent intent) {

    }
}
