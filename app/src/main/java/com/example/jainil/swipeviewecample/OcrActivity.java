package com.example.jainil.swipeviewecample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class OcrActivity extends AppCompatActivity {

    float x1,y1,x2,y2;
    TextToSpeech textToSpeech;
    ArrayList<String> strings;
    long mLastDownEventTime = 0;

    CameraSource mCameraSource;
    TextView mTextView;
    SurfaceView mCameraView;
    private static final String TAG = "OCR Activity";
    private static final int requestPermissionID = 8080;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        mTextView = findViewById(R.id.text_view_ocr);
        mCameraView = findViewById(R.id.surfaceView_ocr);
        strings = new ArrayList<String>();
        strings.add("Align Camera against the text");

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {

                    textToSpeech.setLanguage(Locale.ENGLISH);
                } else if (status == TextToSpeech.ERROR) {
                    Toast.makeText(OcrActivity.this, "Error TTS", Toast.LENGTH_SHORT).show();
                }

                textToSpeech.speak("Text Reader Started", TextToSpeech.QUEUE_FLUSH, null);
            }

        });

        startOCRSource();
    }

    private void startOCRSource() {

        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();


        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");

        } else {

            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
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
                        if (ActivityCompat.checkSelfPermission(OcrActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(OcrActivity.this,
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
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0 ){

                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i=0;i<items.size();i++){
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                String text = stringBuilder.toString();
                                mTextView.setText(text);
                                strings.add(text);

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

                if(x2<x1)
                {
                    Intent intent = new Intent(this,BarcodeActivity.class);
                    startActivity(intent);
                    finish();
                }
                else if (System.currentTimeMillis() - mLastDownEventTime < 1000){

                    textToSpeech.speak(strings.get(strings.size()-1), TextToSpeech.QUEUE_FLUSH, null);

                }
                break;
        }
        return false;
    }


}
