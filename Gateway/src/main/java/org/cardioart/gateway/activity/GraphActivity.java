package org.cardioart.gateway.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

import org.cardioart.gateway.api.BluetoothCommHelper;
import org.cardioart.gateway.R;

import java.util.ArrayList;

/**
 * Created by jirawat on 10/05/2014.
 */
public class GraphActivity extends ActionBarActivity implements Handler.Callback{
    private static final int HISTORY_SIZE = 2000;
    private static String device_name;
    private static String device_address;
    private Handler mainHandler;
    private Runnable mTimer1;
    private Runnable mTimer2;
    private GraphView graphView;
    private GraphViewSeries series1;
    private GraphViewSeries series2;
    private double serie1LastValue = 0d;
    private double serie2LastValue = 0d;
    private ArrayList<GraphViewData> data1 = new ArrayList<GraphViewData>();
    private ArrayList<GraphViewData> data2 = new ArrayList<GraphViewData>();
    private BluetoothCommHelper bluetoothCommHelper;

    @Override
    public boolean handleMessage(Message msg) {
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        device_name = getIntent().getStringExtra("device_name");
        device_address = getIntent().getStringExtra("device_address");
        if (device_name.isEmpty()) {
            this.finish();
        }
        setTitle(device_name);
        mainHandler = new Handler(this);
        bluetoothCommHelper = new BluetoothCommHelper(mainHandler);

        //generate data
        for (;serie2LastValue < HISTORY_SIZE; serie2LastValue++, serie1LastValue++) {
            data1.add(new GraphViewData(serie1LastValue, 0));
            data2.add(new GraphViewData(serie2LastValue, 0));
        }
        //init graphView for series1
        /*
        series1 = new GraphViewSeries(data1.toArray(new GraphViewData[data1.size()]));
        graphView = new LineGraphView(this, "Serie1");
        series1.getStyle().color = Color.GREEN;
        graphView.addSeries(series1);
        graphView.setHorizontalLabels(new String[]{});
        graphView.setVerticalLabels(new String[] {"255","127", "0"});
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(255, 0);
        graphView.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.small));
        LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
        layout.addView(graphView);

        //init graphView for series2
        series2 = new GraphViewSeries(data2.toArray(new GraphViewData[data2.size()]));
        series2.getStyle().color = Color.CYAN;
        graphView = new LineGraphView(this, "Serie2");
        graphView.addSeries(series2);
        graphView.getGraphViewStyle().setTextSize(getResources().getDimension(R.dimen.small));
        graphView.setVerticalLabels(new String[] {"255","127", "0"});
        graphView.setHorizontalLabels(new String[]{});
        graphView.setManualYAxis(true);
        graphView.setManualYAxisBounds(255, 0);
        layout = (LinearLayout) findViewById(R.id.graph2);
        layout.addView(graphView);
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        bluetoothCommHelper.startListening();
        /*
        mTimer1 = new Runnable() {
            @Override
            public void run() {
                for (int i=0; i< 100; i++) {
                    serie1LastValue += 1d;
                    data1.remove(0);
                    data1.add(new GraphViewData(serie1LastValue, getRandom(serie1LastValue)));
                }
                series1.resetData(data1.toArray(new GraphViewData[HISTORY_SIZE]));
                mHandler.postDelayed(this, 1);
            }
        };
        mHandler.postDelayed(mTimer1, 100);

        mTimer2 = new Runnable() {
            @Override
            public void run() {
                for (int i=0; i< 100; i++) {
                    serie2LastValue += 1d;
                    data2.remove(0);
                    data2.add(new GraphViewData(serie2LastValue, getSine(serie2LastValue)));
                }
                series2.resetData(data2.toArray(new GraphViewData[HISTORY_SIZE]));
                mHandler.postDelayed(this, 1);
            }
        };
        mHandler.postDelayed(mTimer2, 100);
        */
    }

    @Override
    protected void onPause() {
        //mHandler.removeCallbacks(mTimer1);
        //mHandler.removeCallbacks(mTimer2);
        bluetoothCommHelper.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        //bluetoothCommHelper.stop();
        super.onDestroy();

    }

    private double getSine(double x) {
        return Math.sin(x/180*Math.PI)*50+127 + Math.random()*100 - 50;
    }
    private double getRandom(double x) {
        return (x % 255);
    }
    private void appendGraph(Object obj) {
        for (int i=0; i < 509; i++) {
            serie1LastValue += 1d;
            data1.remove(0);
            data1.add(new GraphViewData(serie1LastValue, getSine(serie1LastValue)));
        }
        series1.resetData(data1.toArray(new GraphViewData[HISTORY_SIZE]));
    }
}
