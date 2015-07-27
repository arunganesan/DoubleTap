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
    public boolean debugging = false;
    private static String FOLDER = "soundfield";
    private long last_triggered = 0;
    private int trigger_delay = 5000;
    private float THRESH = 2;

    private long last_peak = 0;

    private String TAG = "DTapper";
    int bufferIndex = 0;
    private float [] cb = new float [100];
    public ArrayList<ArrayList<Double>> debugData = new ArrayList<ArrayList<Double>>();

    public DoubleTapper (MyActivity activity) {
        this.activity = activity;
        sm = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void triggerEvent () {
        if (System.currentTimeMillis() > last_triggered + trigger_delay) {
            last_triggered = System.currentTimeMillis();
            (new Thread(eventTrigger)).start();
        }
    }

    public void startMonitor () {
        sm.registerListener(accelListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    Runnable eventTrigger = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < 4; i++) {
                try { Thread.currentThread().sleep(250); } catch (Exception e) { }
                activity.flagStart();
                try { Thread.currentThread().sleep(250); } catch (Exception e) { }
                activity.flagEnd();
            }
        }
    };

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


    private SensorEventListener accelListener = new SensorEventListener() {
        public void onAccuracyChanged (Sensor sensor, int accuracy) {}
        public void onSensorChanged (SensorEvent event) {
            float x = event.values[0], y = event.values[1], z = Math.abs(event.values[2]);
            // 1. Add to respective buffer, interpolate as needed
            // 2. Detect peaks and valleys using port of Matlab code
            // 3. Find basins (can be merged into previous code)
            // 4. Filter basins by variance and size
            // 5. Remove very nearby adjacent peaks
            // 6. Run through double-peak detection

            // Steps 2 - 4 can be done continuously and don't have to be
            // repeated everytime we add a new set of points. The stitch
            // point for those is simply from the last peak -- max or min
            // The stitch point for basins is from the last ending basin (min peak)


            cb[++bufferIndex % cb.length] = z;
            if (Math.abs(z - cb[(bufferIndex-1) % cb.length]) > THRESH) {
                long curr_time = System.currentTimeMillis();
                Log.v(TAG, "Thresh exceeded at " + curr_time);
                activity.flagStart();
                //if (curr_time > last_peak + min_peak_delay && curr_time < last_peak + max_peak_delay) {
                    //triggerEvent();
                //}
                last_peak = curr_time;
            } else {
                activity.flagEnd();
                //Log.v(TAG, "Buffer index is " + bufferIndex + " Diff is z is " + z + " and prev is " + (cb[(bufferIndex-1) % cb.length]) + " and diff is "  + Math.abs(z - cb[(bufferIndex-1) % cb.length]));
            }

            //THRESH

            //for (int i = 0; i < cb.length; i++) sum += cb[i]; average = sum / cb.length;
            //for (int i = 0; i < cb.length; i++) v += Math.pow(cb[i] - average, 2); v /= cb.length;

            //activity.sayVar(v);
            //if (v > 100) triggerEvent();

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
}
