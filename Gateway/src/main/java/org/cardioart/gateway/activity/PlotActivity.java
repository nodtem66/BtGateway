package org.cardioart.gateway.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.androidplot.util.PlotStatistics;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import org.cardioart.gateway.R;

/**
 * Created by jirawat on 11/05/2014.
 */
public class PlotActivity extends ActionBarActivity {
    private static final int HISTORY_SIZE = 2000;
    private XYPlot plot1;
    private XYPlot plot2;
    private CheckBox hwAcceleratedCb;
    private CheckBox showFpsCb;
    private SimpleXYSeries series1;
    private SimpleXYSeries series2;
    private int serie1LastXValue = 0;
    private int serie2LastXValue = 0;
    private Runnable mTimer1;
    private Runnable mTimer2;
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);
        String device_name = getIntent().getStringExtra("device_name");
        setTitle(device_name);

        series1 = new SimpleXYSeries("Series1");
        series1.useImplicitXVals();
        series2 = new SimpleXYSeries("Series2");
        series2.useImplicitXVals();

        plot1 = (XYPlot) findViewById(R.id.aprPlot1);
        plot1.setRangeBoundaries(0,255, BoundaryMode.FIXED);
        plot1.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        plot1.addSeries(series1, new LineAndPointFormatter(Color.GREEN, null, null, null));
        plot1.setDomainStepValue(1);
        plot1.setTicksPerRangeLabel(1);
        plot1.setDomainLabel("Sample");
        plot1.getDomainLabelWidget().pack();
        plot1.setRangeLabel("unit");
        plot1.getRangeLabelWidget().pack();

        plot2 = (XYPlot) findViewById(R.id.aprPlot2);
        plot2.setRangeBoundaries(0,255, BoundaryMode.FIXED);
        plot2.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        plot2.addSeries(series2, new LineAndPointFormatter(Color.RED, null, null, null));
        plot2.setDomainStepValue(1);
        plot2.setTicksPerRangeLabel(1);
        plot2.setDomainLabel("Sample");
        plot2.getDomainLabelWidget().pack();
        plot2.setRangeLabel("unit");
        plot2.getRangeLabelWidget().pack();

        final PlotStatistics plot1Stat = new PlotStatistics(1000, false);
        final PlotStatistics plot2Stat = new PlotStatistics(1000, false);
        plot1.addListener(plot1Stat);
        plot2.addListener(plot2Stat);

        showFpsCb = (CheckBox) findViewById(R.id.showFpsCb);
        showFpsCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                plot1Stat.setAnnotatePlotEnabled(b);
                plot2Stat.setAnnotatePlotEnabled(b);
            }
        });
        for (int i=0; i < HISTORY_SIZE; i++) {
            series1.addLast(null, 0);
            series2.addLast(null, 0);
        }
        plot1.redraw();
        plot2.redraw();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTimer1 = new Runnable() {
            @Override
            public void run() {
                for (int i=0; i < 100; i++) {
                    series1.removeFirst();
                    series1.addLast(null, getRandom(serie1LastXValue++));
                }
                plot1.redraw();
                mHandler.postDelayed(this, 1);
            }
        };
        mHandler.postDelayed(mTimer1, 100);

        mTimer2 = new Runnable() {
            @Override
            public void run() {
                for (int i=0; i < 100; i++) {
                    series2.removeFirst();
                    series2.addLast(null, getSine(serie2LastXValue++));
                }
                plot2.redraw();
                mHandler.postDelayed(this, 1);
            }
        };
        mHandler.postDelayed(mTimer2, 100);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(mTimer1);
        mHandler.removeCallbacks(mTimer2);
        super.onPause();
    }

    private double getSine(double x) {
        return Math.sin(x/180*Math.PI)*50+127 + Math.random()*100 - 50;
    }
    private double getRandom(double x) {
        return (x % 255);
    }
}
