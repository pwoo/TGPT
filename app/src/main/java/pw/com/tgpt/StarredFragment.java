package pw.com.tgpt;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by pwoo on 08/06/15.
 */
public class StarredFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener,
        MenuItemCompat.OnActionExpandListener {
    private static final String TAG = "SFR";
    private MainActivity mActivity;
    private AutoCompleteTextView mSearchView;
    private SwipeRefreshLayout mSwipeLayout;
    private ArrayList<City> mStarredCities;
    private InitStarredFragmentTask mInitStarredFragment;
    private UpdateStarredFragmentTask mUpdateStarredFragment;
    private final int mNavItemId = R.id.prices;

    private class InitStarredFragmentTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private boolean mIsCancelled = false;

        public InitStarredFragmentTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mStarredCities = DBHelper.getInstance(mContext).getStarredCities();

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mSwipeLayout.setRefreshing(true);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            mSwipeLayout.setRefreshing(false);
            if (!mIsCancelled) {
                CityAdapter cityAdapter = new CityAdapter(mContext, mStarredCities);

                setListAdapter(cityAdapter);
                cityAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            mIsCancelled = true;
        }
    }

    private class UpdateStarredFragmentTask extends AsyncTask<Void, Void, Void> {
        private final Context mContext;
        private boolean mIsCancelled = false;

        public UpdateStarredFragmentTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (City city : mStarredCities) {
                if (city.updateTGPTData(mContext)) {
                    city.getDynamicNotification().setLastNotify(city.getLastUpdate());
                    city.saveToDB(mContext);
                }
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mSwipeLayout.setRefreshing(true);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mSwipeLayout.setRefreshing(false);
            if (!mIsCancelled) {
                ((CityAdapter) getListAdapter()).notifyDataSetChanged();
            }
        }

        @Override
        protected void onCancelled(Void aVoid) {
            super.onCancelled(aVoid);
            mIsCancelled = true;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "onCreateView");
        View v = inflater.inflate(R.layout.starred_fragment, container, false);
        mSwipeLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_red_light, android.R.color.holo_green_light,
                android.R.color.holo_blue_light);
        setHasOptionsMenu(true);

        mActivity.selectNavigationItem(mNavItemId);
        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        City city = (City) getListAdapter().getItem(position);
        CityFragment cityFragment = CityFragment.newInstance(city.getID());

        mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_frame, cityFragment).addToBackStack(null).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
//        mSwipeLayout.setRefreshing(true);
        if (mActivity.getSupportActionBar() != null)
            mActivity.getSupportActionBar().setTitle(getResources().getString(R.string.tgpt_prices));
        try {
            mActivity.getInitDataTask().get();
            mInitStarredFragment = new InitStarredFragmentTask(mActivity);
            mInitStarredFragment.execute();
            mUpdateStarredFragment = new UpdateStarredFragmentTask(mActivity);
            mUpdateStarredFragment.execute();
            mSwipeLayout.setRefreshing(false);
        } catch (Exception e) {
            Log.e(TAG, "initDataTask failed");
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwipeLayout.setRefreshing(false);

        if (mInitStarredFragment != null)
            mInitStarredFragment.cancel(true);

        if (mUpdateStarredFragment != null)
            mUpdateStarredFragment.cancel(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(TAG, "onCreateOptionsMenu");
        inflater.inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.city_search);
        MenuItemCompat.setOnActionExpandListener(item, this);
        item.setVisible(true);

        mSearchView = (AutoCompleteTextView) item.getActionView().findViewById(R.id.search_autocomplete);
        if (mSearchView != null) {
            mSearchView.setOnItemClickListener(this);
            mSearchView.setSelectAllOnFocus(true);
            mSearchView.setThreshold(1);
            try {
                mInitStarredFragment.get(10, TimeUnit.SECONDS);
                SparseArray<City> sparseList = City.getCitiesArray();
                ArrayList<City> list = new ArrayList<City>(sparseList.size());
                for (int i = 0; i < sparseList.size(); i++) {
                    list.add(sparseList.valueAt(i));
                }
                ArrayAdapter<City> adapter = new ArrayAdapter<City>(mActivity, R.layout.city_autocomplete_search_item, list);
                mSearchView.setAdapter(adapter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        boolean result = false;
        switch (item.getItemId()) {
            case R.id.city_search:
                InputMethodManager in = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                // getCurrentFocus() must come from activity: http://stackoverflow.com/a/17789187
                View focus = mActivity.getCurrentFocus();
                if (focus != null) {
                    in.showSoftInput(focus, 0);
                    mSearchView.requestFocus();
                }
                result = true;
                break;
        }
        return result;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        boolean result = false;
        switch (item.getItemId()) {
            case R.id.city_search:
                InputMethodManager in = (InputMethodManager) mActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                // getCurrentFocus() must come from activity: http://stackoverflow.com/a/17789187
                View focus = mActivity.getCurrentFocus();
                if (focus != null) {
                    in.hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
                result = true;
                break;
        }
        return result;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        City city = (City) parent.getItemAtPosition(position);
        if (mActivity.getSupportActionBar() != null)
            mActivity.getSupportActionBar().collapseActionView();

        CityFragment cityFragment = CityFragment.newInstance(city.getID());

        mActivity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_frame, cityFragment).addToBackStack(null).commit();
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "Refresh triggered");
        if (mUpdateStarredFragment != null && mUpdateStarredFragment.getStatus() == AsyncTask.Status.FINISHED) {
            mUpdateStarredFragment = new UpdateStarredFragmentTask(mActivity);
            mUpdateStarredFragment.execute();
        }
        else
            mSwipeLayout.setRefreshing(false);
    }
}
