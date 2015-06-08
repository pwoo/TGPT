package pw.com.tgpt;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by pwoo on 08/06/15.
 */
public class StarredFragment extends ListFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.starred_fragment, container, false);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity activity = (MainActivity) getActivity();
        activity.getToolbar().setTitle(getResources().getString(R.string.tgpt_prices));
    }
}
