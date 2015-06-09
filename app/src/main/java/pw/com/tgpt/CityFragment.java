package pw.com.tgpt;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by PW on 2015-06-07.
 */
public class CityFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, MenuItem.OnMenuItemClickListener {
    private static final String TAG = "CFR";
    private SwipeRefreshLayout mSwipeLayout;
    private TextView mLastUpdate;
    private TextView mRegularPrice;
    private TextView mRegularDiffLabel;
    private TextView mRegularDiff;
    private TextView mLastWeek;
    private TextView mLastMonth;
    private TextView mLastYear;
    private ImageView mDirection;
    private TextView mCurrentDate;

    private City mCity;
    private Notification mDynamicNotification;

    private class UpdateCityTask extends AsyncTask<City, Void, Boolean> {
        private Context mContext;
        private City mCity;
        private int mDefaultColor;

        public UpdateCityTask(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(City... params) {
            Boolean result = Boolean.FALSE;
            mCity = params[0];
            if (mCity != null && mContext != null) {

                result = mCity.updateTGPTData(mContext);
            }

            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mDefaultColor = mRegularPrice.getCurrentTextColor();
            mSwipeLayout.setRefreshing(true);
        }

        @Override
        protected void onPostExecute(Boolean aBool) {
            mSwipeLayout.setRefreshing(false);

            if (mCity.getLastUpdate() != null) {
                StringBuffer dateBuf = new StringBuffer();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, LLL d yyyy", Locale.CANADA);

                dateFormatter.format(mCity.getLastUpdate().getTime(), dateBuf, new FieldPosition(0));
                mLastUpdate.setText(dateBuf.toString());
            }

            if (mCity.getCurrentDate() != null) {
                StringBuffer dateBuf = new StringBuffer();
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mmZ", Locale.CANADA);

                dateFormatter.format(mCity.getCurrentDate().getTime(), dateBuf, new FieldPosition(0));
                mCurrentDate.setText("Last updated at " + dateBuf.toString());
            }

            int color;
            int directionRes;
            int regularDiff = (int) mCity.getRegularDiff();
            switch (mCity.getDirection()) {
                case UP:
                    color = mContext.getResources().getColor(android.R.color.holo_red_light);
                    directionRes = R.drawable.ic_trending_up_48dp;
                    break;
                case DOWN:
                    regularDiff = -regularDiff;
                    color = mContext.getResources().getColor(android.R.color.holo_green_light);
                    directionRes = R.drawable.ic_trending_down_48dp;
                    break;
                default:
                    color =  mDefaultColor;
                    directionRes = R.drawable.ic_trending_flat_black_48dp;
                    break;
            }
            mRegularPrice.setTextColor(color);
            mRegularPrice.setText(new Double(mCity.getRegularPrice()).toString());


            if (regularDiff != 0) {
                int labelResId = (regularDiff > 1 || regularDiff < -1)? R.string.units : R.string.unit;
                mRegularDiffLabel.setText(getResources().getString(labelResId));
                mRegularDiff.setTextColor(color);
                mRegularDiff.setText(new Integer(regularDiff).toString());
            }

            mLastWeek.setText(new Double(mCity.getLastWeekRegular()).toString());
            mLastMonth.setText(new Double(mCity.getLastMonthRegular()).toString());
            mLastYear.setText(new Double(mCity.getLastYearRegular()).toString());

            mDirection.setImageResource(directionRes);

            if (aBool.booleanValue() == false) {
                Toast toast = Toast.makeText(mContext, getResources().getString(R.string.update_failed), Toast.LENGTH_SHORT);
                toast.show();
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }
    }

    private class PersistDataTask extends AsyncTask<City, Void, Void> {
        @Override
        protected Void doInBackground(City... params) {
            City city = params[0];
            if (city != null) {
                city.saveToDB(getActivity());
            }
            return null;
        }
    }

    public static CityFragment newInstance(int id) {
        CityFragment cityFragment = new CityFragment();

        Bundle args = new Bundle();
        args.putInt("id", id);
        cityFragment.setArguments(args);

        return cityFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.city_fragment, container, false);

        mSwipeLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mLastUpdate = (TextView) v.findViewById(R.id.city_fragment_last_update);
        mCurrentDate = (TextView) v.findViewById(R.id.city_fragment_current_date);
        mRegularPrice = (TextView) v.findViewById(R.id.city_fragment_regular_price);
        mRegularDiff = (TextView) v.findViewById(R.id.city_fragment_regular_diff);
        mRegularDiffLabel = (TextView) v.findViewById(R.id.city_fragment_regular_diff_label);
        mDirection = (ImageView) v.findViewById(R.id.city_fragment_direction);
        mLastWeek = (TextView) v.findViewById(R.id.city_fragment_last_week);
        mLastMonth = (TextView) v.findViewById(R.id.city_fragment_last_month);
        mLastYear = (TextView) v.findViewById(R.id.city_fragment_last_year);

        mSwipeLayout.setOnRefreshListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        int cityId = getArguments().getInt("id");
        mCity = City.getCity(cityId);
        MainActivity activity = (MainActivity) getActivity();
        activity.getToolbar().setTitle(mCity.getName());

        handleCity();
    }

    @Override
    public void onPause() {
        super.onPause();

        new PersistDataTask().execute(mCity);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        int starResId = mCity.getStarred()? R.drawable.ic_star_24dp : R.drawable.ic_star_border_black_24dp;

        inflater.inflate(R.menu.city_menu, menu);
        MenuItem citySettings = menu.findItem(R.id.city_settings);
        MenuItem cityStarred = menu.findItem(R.id.city_starred);

        citySettings.setOnMenuItemClickListener(this);
        cityStarred.setIcon(starResId);
        cityStarred.setOnMenuItemClickListener(this);
    }

    @Override
    public void onRefresh() {
        Log.v("TAG", "Refresh triggered");
        handleCity();
    }

    public void handleCity() {
        if (mCity != null) {
            UpdateCityTask updateTask = new UpdateCityTask(getActivity());
            updateTask.execute(mCity);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        boolean result = false;
        switch (item.getItemId()) {
            case R.id.city_settings:
                result = true;
                break;
            case R.id.city_starred:
                toggleStar(item);
                result = true;
                break;
            default:
        }
        return result;
    }

    public void toggleStar(MenuItem item) {
        boolean toggle = !mCity.getStarred();
        int starResId = toggle? R.drawable.ic_star_24dp : R.drawable.ic_star_border_black_24dp;
        mCity.setStarred(toggle);
        item.setIcon(starResId);
    }
}
