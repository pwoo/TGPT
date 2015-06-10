package pw.com.tgpt;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
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

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by pwoo on 08/06/15.
 */
public class StarredFragment extends ListFragment implements AdapterView.OnItemClickListener,
        MenuItemCompat.OnActionExpandListener {
    private static final String TAG = "SFR";
    private AutoCompleteTextView mSearchView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.starred_fragment, container, false);
        setHasOptionsMenu(true);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = (MainActivity) getActivity();
        activity.getToolbar().setTitle(getResources().getString(R.string.tgpt_prices));
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
                MainActivity activity = (MainActivity) getActivity();
                activity.getInitDataTask().get(5, TimeUnit.SECONDS);
                ArrayList<City> list = new ArrayList<City>(City.getCitiesArray().values());
                ArrayAdapter<City> adapter = new ArrayAdapter<City>(getActivity(), R.layout.city_autocomplete_search_item, list);
                mSearchView.setAdapter(adapter);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
            finally {
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
}
