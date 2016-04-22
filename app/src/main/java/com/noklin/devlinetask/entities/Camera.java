package com.noklin.devlinetask.entities;


import android.graphics.Bitmap;

import java.io.IOException;
import java.io.Serializable;


public class Camera implements Serializable{

    private final String mUri;
    private final String mName;
    private final int mWidth;
    private final int mHeight;
    private final int mPixelAspectRatioX;
    private final int mPixelAspectRatioY;
    private final String mImageUri;
    private final String mVideoUri;
    private final String mStreamingUri;
    private final String mOsdUri;


    public Camera(String uri, String name, int width, int height, int pixelAspectRatioX
            , int pixelAspectRatioY, String imageUri, String videoUri, String streamingUri
            , String osdUri) {

        mUri = uri;
        mName = name;
        mWidth = width;
        mHeight = height;
        mPixelAspectRatioX = pixelAspectRatioX;
        mPixelAspectRatioY = pixelAspectRatioY;
        mImageUri = imageUri;
        mVideoUri = videoUri;
        mStreamingUri = streamingUri;
        mOsdUri = osdUri;
    }


    public String getUri() {
        return mUri;
    }

    public String getName() {
        return mName;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getPixelAspectRatioX() {
        return mPixelAspectRatioX;
    }

    public int getPixelAspectRatioY() {
        return mPixelAspectRatioY;
    }

    public String getImageUri() {
        return mImageUri;
    }

    public String getVideoUri() {
        return mVideoUri;
    }

    public String getStreamingUri() {
        return mStreamingUri;
    }

    public String getOsdUri() {
        return mOsdUri;
    }

    public void getPicture(PictureDownloadCallback callback){

    }

    public interface PictureDownloadCallback{
        void onSuccess(Bitmap picture) throws IOException;
    }
}
