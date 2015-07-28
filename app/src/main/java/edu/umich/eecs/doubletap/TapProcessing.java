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
    final static int MAX = 3500;
    int bufferStartTime = 0;
    long lastTime = 0;
    LinkedList<Double> values = new LinkedList<Double>();
    ArrayList<Pair<Boolean, Integer>> peaks = new ArrayList<Pair<Boolean, Integer>>(); // Again, stores the time intervals
    ArrayList<Integer> taps = new ArrayList<Integer>(); // Again, stores the time intervals

    // Peakdetection variables
    double mn = 1000, mx = -1000;
    int mnpos = 0, mxpos = 0;
    boolean lookformax = false;


    // Parameters
    private double delta = 0.35;
    private double basinSize = 100;
    private double basinVari = 0.7;
    private int adjacentCheck = 25;

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


        value = Math.abs(value);
        long steps = 0;
        if (lastTime == time) values.set(values.size()-1, value);
        else if (values.size() == 0) values.add(value);
        else {
            double lastValue = values.getLast();
            double jump = value - lastValue;
            steps = time - lastTime;
            for (int i = 0; i < steps; i++)
                values.add(lastValue + jump*((double)(i+1)/steps));
        }
        lastTime = time;


        // XXX: This is where the bug is...
        int peakSearchFrom = Math.min(mnpos, mxpos) - bufferStartTime;
        //Log.v(TAG, "(V:" + value + " |vals|:" + values.size() + " upPeakSearch:" + lookformax + " mxpos:" + mxpos + " mnpos:" + mnpos + " bufferStart:" + bufferStartTime + " peakSearchFrom " + peakSearchFrom + " addedSteps:" + steps + ")");
        for (int i = peakSearchFrom; i < MAX+100; i++) {
            if (values.size() <= i) break;
            double v = values.get(i);
            int loc = i+bufferStartTime;
            if (v > mx) { mx = v; mxpos = loc; }
            if (v < mn) { mn = v; mnpos = loc; }

            if (lookformax) {
                if (v < mx-delta) {
                    if (!peaks.isEmpty()) peaks.add(Pair.create(true, loc));
                    //Log.v(TAG, "Added up peak - " + loc);
                    mn = v; mnpos = loc;
                    lookformax = false;
                }
            } else {
                if (v > mn + delta) {
                    peaks.add(Pair.create(false, loc));
                    mx = v; mxpos = loc;
                    lookformax = true;

                    //Log.v(TAG, "Added down peak - " + loc);
                    // Pop out last few peaks to create basin
                    if (peaks.size() >= 3) {
                        Pair<Boolean, Integer> start= peaks.remove(peaks.size() - 3);
                        Pair<Boolean, Integer> middle = peaks.remove(peaks.size() - 2);
                        Pair<Boolean, Integer> end = peaks.get(peaks.size() - 1); // Not removing this one

                        int startIdx = start.second;
                        int endIdx = end.second;

                        // We've timed out
                        if (startIdx < bufferStartTime) continue;

                        //Log.v(TAG, "Found basin - " + (endIdx - startIdx));
                        if (endIdx - startIdx < basinSize) {
                            ArrayList<Double> subset = new ArrayList<Double>();

                            // XXX: Bug in this line
                            for (int jj = startIdx; jj < endIdx; jj++) {
                                //Log.v(TAG, "JJ=" + jj + " BuferStartTime=" + bufferStartTime + " startIdx=" + startIdx + " jj-bst=" + (jj - bufferStartTime) + " |val|=" + values.size());
                                subset.add(values.get(jj - bufferStartTime));
                            }

                            double sum = 0; for (Double d : subset) sum += d;
                            double average = sum / subset.size();
                            double var = 0; for (Double d : subset) var += Math.pow(d - average, 2); var /= subset.size();

                            //Log.e(TAG, "Small enough. Variance is " + var);
                            if (var > basinVari) {
                                if (taps.isEmpty()) {
                                    Log.v(TAG, "Tap found: " + middle.second);
                                    taps.add(middle.second);
                                }
                                else if (middle.second - taps.get(taps.size()-1) > adjacentCheck) {
                                    Log.v(TAG, "Tap found: " + middle.second);
                                    taps.add(middle.second);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Cleaning values
        while (values.size() > MAX) {
            bufferStartTime++;
            //if (--mxpos < 0) mxpos = 0;
            //if (--mnpos < 0) mnpos = 0;
            if (mxpos < bufferStartTime) mxpos = bufferStartTime;
            if (mnpos < bufferStartTime) mnpos = bufferStartTime;
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
            } else {
                Log.e(TAG, "Found two taps difference is " + diff);
            }
        }

        while (!taps.isEmpty() && (taps.get(0) < lastTime - 1500)) taps.remove(0);
        return false;
    }


    /**
     * Parameter tweaking functions.
     */
    public void setDelta (double delta) { this.delta = delta; }
    public void setBasinSize (double basinSize) { this.basinSize = basinSize; }
    public void setBasinVariance (double basinVari) { this.basinVari = basinVari; }
}
