package com.noklin.devlinetask.network;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.noklin.devlinetask.entities.Camera;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

public class CameraManager {
    private static final String TAG = CameraManager.class.getSimpleName();
    private static final String ROOT = "http://demo.devline.ru:1818";
    private static final String URL = "/cameras";
    private final Map<String, DownloadImagePeriodTask> mDownloadHub = new HashMap<>();
    private final CameraListXMLParser mParser = new CameraListXMLParser();
    private final Handler mBGHandler;
    private final Handler mUIHandler;


    public CameraManager() throws MalformedURLException {
        mUIHandler = new UIHandler();
        HandlerThread worker = new HandlerThread(BGHandler.class.getSimpleName()
                , android.os.Process.THREAD_PRIORITY_BACKGROUND);
        worker.start();
        mBGHandler = new BGHandler(worker.getLooper());
    }


    public void downloadList(CameraListDownloadCallback callback){
        Message msg = mBGHandler.obtainMessage(BGHandler.DOWNLOAD_CAMERA_LIST , callback);
        msg.sendToTarget();
    }

    public void downloadImage(ImageDownloadCallback callback){
        Message msg = mBGHandler.obtainMessage(BGHandler.DOWNLOAD_IMAGE_ONCE, callback);
        msg.sendToTarget();
    }

    public void stopDownloadWithRepeat(ImageDownloadCallback callback){
        DownloadImagePeriodTask task = mDownloadHub.remove(callback.getUrl());
        if(task != null)
            task.interrupt();
        mBGHandler.removeMessages(BGHandler.DOWNLOAD_IMAGE_MANY);
    }

    public void startDownloadWithRepeat(ImageDownloadCallback callback , long repeatPause){
        DownloadImagePeriodTask task = new DownloadImagePeriodTask(repeatPause , callback);
        mDownloadHub.put(callback.getUrl() , task);
        task.start();
    }

    class DownloadImagePeriodTask extends Thread{
        private final long mPause;
        private final ImageDownloadCallback mImageDownloadCallback;

        public DownloadImagePeriodTask(long pause, ImageDownloadCallback callback){
            mPause = pause;
            mImageDownloadCallback = callback;
        }

        @Override
        public void run() {
            try{
                while(!isInterrupted()){
                        TimeUnit.MILLISECONDS.sleep(mPause);
                    Message msg = mBGHandler.obtainMessage(BGHandler.DOWNLOAD_IMAGE_MANY, mImageDownloadCallback);
                    msg.sendToTarget();
                }
            }catch(InterruptedException ignore){
                /*NOP*/
            }
            Log.d(TAG , "Stop task");
        }
    }


    public abstract static class CameraListDownloadCallback{
        private List<Camera> mCameraList;

        public void setCameraList(List<Camera> cameraList) {
            mCameraList = cameraList;
        }

        public abstract void onDownload(List<Camera> list);
    }

    public abstract static class ImageDownloadCallback{
        private Bitmap mImage;

        public void setCameraList(Bitmap image) {
            mImage = image;
        }

        public abstract void onDownload(Bitmap image);
        public abstract String getUrl();
    }



    //                            HANDLERS PART

    private static class UIHandler extends Handler{
        public static final int LIST_DOWNLOADED = 1;
        public static final int IMAGE_DOWNLOADED = 2;
        public UIHandler(){
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == LIST_DOWNLOADED){
                CameraListDownloadCallback callback = (CameraListDownloadCallback)msg.obj;
                if(callback != null){
                    if(callback.mCameraList != null)
                        callback.onDownload(callback.mCameraList);
                }
            }else if(msg.what == IMAGE_DOWNLOADED){
                ImageDownloadCallback imageDownloadCallback = (ImageDownloadCallback)msg.obj;
                if(imageDownloadCallback != null)
                    if(imageDownloadCallback.mImage != null)
                        imageDownloadCallback.onDownload(imageDownloadCallback.mImage);
            }
        }
    }

    private class BGHandler extends Handler{
        public static final int DOWNLOAD_CAMERA_LIST = 1;
        public static final int DOWNLOAD_IMAGE_ONCE = 2;
        public static final int DOWNLOAD_IMAGE_MANY = 3;
        public BGHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.what == DOWNLOAD_CAMERA_LIST){
                CameraListDownloadCallback cameraDownloadCallback = (CameraListDownloadCallback)msg.obj;
                if(cameraDownloadCallback == null) return;
                List<Camera> list = null;
                try{
                    HttpURLConnection connection  = Utils.getConnection(ROOT + URL);
                    list = mParser.parse(connection.getInputStream());
                    cameraDownloadCallback.setCameraList(list);
                    Message msgToUI = mUIHandler.obtainMessage(UIHandler.LIST_DOWNLOADED , cameraDownloadCallback);
                    msgToUI.sendToTarget();
                }catch (IOException | SAXException | ParserConfigurationException ex){
                    Log.d(TAG, "Exception while downloadList: " + ex.getMessage());
                }
            }else if (msg.what == DOWNLOAD_IMAGE_ONCE || msg.what == DOWNLOAD_IMAGE_MANY){
                ImageDownloadCallback imageDownloadCallback = (ImageDownloadCallback)msg.obj;
                if(imageDownloadCallback == null) return;
                try{
                    HttpURLConnection connection  = Utils.getConnection(ROOT + imageDownloadCallback.getUrl());
                    Bitmap downloadedBitmap = BitmapFactory.decodeStream(connection.getInputStream());
                    imageDownloadCallback.setCameraList(downloadedBitmap);
                    Message msgToUI = mUIHandler.obtainMessage(UIHandler.IMAGE_DOWNLOADED , imageDownloadCallback);
                    msgToUI.sendToTarget();
                }catch(IOException ex){
                    Log.d(TAG, "Exception while download image : " + ex.getMessage());
                }
            }
        }
    }





    //                            XML PARSER PART

    static class CameraListXMLParser{
        public List<Camera> parse(InputStream in) throws IOException, SAXException, ParserConfigurationException{
            Log.d(TAG , "HERE");

            List<Camera> list = new ArrayList<>();
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc  = dBuilder.parse(in);
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("camera");
            for(int i = 0 ; i < nList.getLength() ; i++){
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if(eElement.getElementsByTagName("uri").getLength() == 0
                            ||eElement.getElementsByTagName("name").getLength() == 0
                            ||eElement.getElementsByTagName("width").getLength() == 0
                            ||eElement.getElementsByTagName("height").getLength() == 0
                            ||eElement.getElementsByTagName("pixel-aspect-ratio-x").getLength() == 0
                            ||eElement.getElementsByTagName("pixel-aspect-ratio-y").getLength() == 0
                            ||eElement.getElementsByTagName("image-uri").getLength() == 0
                            ||eElement.getElementsByTagName("video-uri").getLength() == 0
                            ||eElement.getElementsByTagName("streaming-uri").getLength() == 0
                            ||eElement.getElementsByTagName("osd-uri").getLength() == 0)
                        throw new IOException("Illegal xml format");

                    try{
                        Camera current = new Camera(
                                eElement.getElementsByTagName("uri").item(0).getTextContent()
                                ,eElement.getElementsByTagName("name").item(0).getTextContent()
                                , Integer.valueOf(eElement.getElementsByTagName("width").item(0).getTextContent())
                                , Integer.valueOf(eElement.getElementsByTagName("height").item(0).getTextContent())
                                , Integer.valueOf(eElement.getElementsByTagName("pixel-aspect-ratio-x").item(0).getTextContent())
                                , Integer.valueOf(eElement.getElementsByTagName("pixel-aspect-ratio-y").item(0).getTextContent())
                                , eElement.getElementsByTagName("image-uri").item(0).getTextContent()
                                , eElement.getElementsByTagName("video-uri").item(0).getTextContent()
                                , eElement.getElementsByTagName("streaming-uri").item(0).getTextContent()
                                , eElement.getElementsByTagName("osd-uri").item(0).getTextContent()
                        );
                        list.add(current);
                    }catch (NumberFormatException ex){
                        throw new IOException("Illegal xml format");
                    }
                }
            }
            return list;
        }
    }
}
