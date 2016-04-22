package com.noklin.devlinetask;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.noklin.devlinetask.entities.Camera;
import com.noklin.devlinetask.network.CameraManager;
import com.noklin.devlinetask.services.NetworkService;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private AdapterInitializer mAdapterInitializer = new AdapterInitializer();
    private CameraManager mCameraManager;
    private CameraListFragment mCameraListFragment;
    private boolean mBound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraListFragment = (CameraListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_CameraList);

    }

    private void setAdapter(){
        if(mCameraManager != null){
            mCameraManager.downloadList(mAdapterInitializer);
        }
    }

    class AdapterInitializer extends CameraManager.CameraListDownloadCallback{
        @Override
        public void onDownload(List<Camera> list) {
            mCameraListFragment.setListAdapter(new CameraListAdapter(list));
        }
    }


    public static class CameraListFragment extends ListFragment{

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Camera cam = (Camera)getListAdapter().getItem(position);
            Intent intent = new Intent(getActivity() , CameraActivity.class);
            intent.putExtra(CameraActivity.EXTRA_CAMERA , cam);
            getActivity().startActivity(intent);
        }
    }

    public class CameraListAdapter extends ArrayAdapter<Camera> {

        public CameraListAdapter(List<Camera> albums) {
            super(MainActivity.this, 0, albums);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.camera, null);
            TextView tvName = (TextView) convertView.findViewById(R.id.tv_CameraName);
            final ImageView ivCameraImage = (ImageView) convertView.findViewById(R.id.iv_CameraImage);
            ivCameraImage.setImageResource(R.drawable.no_photo);
            final Camera camera = getItem(position);
            CameraManager.ImageDownloadCallback imageDownloadCallback = new CameraManager.ImageDownloadCallback() {
                @Override
                public void onDownload(Bitmap image) {
                    ivCameraImage.setImageBitmap(image);
                }

                @Override
                public String getUrl() {
                    return camera.getImageUri();
                }
            };
            mCameraManager.downloadImage(imageDownloadCallback);
            tvName.setText(camera.getName());

            return convertView;
        }
    }




    private final ServiceConnection mChatServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCameraManager =  ((NetworkService.LocalBinder)service).getCameraManager();
            mBound = true;
            setAdapter();
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
        super.onStop();
    }

}
