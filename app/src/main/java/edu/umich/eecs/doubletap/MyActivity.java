package edu.umich.eecs.doubletap;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.support.wearable.activity.WearableActivity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.util.Log;


public class MyActivity extends WearableActivity {
    final static String TAG = "MyActivity";
    private DoubleTapper doubleTapper;
    private TextView textView;
    private FrameLayout frame;
    private FrameLayout xPane, yPane, zPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Log.v(TAG, "Prewire");
        wireUI();

        Log.v(TAG, "DTapper");
        doubleTapper = new DoubleTapper(this);
        doubleTapper.startMonitor();
    }

    private void wireUI () {
        ((Button) findViewById(R.id.recordButton)).setOnClickListener(recordListener);
        textView = (TextView) findViewById(R.id.textView);

        xPane = (FrameLayout) findViewById(R.id.xPane);
        yPane = (FrameLayout) findViewById(R.id.yPane);
        zPane = (FrameLayout) findViewById(R.id.zPane);
    }

    public int f (float i) { return (int)i*100; }
    public void sayAccel (final float x, final float y, final float z) {
        updateText("(" + f(x) + ", " + f(y) + ", " + f(z) + ")");
    }

    public void sayVar (final float variance) {
        updateText("Variance: " + variance);
    }


    public void updateText (final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                textView.setText(msg);
            }
        });
    }

    public void flashColor (String color) {
        final FrameLayout pane;
        if (color.equals("x")) pane = xPane;
        else if (color.equals("y")) pane = yPane;
        else pane = zPane;

        Log.e(TAG, "Flashing color " + color);


        /*(new Thread(new Runnable() {
            @Override
            public void run () {
                for (int i = 0; i < 10; i++) {
                    try { Thread.currentThread().sleep(100); } catch (Exception e) {}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run () { pane.setAlpha(1); }
                    });
                    try { Thread.currentThread().sleep(100); } catch (Exception e) {}
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pane.setAlpha(0);
                        }
                    });
                }
            }
        })).start();*/
    }


    @Override
    public void onDestroy () {
        super.onDestroy();
        doubleTapper.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    View.OnClickListener recordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.v(TAG, "Starting");
            updateText("Starting experiment");
            doubleTapper.debugging = true;
            doubleTapper.saved = false;
            doubleTapper.debugCounter = 0;
            doubleTapper.startDebugTime = System.currentTimeMillis();
            updateText("Running experiment");
        }
    };
}
