package com.sebswebs.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class SettingsFragment extends PreferenceFragmentCompat {
    private boolean isValidURLOrEmpty(String s){
        try {
            new URL(s).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return s.trim().isEmpty();
        }
    }

    private void registerListener(EditTextPreference preference){
        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                if (isValidURLOrEmpty((String) newValue)) {
                    return true;
                } else {
                    Toast.makeText(requireContext(), "Provide a valid URL, starting with http:// or https://", Toast.LENGTH_LONG).show();
                    return  false;
                }
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        EditTextPreference databasePref = findPreference("patientDatabaseURL");
        EditTextPreference imageURLPref = findPreference("imageServerURL");
        assert databasePref != null;
        registerListener(databasePref);
        assert imageURLPref != null;
        registerListener(imageURLPref);
    }
}
