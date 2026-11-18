package com.smartaqi.myapplication;

import android.app.Application;
import androidx.lifecycle.ProcessLifecycleOwner;

public class UserTracking extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // This registers the observer for the WHOLE app
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());
    }
}
