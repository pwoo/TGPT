package pw.com.tgpt;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by PW on 2015-06-07.
 */
public class CityFragment extends Fragment {
    TextView mRegularPrice;

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

        mRegularPrice = (TextView) v.findViewById(R.id.city_fragment_regular_price);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        int cityId = getArguments().getInt("id");

        if (cityId != 0) {
            City city = City.getCity(cityId);
            if (city != null) {
                mRegularPrice.setText("RP: " + city.getRegularPrice());
            }
        }

    }
}
