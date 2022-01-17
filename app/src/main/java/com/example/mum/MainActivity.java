package com.example.mum;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    private CustomView canvas;
    private TextView zValueField;
    private TextView yValueField;
    private TextView xValueField;
    private TextView zValueAvgField;
    private TextView yValueAvgField;
    private TextView xValueAvgField;
    private TextView zValueAvgFieldCl;
    private TextView yValueAvgFieldCl;
    private TextView xValueAvgFieldCl;
    private TextView absvalueField;
    private TextView absvalueFieldCl;
    private Button btn_start;
    private Button btn_log;
    private EditText x;
    private EditText y;
    private TextView realx;
    private TextView realy;
    private EditText interval;
    private EditText duration;
    private TabLayout tabLayout;
    private Context context;
    private Button btn_calibrate;
    private SensorManager sensorManager;
    private SensorEventListener magnetSensorListener;
    private Sensor mag;
    private boolean record;
    private CountDownTimer timer;
    private int realX;
    private int realY;
    public static DecimalFormat DECIMAL_FORMATTER;
    private String values;
    EditText ip;
    EditText posName;
    Queue<Float> samplesX;
    Queue<Float> samplesY;
    Queue<Float> samplesZ;
    int bufferLength;
    float[] calibrationValues = {0.0f, 0.0f, 0.0f};
    private TextView vectorLength;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //switch to know when to record data and send it and when only to display new values
        record = false;
        samplesX = new LinkedList<Float>();
        samplesY = new LinkedList<Float>();
        samplesZ = new LinkedList<Float>();
        bufferLength = 50;
        calibrationValues = new float[]{0.0f, 0.0f, 0.0f};
        //init queue
        for (int i = 0; i < bufferLength; i++) {
            samplesX.add(0.0f);
            samplesY.add(0.0f);
            samplesZ.add(0.0f);
        }

        values = "INSERT INTO magnetometer (position_name,x,y,z,realX,realY,minterval,mduration,time) VALUES ";

        magnetSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    // get values for each axes X,Y,Z
                    zValueField.setText(String.format("%.2f",event.values[2]));
                    xValueField.setText(String.format("%.2f",event.values[0]));
                    yValueField.setText(String.format("%.2f",event.values[1]));

                    //calc running sum
                    samplesX.remove();
                    samplesY.remove();
                    samplesZ.remove();
                    samplesX.add(event.values[0]);
                    samplesY.add(event.values[1]);
                    samplesZ.add(event.values[2]);

                    xValueAvgField.setText(String.format("%.2f", (avg(samplesX))));
                    yValueAvgField.setText(String.format("%.2f", (avg(samplesY))));
                    zValueAvgField.setText(String.format("%.2f", (avg(samplesZ))));
                    xValueAvgFieldCl.setText(String.format("%.2f", (avg(samplesX) - calibrationValues[0])));
                    yValueAvgFieldCl.setText(String.format("%.2f", (avg(samplesY) - calibrationValues[1])));
                    zValueAvgFieldCl.setText(String.format("%.2f", (avg(samplesZ) - calibrationValues[2])));
                    absvalueField.setText(String.format("%.2f", ((Math.abs(avg(samplesX)) + Math.abs(avg(samplesY)) + Math.abs(avg(samplesZ))))));
                    absvalueFieldCl.setText(String.format("%.2f", ((Math.abs(avg(samplesX) - calibrationValues[0]) + Math.abs(avg(samplesY) - calibrationValues[1]) + Math.abs(avg(samplesZ) - calibrationValues[2])))));
                    //rotate image
                    //arrow.setRotation((float) (180*Math.atan(avg(samplesY)-calibrationValues[1])/avg(samplesX)-calibrationValues[0]/Math.PI));
                    //vectorLength.setText(180*Math.atan(avg(samplesY)-calibrationValues[1])/avg(samplesX)-calibrationValues[0]/Math.PI+"");
                    canvas.setCoords(new float[]{avg(samplesX) - calibrationValues[0], avg(samplesY) - calibrationValues[1]});
                    canvas.invalidate();
                }
                if(record){
                    Log.i("data",String.format("%.2f", (avg(samplesX) - calibrationValues[0]))+"|"+String.format("%.2f", (avg(samplesY) - calibrationValues[1]))+"|"+String.format("%.2f", (avg(samplesZ) - calibrationValues[2])));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

        //init all the gui refs.

        zValueField = (TextView) findViewById(R.id.txt_z);
        xValueField = (TextView) findViewById(R.id.txt_x);
        yValueField = (TextView) findViewById(R.id.txt_y);

        zValueAvgField = (TextView) findViewById(R.id.txt_z_avg);
        xValueAvgField = (TextView) findViewById(R.id.txt_x_avg);
        yValueAvgField = (TextView) findViewById(R.id.txt_y_avg);
        zValueAvgFieldCl = (TextView) findViewById(R.id.txt_z_avg2);
        xValueAvgFieldCl = (TextView) findViewById(R.id.txt_x_avg2);
        yValueAvgFieldCl = (TextView) findViewById(R.id.txt_y_avg2);
        interval = (EditText) findViewById(R.id.edt_interval);
        absvalueField = (TextView) findViewById(R.id.txt_absvalue);
        absvalueFieldCl = (TextView) findViewById(R.id.txt_absvalue2);
        vectorLength = findViewById(R.id.txt_vectorLength);
        canvas = findViewById(R.id.canvass);


        context = this;
        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.getTabAt(0).select();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.i("info", "chouette " + tab.getText());
                if (tab.getText().equals("prototype")) {
                    Log.i("info", "chouette equals acc");
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        btn_calibrate = findViewById(R.id.btn_calibrate);
        btn_calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                calibrationValues[0] = avg(samplesX);
                calibrationValues[1] = avg(samplesY);
                calibrationValues[2] = avg(samplesZ);
                btn_calibrate.setText("Calibrated");
            }
        });
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sensorManager.registerListener(magnetSensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), Integer.parseInt(interval.getText().toString()));
            }
        });
        btn_log =findViewById(R.id.btn_log);
        btn_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record = !record;
                if(record)
                    btn_log.setText("logs");
                else
                    btn_log.setText("log");
            }
        });

        //get mapping


    }

    private float avg(Queue<Float> samples) {
        float sum = 0.0f;
        for (float number : samples) {
            sum += number;
        }
        return sum / samples.size();
    }

    private float[] calcPos(ArrayList<Float[]> posList, float xt, float yt) {
        //lookup: calc distance to each point in list. If distance < previous distance, overwrite previous distance with distance and move on, else move on. At the end return the real coords
        double previous_distance = 10000;
        float[] result = {0f, 0f};
        float realX = 0f;
        float realY = 0f;
        for (Float[] f : posList) {
            double distance = Math.sqrt((f[2] - xt) * (f[2] - xt) + (f[3] - yt) * (f[3] - yt));
            //Log.i("Debug", "distance: "+distance);
            if (distance < previous_distance) {
                previous_distance = distance;
                result[0] = f[0];
                result[1] = f[1];
            }
        }
        return result;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        tabLayout.getTabAt(0).select();
    }
}