package pw.com.tgpt;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    private AutoCompleteTextView mSearchView;
    private SwipeRefreshLayout mSwipeLayout;
    private ArrayList<City> mStarredCities;
    private InitStarredFragmentTask mInitStarredFragment;

    private class InitStarredFragmentTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;
        public InitStarredFragmentTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            mStarredCities = DBHelper.getInstance(mContext).getStarredCities();
            // TODO: Toast on error
            for (City city : mStarredCities) {
                city.updateTGPTData(mContext);
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
            CityAdapter cityAdapter = new CityAdapter(mContext, mStarredCities);

            setListAdapter(cityAdapter);
            cityAdapter.notifyDataSetChanged();
        }
    }

    private class UpdateStarredFragmentTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;

        public UpdateStarredFragmentTask(Context context) {
            super();
            mContext = context;
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (City city : mStarredCities)
                city.updateTGPTData(mContext);

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
            ((CityAdapter) getListAdapter()).notifyDataSetChanged();
            mSwipeLayout.setRefreshing(false);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.starred_fragment, container, false);
        mSwipeLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorSchemeResources(android.R.color.holo_red_light, android.R.color.holo_green_light,
                android.R.color.holo_blue_light);
        setHasOptionsMenu(true);

        return v;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        City city = (City) getListAdapter().getItem(position);
        CityFragment cityFragment = CityFragment.newInstance(city.getID());

        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_frame, cityFragment).addToBackStack(null).commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = (MainActivity) getActivity();
        activity.getToolbar().setTitle(getResources().getString(R.string.tgpt_prices));
        try {
            activity.getInitDataTask().get(10, TimeUnit.SECONDS);
            mInitStarredFragment = new InitStarredFragmentTask(getActivity());
            mInitStarredFragment.execute();
            mInitStarredFragment.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "initDataTask failed");
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        for (City city : mStarredCities)
            city.saveToDB(getActivity());
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

            ArrayList<City> list = new ArrayList<City>(City.getCitiesArray().values());
            ArrayAdapter<City> adapter = new ArrayAdapter<City>(getActivity(), R.layout.city_autocomplete_search_item, list);
            mSearchView.setAdapter(adapter);
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
                InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                // getCurrentFocus() must come from activity: http://stackoverflow.com/a/17789187
                View focus = getActivity().getCurrentFocus();
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
                InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
                // getCurrentFocus() must come from activity: http://stackoverflow.com/a/17789187
                View focus = getActivity().getCurrentFocus();
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

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().collapseActionView();

        CityFragment cityFragment = CityFragment.newInstance(city.getID());

        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment_frame, cityFragment).addToBackStack(null).commit();
    }

    @Override
    public void onRefresh() {
        Log.v(TAG, "Refresh triggered");
        if (mInitStarredFragment.getStatus() == AsyncTask.Status.FINISHED) {
            new UpdateStarredFragmentTask(getActivity()).execute();
        }
        else
            mSwipeLayout.setRefreshing(false);
    }
}
