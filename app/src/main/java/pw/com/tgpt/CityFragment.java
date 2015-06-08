package pw.com.tgpt;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Created by PW on 2015-06-07.
 */
public class CityFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "CITYFRAG";
    SwipeRefreshLayout mSwipeLayout;
    TextView mLastUpdate;
    TextView mRegularPrice;
    TextView mRegularDiff;
    ImageView mDirection;
    private City mCity;

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
                DecimalFormat formatter = new DecimalFormat("###.##");
                SimpleDateFormat dateFormatter = new SimpleDateFormat("EEEE, LLL d yyyy", Locale.CANADA);

                dateFormatter.format(mCity.getLastUpdate().getTime(), dateBuf, new FieldPosition(0));
                mLastUpdate.setText(dateBuf.toString());
            }

            int regularDiff = (int) mCity.getRegularDiff();
            if (regularDiff != 0) {
                StringBuffer regDiffBuf = new StringBuffer();
                regDiffBuf.append(mCity.getDirection().toString());
                regDiffBuf.append(" ");
                regDiffBuf.append(regularDiff);
                regDiffBuf.append(" ");
                regDiffBuf.append(getResources().getString(R.string.units));
                mRegularDiff.setText(regDiffBuf.toString());
            }

            int color;
            int directionRes;
            switch (mCity.getDirection()) {
                case UP:
                    color = mContext.getResources().getColor(android.R.color.holo_red_light);
                    directionRes = R.drawable.ic_trending_up_48dp;
                    break;
                case DOWN:
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

            mDirection.setImageResource(directionRes);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.city_fragment, container, false);

        mSwipeLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_container);
        mLastUpdate = (TextView) v.findViewById(R.id.city_fragment_last_update);
        mRegularPrice = (TextView) v.findViewById(R.id.city_fragment_regular_price);
        mRegularDiff = (TextView) v.findViewById(R.id.city_fragment_regular_diff);
        mDirection = (ImageView) v.findViewById(R.id.city_fragment_direction);

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
    public void onRefresh() {
        Log.v("TAG", "Refresh triggered");
        handleCity();
    }

    public void handleCity() {
        if (mCity != null) {
            UpdateCityTask updateTask = new UpdateCityTask(getActivity());
            updateTask.execute(mCity);

            try {
                if (updateTask.get() == Boolean.FALSE) {
                    Toast toast = Toast.makeText(getActivity(), getResources().getString(R.string.update_failed), Toast.LENGTH_SHORT);
                    toast.show();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
