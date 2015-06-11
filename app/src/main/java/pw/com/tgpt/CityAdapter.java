package pw.com.tgpt;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by PW on 2015-06-10.
 */
public class CityAdapter extends ArrayAdapter<City> {
    private static final String TAG = "CADR";
    private Context mContext;
    private int mResource = R.layout.starred_item;
    private LayoutInflater  mInflater;

    public CityAdapter(Context context, List<City> objects) {
        super(context, R.layout.starred_item, objects);

        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        City city = getItem(position);
        View view;
        TextView name;
        TextView regularPrice;
        TextView regularDiff;
        TextView lastUpdate;
        StringBuilder displayDiff = new StringBuilder();

        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE MM/yy");
        DecimalFormat decimalFormatter = new DecimalFormat(view.getResources().getString(R.string.decimal_format));
        name = (TextView) view.findViewById(R.id.starred_city_name);
        name.setText(city.getName());

        regularPrice = (TextView) view.findViewById(R.id.starred_city_reg_price);
        regularPrice.setText(new Double(city.getRegularPrice()).toString());

        regularDiff = (TextView) view.findViewById(R.id.starred_city_reg_diff);

        switch (city.getDirection()) {
            case UP:
                regularDiff.setBackgroundResource(android.R.color.holo_red_light);
                displayDiff.append('+');
                break;
            case DOWN:
                regularDiff.setBackgroundResource(android.R.color.holo_green_light);
                displayDiff.append('-');
                break;
            case NO_CHANGE:
                regularDiff.setBackgroundResource(R.color.light_grey);
                break;
        }
        displayDiff.append(decimalFormatter.format(city.getRegularDiff()));
        regularDiff.setText(displayDiff.toString());

        if (city.getLastUpdate() != null) {
            lastUpdate = (TextView) view.findViewById(R.id.starred_city_date);
            lastUpdate.setText(dateFormatter.format(city.getLastUpdate().getTime()));
        }
        return view;
    }

}
