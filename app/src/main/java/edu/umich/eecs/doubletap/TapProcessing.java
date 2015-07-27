package edu.umich.eecs.doubletap;

import android.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import android.util.Log;

/**
 * Created by Arun on 7/26/2015.
 */
public class TapProcessing {
    // Buffers + pointers
    final static int MAX = 1500;
    int bufferStartTime = 0;
    long lastTime = 0;
    LinkedList<Double> values = new LinkedList<Double>();
    LinkedList<Pair<Boolean, Integer>> peaks = new LinkedList<Pair<Boolean, Integer>>(); // Again, stores the time intervals
    ArrayList<Integer> taps = new ArrayList<Integer>(); // Again, stores the time intervals

    // Peakdetection variables
    double mn = 1000, mx = -1000;
    int mnpos = 0, mxpos = 0;
    boolean lookformax = true;


    // Parameters
    private double delta = 0.35;
    private double basinSize = 100;
    private double basinVari = 0.7;

    // Debug
    final static String TAG = "TapProcessing";


    /**
     * Adds new data, continues from last point where it left off to calculate
     * peaks and basins. As basins come it, it calculates variance and determines
     * if basin is a tap event. It ultimately just updates the tap event.
     *
     * @param time
     * @param value
     */
    public void addData (long time, double value) {
        // XXX: Make sure this works even when we re-run this after adding a
        // each row to the array. Basically, does the peak detection algorithm
        // work well for the corner cases? Would it treat the last point as a
        // min even though it might not be?

        // How can we be sure teh interpolation works? Probably just running everything...

        value = Math.abs(value);
        if (lastTime == time) values.set(values.size()-1, value);
        else if (values.size() == 0) values.add(value);
        else {
            double lastValue = values.getLast();
            double jump = value - lastValue;
            long steps = time - lastTime;
            for (int i = 0; i < steps; i++)
                values.add(lastValue + jump * ((double)(i+1)/steps));
        }
        lastTime = time;


        int peakSearchFrom = Math.min(mnpos, mxpos);
        for (int i = peakSearchFrom; i < MAX; i++) {
            if (values.size() <= i) break;
            double v = values.get(i);
            int loc = i+bufferStartTime;
            if (v > mx) { mx = v; mxpos = i+bufferStartTime; }
            if (v < mn) { mn = v; mnpos = i+bufferStartTime; }

            if (lookformax) {
                if (v < mx-delta) {
                    if (!peaks.isEmpty()) peaks.add(Pair.create(true, loc));
                    //Log.v(TAG, "Added up peak");
                    mn = v; mnpos = loc;
                    lookformax = false;
                }
            } else {
                if (v > mn + delta) {
                    peaks.add(Pair.create(false, loc));
                    mx = v; mxpos = loc;
                    lookformax = true;

                    //Log.v(TAG, "Added down peak");
                    // Pop out last few peaks to create basin
                    if (peaks.size() >= 3) {

                        Pair<Boolean, Integer> end = peaks.pop();
                        Pair<Boolean, Integer> middle = peaks.pop();
                        Pair<Boolean, Integer> start = peaks.pop();

                        int startIdx = start.second;
                        int endIdx = end.second;
                        Log.v(TAG, "Found basin - " + (endIdx - startIdx));
                        if (endIdx - startIdx < basinSize) {
                            ArrayList<Double> subset = new ArrayList<Double>();
                            for (int jj = startIdx; jj < endIdx; jj++) subset.add(values.get(jj - bufferStartTime));
                            double sum = 0; for (Double d : subset) sum += d;
                            double average = sum / subset.size();
                            double var = 0; for (Double d : subset) var += Math.pow(d - average, 2); v /= subset.size();
                            if (var > basinVari) taps.add(middle.second);
                        }
                    }
                }
            }
        }

        while (values.size() > MAX) {
            bufferStartTime++;
            if (--mxpos < 0) mxpos = 0;
            if (--mnpos < 0) mnpos = 0;
            values.pop();
        }
    };


    /**
     * Uses this.taps() to find whether two taps were made with appropriate
     * distance between them.
     *
     * @return Returns true if a double tap event was detected.
     */
    boolean detectDoubleTap () {
        if (taps.size() <= 1) return false;
        for (int i = 1; i < taps.size(); i++) {
            double diff = taps.get(i) - taps.get(i-1);
            if (diff > 250 && diff < 550) {
                taps.remove(i-1);taps.remove(i-1);
                return true;
            }
        }

        while (taps.get(0) < lastTime - 1000) taps.remove(0);
        return false;
    }


    /**
     * Parameter tweaking functions.
     */
    public void setDelta (double delta) { this.delta = delta; }
    public void setBasinSize (double basinSize) { this.basinSize = basinSize; }
    public void setBasinVariance (double basinVari) { this.basinVari = basinVari; }
}
