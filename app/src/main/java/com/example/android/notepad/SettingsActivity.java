package com.example.android.notepad;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Exposes basic personalization options so users can tailor how the notebook looks and feels.
 */
public class SettingsActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_DARK_THEME = "pref_dark_theme";
    public static final String KEY_EDITOR_TEXT_SIZE = "pref_editor_text_size";

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        bindSummaryToValue(findPreference(KEY_EDITOR_TEXT_SIZE));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(value.toString());
            if (index >= 0) {
                preference.setSummary(listPreference.getEntries()[index]);
            }
        }
        return true;
    }

    private void bindSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        String currentValue = PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                .getString(preference.getKey(), "");
        onPreferenceChange(preference, currentValue);
    }
}

