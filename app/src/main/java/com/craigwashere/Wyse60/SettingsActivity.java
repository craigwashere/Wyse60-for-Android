package com.craigwashere.Wyse60;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.craigwashere.Wyse60.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat //implements //SharedPreferences.OnSharedPreferenceChangeListener,
           // Preference.OnPreferenceChangeListener
    {
        private static final String TAG = "SettingsFragment";
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference font_preference = findPreference(getString(R.string.font_size_key));

            font_preference.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener(){
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue){
                            Log.d(TAG, "onPreferenceChange: newValue = " + newValue.toString());

                            return true;
                        }
                    }
            );
            Log.d(TAG, "onCreatePreferences: this;" + this.toString());
        }
    }
}