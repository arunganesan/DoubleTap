package edu.umich.eecs.doubletap;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;


public class MyActivity extends Activity {
    private DoubleTapper doubleTapper;
    private TextView textView;
    private FrameLayout frame;
    private FrameLayout xPane, yPane, zPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        wireUI();

        doubleTapper = new DoubleTapper(this);
        doubleTapper.startMonitor();
    }

    private void wireUI () {
        textView = (TextView) findViewById(R.id.textView);
        frame = (FrameLayout) findViewById(R.id.parentView);
        ((Button) findViewById(R.id.recordButton)).setOnClickListener(recordListener);

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
        FrameLayout pane;
        if (color.equals("x")) pane = xPane;
        else if (color.equals("y")) pane = yPane;
        else if (color.equals("z")) pane = zPane;

        (new Thread(new Runnable() {
            @Override
            public void run () {
                for (int i = 0; i < 10; i++) {
                    try { Thread.currentThread().sleep(100); } catch (Exception e) {}
                    panel.setAlpha(1);
                    try { Thread.currentThread().sleep(100); } catch (Exception e) {}
                    panel.setAlpha(0);
                }
            }
        })).start();
    }

    public void flagStart () {
        runOnUiThread(new Runnable() {
            public void run () {
                frame.setBackgroundColor(0xFFFF0000);
            }
        });
    }

    public void flagEnd () {
        runOnUiThread(new Runnable() {
            public void run () {
                frame.setBackgroundColor(0xFFFFFFFF);
            }
        });
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
            doubleTapper.debugging = true;
            doubleTapper.saved = false;
            doubleTapper.debugData.clear();
            updateText("Running experiment");
        }
    };
}
