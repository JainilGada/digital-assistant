package com.example.jainil.swipeviewecample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Locale;


import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;


public class BarcodeActivity extends AppCompatActivity {

    float x1,y1,x2,y2;
    TextToSpeech textToSpeech;
    //ArrayList<String> strings;
    ArrayList<Barcode> barcodes;
    long mLastDownEventTime = 0;

    CameraSource mCameraSource;
    TextView mTextView;
    SurfaceView mCameraView;
    private static final String TAG = "Barcode Activity";
    private static final int requestPermissionID = 8080;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        mTextView = findViewById(R.id.text_view_barcode);
        mCameraView = findViewById(R.id.surfaceView_barcode);
        //strings = new ArrayList<String>();
        barcodes = new ArrayList<Barcode>();
        //strings.add("Align Camera against the Barcode");


        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {

                    textToSpeech.setLanguage(Locale.ENGLISH);
                } else if (status == TextToSpeech.ERROR) {
                    Toast.makeText(BarcodeActivity.this, "Error TTS", Toast.LENGTH_SHORT).show();
                }

                textToSpeech.speak("Barcode Reader Started", TextToSpeech.QUEUE_FLUSH, null);
            }

        });
        startBarcodeSource();
    }

    private void startBarcodeSource() {

        Toast.makeText(this, "Barcode Called", Toast.LENGTH_SHORT).show();
        final BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getApplicationContext()).build();

        if (!barcodeDetector.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        } else {

            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), barcodeDetector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(15f)
                    .build();

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(BarcodeActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                /**
                 * Release resources for cameraSource
                 */
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            //Set the TextRecognizer's Processor.
            barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */
                @Override
                public void receiveDetections(Detector.Detections<Barcode> detections) {
                    final SparseArray<Barcode> items = detections.getDetectedItems();
                    if (items.size() != 0 ){

                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
//                                for(int i=0;i<items.size();i++){
//                                    Barcode item = items.valueAt(i);
//                                    System.out.println("i : "+i+","+item.displayValue);
//                                    stringBuilder.append(item.displayValue);
//                                    stringBuilder.append("\n");
//                                }
                              //Log.e(TAG,"Size of item "+items.size());
                                stringBuilder.append(items.valueAt(0).displayValue);
                                barcodes.add(items.valueAt(0));
                                String text = stringBuilder.toString();
                                mTextView.setText(text);
                                //strings.add(text);

                            }
                        });
                    }
                }
            });
        }
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

                if(x2<   x1)
                {
                    Intent intent = new Intent(this,NoteActivity.class);
                    startActivity(intent);
                    finish();
                }
                else if (System.currentTimeMillis() - mLastDownEventTime < 1000){
                    System.out.println("Before Async Task");

                    RequestParams requestParams = new RequestParams();
                    String text = "scanning";
                    if (barcodes.size()==0) {
                        textToSpeech.speak("Align camera against barcode",TextToSpeech.QUEUE_FLUSH,null);
                        return false ;
                    }
                    Barcode barcode = barcodes.get(barcodes.size() - 1);
                    String t;
                    switch (barcode.valueFormat){
                        case Barcode.PHONE:
                            Barcode.Phone phone = barcode.phone;
                            t="";
                            for(int temp =0; temp < phone.number.length() ; temp++)
                            {
                                t = t + " " +phone.number.charAt(temp);
                            }
                            text = "phone number is : "+t;
                            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);

                            break;
                        case Barcode.WIFI:
                            Barcode.WiFi wiFi = barcode.wifi;
                            t= "";
                            for(int temp =0; temp < wiFi.password.length() ; temp++)
                            {
                                t = t + " " +wiFi.password.charAt(temp);
                            }
                            text = "Password of wifi  " + wiFi.ssid + " is " + t;
                            textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null);
                            break;
                        case Barcode.EAN_8:
                        case Barcode.EAN_13:
                        case Barcode.ISBN:
                        case Barcode.PRODUCT:
                            textToSpeech.speak("Getting results ...", TextToSpeech.QUEUE_FLUSH, null);
                            requestParams.put("ean",barcode.displayValue);
                            requestParams.put("upc","u");
                            RestApi.post("/file/product/",requestParams, new JsonHttpResponseHandler(){

                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    super.onSuccess(statusCode, headers, response);
                                    try {
                                        Log.e(TAG,response.getString("result"));
                                        mTextView.setText(response.getString("result"));
                                        textToSpeech.speak(response.getString("result"), TextToSpeech.QUEUE_FLUSH, null);

                                    } catch (JSONException e) {

                                        e.printStackTrace();
                                    }
                                }
                            });
                            break;
                        case Barcode.UPC_A:
                        case Barcode.UPC_E:
                            textToSpeech.speak("Getting results ...", TextToSpeech.QUEUE_FLUSH, null);

                            requestParams.put("upc",barcode.displayValue);
                            requestParams.put("ean","e");
                            RestApi.post("/file/product/",requestParams, new JsonHttpResponseHandler(){

                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    super.onSuccess(statusCode, headers, response);
                                    try {
                                        Log.e(TAG,response.getString("result"));
                                        mTextView.setText(response.getString("result"));
                                        textToSpeech.speak(response.getString("result"), TextToSpeech.QUEUE_FLUSH, null);

                                    } catch (JSONException e) {

                                        e.printStackTrace();
                                    }
                                }
                            });
                            break;

                    }


                    //requestParams.put("upc",strings.get(strings.size()-1));


                    }
                break;
        }
        return false;
    }



}
