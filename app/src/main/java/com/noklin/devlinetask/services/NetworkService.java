package com.noklin.devlinetask.services;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.noklin.devlinetask.network.CameraManager;

import java.net.MalformedURLException;

public class NetworkService extends Service{
    private static final String TAG = NetworkService.class.getSimpleName();
    private CameraManager mCameraManager;

    @Override
    public void onCreate() {
        super.onCreate();
        try{
            mCameraManager = new CameraManager();
        }catch (MalformedURLException ex){
            Log.d(TAG , "Exception while new CameraManager(): " + ex.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        public CameraManager getCameraManager() {
            return mCameraManager;
        }
    }

}
