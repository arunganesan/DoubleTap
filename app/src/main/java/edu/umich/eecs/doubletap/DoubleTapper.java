package edu.umich.eecs.doubletap;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by Arun on 2/11/2015.
 */
public class DoubleTapper {
    private MyActivity activity;
    private SensorManager sm;
    private Sensor accelerometer;
    public boolean saved = false;

    // Debug variables
    private String TAG = "DTapper";
    public boolean debugging = false;
    private static String FOLDER = "twatch";

    //public ArrayList<ArrayList<Double>> debugData = new ArrayList<ArrayList<Double>>();

    final int DEBUG_UPTO = 1000;
    float [][] debugData = new float [DEBUG_UPTO][4];
    public int debugCounter = 0;
    public long startDebugTime;


    // Tap processors
    TapProcessing xP, yP, zP;

    public DoubleTapper (MyActivity activity) {
        this.activity = activity;

        //xP = new TapProcessing();
        //yP = new TapProcessing();
        //zP = new TapProcessing();

        sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void triggerEvent () {
        activity.flashColor("x");
        activity.flashColor("y");
        activity.flashColor("z");
    }

    public void startMonitor () {
        Log.v(TAG, "Starting monitor!");
        sm.registerListener(accelListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private SensorEventListener accelListener = new SensorEventListener() {
        public void onAccuracyChanged (Sensor sensor, int accuracy) {}
        public void onSensorChanged (SensorEvent event) {
            long time = System.currentTimeMillis();
            float x = event.values[0], y = event.values[1], z = event.values[2];
            //xP.addData(time, x);
            //yP.addData(time, y);
            //zP.addData(time, z);

            //if (xP.detectDoubleTap()) activity.flashColor("x");
            //if (yP.detectDoubleTap()) activity.flashColor("y");
            //if (zP.detectDoubleTap()) activity.flashColor("z");

            if (debugging) {
                if (debugCounter >= DEBUG_UPTO) {
                    //shutdown();
                    //activity.updateText("Done experiment");
                    saveDebug();
                    //triggerEvent();
                    debugging = false;
                    debugCounter = 0;
                } else {
                    debugData[debugCounter][0] = (float)(System.currentTimeMillis() - startDebugTime);
                    debugData[debugCounter][1] = x;
                    debugData[debugCounter][2] = y;
                    debugData[debugCounter][3] = z;
                    debugCounter ++;
                }
            }
        }
    };

    public void shutdown () {
        sm.unregisterListener(accelListener);
    }


    //
    // DEBUG TOOLS
    //
    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, FOLDER);
        if(!file.exists()) file.mkdirs();
        return file.getAbsolutePath() + "/recording." + System.currentTimeMillis() + ".txt";
    }

    public void saveDebug () {
        if (saved) return; saved = true;
        FileOutputStream out;
        String outFilename = getFilename();
        try {
            out = new FileOutputStream(outFilename);
            out.write("t\tx\ty\tz\n".getBytes());
            for (int i = 0; i < DEBUG_UPTO; i++) {
                String line = String.format("%f\t%f\t%f\t%f\n", debugData[i][0], debugData[i][1], debugData[i][2], debugData[i][3]);
                out.write(line.getBytes());
            }
            out.close();
            activity.updateText("Done saving to " + outFilename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
