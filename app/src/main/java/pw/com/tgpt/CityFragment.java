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
import android.widget.TextView;

import java.util.concurrent.ExecutionException;

/**
 * Created by PW on 2015-06-07.
 */
public class CityFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = "CITYFRAG";
    SwipeRefreshLayout mSwipeLayout;
    TextView mRegularPrice;

    private class UpdateCityTask extends AsyncTask<City, Void, Boolean> {
        private Context mContext;
        private City mCity;
        private int mDefaultColor;

        public UpdateCityTask(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(City... params) {
            mCity = params[0];
            if (mCity != null && mContext != null) {
                mCity.updateTGPTData(mContext);
            }

            return Boolean.TRUE;
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
            int color;
            switch (mCity.getDirection()) {
                case UP:
                    color = mContext.getResources().getColor(android.R.color.holo_red_light);
                    break;
                case DOWN:
                    color = mContext.getResources().getColor(android.R.color.holo_green_light);
                    break;
                default:
                    color =  mDefaultColor;
                    break;
            }
            mRegularPrice.setTextColor(color);
            mRegularPrice.setText(new Double(mCity.getRegularPrice()).toString());
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
        mRegularPrice = (TextView) v.findViewById(R.id.city_fragment_regular_price);

        mSwipeLayout.setOnRefreshListener(this);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        handleCity();
    }

    @Override
    public void onRefresh() {
        Log.v("TAG", "Refresh triggered");
        handleCity();
    }

    public void handleCity() {
        int cityId = getArguments().getInt("id");

        if (cityId != 0) {
            City city = City.getCity(cityId);
            UpdateCityTask updateTask = new UpdateCityTask(getActivity());
            updateTask.execute(city);

            try {
                if (updateTask.get() == Boolean.FALSE) {
                    // Retrieval failed -- add retry?
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}
