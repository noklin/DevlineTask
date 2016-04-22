package com.noklin.devlinetask;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.noklin.devlinetask.entities.Camera;
import com.noklin.devlinetask.network.CameraManager;
import com.noklin.devlinetask.services.NetworkService;

public class CameraActivity extends AppCompatActivity{
    private final String TAG = CameraActivity.class.getSimpleName();
    public static final String EXTRA_CAMERA = "camera";
    private Camera mCamera;
    private CameraManager mCameraManager;
    private boolean mBound;
    private ImageView mImageView;
    private CameraManager.ImageDownloadCallback mImageDownloadCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Intent intent = getIntent();
        mCamera = (Camera)intent.getSerializableExtra(EXTRA_CAMERA);
        mImageView = (ImageView)findViewById(R.id.iv_CameraImage);
        setTitle(mCamera.getName());
        mImageDownloadCallback = new CameraManager.ImageDownloadCallback() {
            @Override
            public void onDownload(Bitmap image) {
                mImageView.setImageBitmap(image);
            }

            @Override
            public String getUrl() {
                return mCamera.getImageUri();
            }
        };

        Log.d(TAG, "input: " + mCamera.getName());
    }



    private final ServiceConnection mChatServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCameraManager =  ((NetworkService.LocalBinder)service).getCameraManager();
            mBound = true;
            mCameraManager.startDownloadWithRepeat(mImageDownloadCallback , 1000l);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };


    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this , NetworkService.class);
        bindService(intent, mChatServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(mChatServiceConnection);
        if (mBound)
            mCameraManager.stopDownloadWithRepeat(mImageDownloadCallback);
        super.onStop();
    }
}
