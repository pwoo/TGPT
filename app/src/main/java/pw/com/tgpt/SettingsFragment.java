package pw.com.tgpt;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SettingsFragment extends Fragment implements android.widget.CompoundButton.OnCheckedChangeListener {
    private MainActivity mActivity;
    private Switch mSwitch;
    private boolean mDisableNotifications = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SettingsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_fragment, container, false);

        mSwitch = (Switch) v.findViewById(R.id.settings_disable_notifications_toggle);
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.setChecked(mDisableNotifications);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mActivity.getSupportActionBar() != null)
            mActivity.getSupportActionBar().setTitle(getResources().getString(R.string.tgpt_settings));
    }

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        SharedPreferences.Editor prefEditor = pref.edit();
        prefEditor.putBoolean(getResources().getString(R.string.setting_disable_notifications), mDisableNotifications);
        prefEditor.apply();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (MainActivity) activity;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mDisableNotifications = pref.getBoolean(getResources().getString(R.string.setting_disable_notifications), false);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.settings_disable_notifications_toggle:
                mDisableNotifications = isChecked;
                break;
        }
    }
}
