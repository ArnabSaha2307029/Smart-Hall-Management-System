package com.example.smarthall.ui.base;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import com.example.smarthall.util.ThemeHelper;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String LOG_TAG = "HallLifecycle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, getClass().getSimpleName() + " -> onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, getClass().getSimpleName() + " -> onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, getClass().getSimpleName() + " -> onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, getClass().getSimpleName() + " -> onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, getClass().getSimpleName() + " -> onStop");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LOG_TAG, getClass().getSimpleName() + " -> onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, getClass().getSimpleName() + " -> onDestroy");
    }

    public int getThemeColor(int attrRes, int defaultColor) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getTheme().resolveAttribute(attrRes, typedValue, true)) {
            return typedValue.data;
        }
        return defaultColor;
    }
}