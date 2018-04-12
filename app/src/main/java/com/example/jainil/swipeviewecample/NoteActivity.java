package com.example.jainil.swipeviewecample;



import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;


public class NoteActivity extends AppCompatActivity {


    private static String TAG = "Note Activity";
    TextView mTextView;
    SurfaceView surfaceView;
    private Camera mCamera;
    private CameraPreview mPreview;
    TextToSpeech textToSpeech;
    private long mLastDownEventTime;
    float x1,x2,y1,y2;
    Camera.PictureCallback mPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        mTextView = findViewById(R.id.text_view_note);
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {

                    textToSpeech.setLanguage(Locale.ENGLISH);
                } else if (status == TextToSpeech.ERROR) {
                    Toast.makeText(NoteActivity.this, "Error TTS", Toast.LENGTH_SHORT).show();
                }

                textToSpeech.speak("Currency Detector started", TextToSpeech.QUEUE_FLUSH, null);
            }

        });


          mPicture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (pictureFile == null){
                    Log.d(TAG, "Error creating media file, check storage permissions: " );
                    return;
                }

                camera.startPreview();

                try {
                    textToSpeech.speak("Getting results ...", TextToSpeech.QUEUE_FLUSH, null);

                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    RequestParams requestParams = new RequestParams();
                    requestParams.put("file",pictureFile);
                    RestApi.post("/file/upload/",requestParams, new JsonHttpResponseHandler(){

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            super.onSuccess(statusCode, headers, response);
                            try {
                                Log.e(TAG,response.getString("result"));
                                mTextView.setText(response.getString("result"));
                                textToSpeech.speak(response.getString("result"), TextToSpeech.QUEUE_FLUSH, null);

                            } catch (JSONException e) {
                                Log.d(TAG,"ERRRoorr");
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, "Error accessing file: " + e.getMessage());
                }
            }
        };


        checkCameraHardware(NoteActivity.this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);
        }
        else {

            mCamera = getCameraInstance();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 50);
        }
        else {


        }
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);



    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            Toast.makeText(context, "Camera is there in this device", Toast.LENGTH_SHORT).show();
            Log.d(TAG,"@@@@@@@Camera exist");
            return true;
        } else {
            Toast.makeText(context, "Camera is not there in this device", Toast.LENGTH_SHORT).show();
            Log.d(TAG,"@@@@@@@Camera  do notexist");
            return false;
        }


    }

    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK); // attempt to get a Camera instance
            Log.d(TAG,"@@@@Camera opened ");
            Camera.Parameters parameters = c.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            parameters.setJpegQuality(100);
            List<Camera.Size> supportedSize = parameters.getSupportedPictureSizes();
            Camera.Size bestSize = supportedSize.get(0);
            for(int i=1;i<supportedSize.size();i++)
            {
                if((supportedSize.get(i).width*supportedSize.get(i).height)>(bestSize.width*bestSize.height))
                {
                    bestSize = supportedSize.get(i);
                }
            }

            List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
            Iterator<Integer> supportedPreviewFormatsInterator = (Iterator<Integer>) supportedPreviewFormats.iterator();
            while (supportedPreviewFormatsInterator.hasNext())
            {
                Integer previewFormat = supportedPreviewFormatsInterator.next();
                if (previewFormat == ImageFormat.YV12)
                {
                    parameters.setPreviewFormat(previewFormat);
                }
            }

            parameters.setPictureSize(bestSize.width,bestSize.height);
            parameters.setPreviewSize(bestSize.width,bestSize.height);

            c.setParameters(parameters);
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.d(TAG,"@@@@Camera not opened ");
            Log.d(TAG,"@@@@"+e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }


    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setDisplayOrientation(90);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Digital Assistant");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("Digital Assistant", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }
        Log.d("***************",mediaFile.getAbsolutePath());
         return mediaFile;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {


        switch (event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                mLastDownEventTime = System.currentTimeMillis();
                x1 = event.getX();
                y1 = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                x2=event.getX();
                y2=event.getY();

                if(x2<x1)
                {
                    Intent intent = new Intent(this,OcrActivity.class);
                    startActivity(intent);
                    finish();
                }
                else if (System.currentTimeMillis() - mLastDownEventTime < 1000){

                    // get an image from the camera
                    mCamera.takePicture(null, null, mPicture);
                }
                break;
        }
        return false;
    }

}
