package com.sebswebs.myapplication;


import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.widget.Toolbar;

public class Settings extends BaseMenus {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
}
