package com.example.hello;

import android.app.Application;
import android.content.Context;

import com.example.hello.LocaleHelper;

public class MyApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }
}