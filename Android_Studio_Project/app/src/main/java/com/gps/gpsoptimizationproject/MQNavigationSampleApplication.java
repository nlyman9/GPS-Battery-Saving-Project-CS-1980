package com.gps.gpsoptimizationproject;

import android.app.Application;
//import android.support.annotation.NonNull;

import androidx.annotation.NonNull;

import com.mapquest.mapping.MapQuest;
import com.mapquest.navigation.location.LocationProviderAdapter;
import com.gps.gpsoptimizationproject.GoogleLocationProviderAdapter;

public class MQNavigationSampleApplication extends Application {
    @NonNull
    private LocationProviderAdapter mLocationProviderAdapter;

    @Override
    public void onCreate() {
        super.onCreate();

        MapQuest.start(getApplicationContext());

        mLocationProviderAdapter = new GoogleLocationProviderAdapter(this);
    }

    @NonNull
    public LocationProviderAdapter getLocationProviderAdapter() {
        return mLocationProviderAdapter;
    }

    public void initializeLocationProviderAdapter() {
        mLocationProviderAdapter.initialize();
    }
}
