package edu.umich.eecs.doubletap;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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
    private static String FOLDER = "soundfield";
    public ArrayList<ArrayList<Double>> debugData = new ArrayList<ArrayList<Double>>();

    // Tap processors
    TapProcessing xP, yP, zP;

    public DoubleTapper (MyActivity activity) {
        this.activity = activity;
        sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void triggerEvent () {
        activity.flashColor("x");
        activity.flashColor("y");
        activity.flashColor("z");
    }

    public void startMonitor () {
        sm.registerListener(accelListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private SensorEventListener accelListener = new SensorEventListener() {
        public void onAccuracyChanged (Sensor sensor, int accuracy) {}
        public void onSensorChanged (SensorEvent event) {
            long time = System.currentTimeMillis();
            float x = event.values[0], y = event.values[1], z = Math.abs(event.values[2]);
            xP.addData(time, x); yP.addData(time, y); zP.addData(time, z);

            if (xP.detectDoubleTap()) activity.flashColor("x");
            if (yP.detectDoubleTap()) activity.flashColor("y");
            if (zP.detectDoubleTap()) activity.flashColor("z");

            if (debugging) {
                if (debugData.size() > 1000) {
                    //shutdown();
                    //activity.updateText("Done experiment");
                    saveDebug();
                    triggerEvent();
                } else {
                    ArrayList<Double> row = new ArrayList<Double>();
                    row.add((double) System.currentTimeMillis());
                    row.add((double) x);
                    row.add((double) y);
                    row.add((double) z);
                    debugData.add(row);
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
            for (ArrayList<Double> row : debugData) {
                String line = "" + row.get(0) + "\t" + row.get(1) + "\t" + row.get(2) + "\t" + row.get(3) + "\n";
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
