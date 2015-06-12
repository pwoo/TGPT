package pw.com.tgpt;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MAIN";
    private DrawerLayout mDrawerLayout;
    private ActionBar mActionBar;
    private NavigationView mNavigationView;
    private InitDataTask mInitDataTask;

    public class InitDataTask extends AsyncTask<Context, Void, Boolean> {
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

        initActionBar();
        initDrawerLayout();
        initNavigationView();

        mInitDataTask = new InitDataTask();
        mInitDataTask.execute(this);

        initStarredFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mInitDataTask != null)
            mInitDataTask.cancel(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.v(TAG, "onCreateOptionsMenu");
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.v(TAG, "onPrepareOptionsMenu");
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

    private void initStarredFragment() {
        StarredFragment fragment = new StarredFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_frame, fragment).commit();
    }

    private void initNavigationView() {
        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        Fragment fragment = null;
        switch (menuItem.getItemId()) {
            case R.id.prices:
                fragment = new StarredFragment();
                break;
            case R.id.settings:
                fragment = new SettingsFragment();
                break;
        }
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_frame, fragment).addToBackStack(null).commit();
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

    public InitDataTask getInitDataTask() { return mInitDataTask; }
}
