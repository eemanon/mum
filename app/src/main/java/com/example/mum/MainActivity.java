package com.example.mum;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    private CustomView canvas;
    private CustomView2 positionCanvas;
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
    private TextView status;
    private EditText interval;
    private Button btn_calibrate;
    private SensorManager sensorManager;
    private SensorEventListener magnetSensorListener;
    private Sensor mag;
    private boolean record;
    Queue<Float> samplesX;
    Queue<Float> samplesY;
    Queue<Float> samplesZ;
    Queue<Float> historyTotalValue;
    float previousTotalValue;
    Queue<double[]> vectorHistory;
    double[] equation;
    int bufferLength;
    int historyLength;
    private int[] statusMoving;
    float[] calibrationValues = {0.0f, 0.0f, 0.0f};
    private TextView vectorLength;
    private TextView zValueFieldTrans;
    private TextView xValueFieldTrans;
    private TextView yValueFieldTrans;
    private float currentTotalValue = 0.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //load mapping
        ArrayList<Float[]> mapping = importMapping();

        //load z-density-distance list
        ArrayList<Float[]> densdist = importDistanceDensity();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        //switch to know when to record data and send it and when only to display new values
        record = false;
        samplesX = new LinkedList<Float>();
        samplesY = new LinkedList<Float>();
        samplesZ = new LinkedList<Float>();

        vectorHistory = new LinkedList<double[]>();
        bufferLength = 10;
        historyLength = 50;
        calibrationValues = new float[]{0.0f, 0.0f, 0.0f};
        //init queues
        for (int i = 0; i < bufferLength; i++) {
            samplesX.add(0.0f);
            samplesY.add(0.0f);
            samplesZ.add(0.0f);
        }

        magnetSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    // get values for each axes X,Y,Z
                    zValueField.setText(String.format("%.2f", event.values[2]));
                    xValueField.setText(String.format("%.2f", event.values[0]));
                    yValueField.setText(String.format("%.2f", event.values[1]));

                    //calc running sum
                    samplesX.remove();
                    samplesY.remove();
                    samplesZ.remove();
                    samplesX.add(event.values[0]);
                    samplesY.add(event.values[1]);
                    samplesZ.add(event.values[2]);

                    previousTotalValue = currentTotalValue;
                    currentTotalValue = (Math.abs(avg(samplesX) - calibrationValues[0]) + Math.abs(avg(samplesY) - calibrationValues[1]) + Math.abs(avg(samplesZ) - calibrationValues[2]));
                    xValueAvgField.setText(String.format("%.2f", (avg(samplesX))));
                    yValueAvgField.setText(String.format("%.2f", (avg(samplesY))));
                    zValueAvgField.setText(String.format("%.2f", (avg(samplesZ))));
                    xValueAvgFieldCl.setText(String.format("%.2f", (avg(samplesX) - calibrationValues[0])));
                    yValueAvgFieldCl.setText(String.format("%.2f", (avg(samplesY) - calibrationValues[1])));
                    zValueAvgFieldCl.setText(String.format("%.2f", (avg(samplesZ) - calibrationValues[2])));
                    absvalueField.setText(String.format("%.2f", ((Math.abs(avg(samplesX)) + Math.abs(avg(samplesY)) + Math.abs(avg(samplesZ))))));
                    absvalueFieldCl.setText(String.format("%.2f", currentTotalValue));
                    //rotate image
                    //arrow.setRotation((float) (180*Math.atan(avg(samplesY)-calibrationValues[1])/avg(samplesX)-calibrationValues[0]/Math.PI));
                    //vectorLength.setText(180*Math.atan(avg(samplesY)-calibrationValues[1])/avg(samplesX)-calibrationValues[0]/Math.PI+"");
                    //a simple way to check if we're moving or not...use accelerometer when in low confidence zones


                    //update "real" position
                    float length = calcDistance(avg(samplesZ), currentTotalValue, 3400, densdist);
                    float[] pos = calcPos(mapping, avg(samplesX), avg(samplesY));

                    realx.setText(pos[0] + "");
                    realy.setText(pos[1] + "");
                    vectorLength.setText(length + " mm");
                    double[] normalizedVector = normalize2d(new double[]{avg(samplesX) - calibrationValues[0], -avg(samplesY) + calibrationValues[1]});
                    double[] scaledVector = scaleVector2d(normalizedVector, length);
                    //Moving or not? problematic for far away values
                    if (Math.abs(previousTotalValue - currentTotalValue) > 2.0f) {
                        status.setText("moving");

                        vectorHistory.add(scaledVector);
                        if (vectorHistory.size() > historyLength)
                            vectorHistory.remove();
                        if (vectorHistory.size() >= bufferLength){
                            equation = leastSquare(vectorHistory);
                            //print coords to screen and draw equation line
                            positionCanvas.setPoints(vectorHistory);
                            positionCanvas.setEquation(equation);
                            positionCanvas.invalidate();
                        }

                        Log.i("vectors", scaledVector[0] + ", " + scaledVector[1]);
                    } else
                        status.setText("not moving");

                    canvas.setCoords(scaledVector);
                    canvas.invalidate();
                }
                if (record) {
                    Log.i("data", String.format("%.2f", (avg(samplesX) - calibrationValues[0])) + "|" + String.format("%.2f", (avg(samplesY) - calibrationValues[1])) + "|" + String.format("%.2f", (avg(samplesZ) - calibrationValues[2])));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);

        //init all the gui refs.

        realx = (TextView) findViewById(R.id.txtRealX);
        realy = (TextView) findViewById(R.id.txtRealY);

        zValueField = (TextView) findViewById(R.id.txt_z);
        xValueField = (TextView) findViewById(R.id.txt_x);
        yValueField = (TextView) findViewById(R.id.txt_y);

        zValueAvgField = (TextView) findViewById(R.id.txt_z_avg);
        xValueAvgField = (TextView) findViewById(R.id.txt_x_avg);
        yValueAvgField = (TextView) findViewById(R.id.txt_y_avg);

        zValueAvgFieldCl = (TextView) findViewById(R.id.txt_z_avg2);
        xValueAvgFieldCl = (TextView) findViewById(R.id.txt_x_avg2);
        yValueAvgFieldCl = (TextView) findViewById(R.id.txt_y_avg2);

        zValueFieldTrans = (TextView) findViewById(R.id.txt_z_trans);
        xValueFieldTrans = (TextView) findViewById(R.id.txt_x_trans);
        yValueFieldTrans = (TextView) findViewById(R.id.txt_y_trans);

        interval = (EditText) findViewById(R.id.edt_interval);
        absvalueField = (TextView) findViewById(R.id.txt_absvalue);
        absvalueFieldCl = (TextView) findViewById(R.id.txt_absvalue2);
        vectorLength = findViewById(R.id.txt_vectorLength);

        status = findViewById(R.id.txt_status);

        canvas = findViewById(R.id.canvass);
        positionCanvas= findViewById(R.id.canvass2);

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
        btn_log = findViewById(R.id.btn_log);
        btn_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record = !record;
                if (record)
                    btn_log.setText("logs");
                else
                    btn_log.setText("log");
            }
        });

        //get mapping

    }

    private double[] leastSquare(Queue<double[]> vectorHistory) {
        //returns function that approximates vectorHistory Points
        double n = vectorHistory.size();
        double sumx = sum(vectorHistory, 0);
        double sumy = sum(vectorHistory, 1);
        //m = (N Σ(xy) − Σx Σy) / (N Σ(x^2) − (Σx)^2)
        double m = (n * sumxy(vectorHistory) - sumx * sumy) / (n * sumsquare(vectorHistory, 0) - Math.pow(sumx, 2));
        //b = Σy − m Σx/ N
        double b = (sumy - m * sumx) / n;
        return new double[]{b, m};
    }

    private double sumsquare(Queue<double[]> vectorHistory, int i) {
        double result = 0.0f;
        for (double[] coords : vectorHistory) {
            result += coords[i] * coords[i];
        }
        return result;
    }

    private double sum(Queue<double[]> vectorHistory, int i) {
        double result = 0.0f;
        for (double[] coords : vectorHistory) {
            result += coords[i];
        }
        return result;
    }

    private double sumxy(Queue<double[]> vectorHistory) {
        double result = 0.0f;
        for (double[] coords :
                vectorHistory) {
            result += (coords[0] * coords[1]);
        }
        return result;
    }

    private double[] scaleVector2d(double[] vector, double length) {
        double k = (length * length) / (vector[0] * vector[0] + vector[1] * vector[1]);
        return new double[]{k * vector[0], k * vector[1]};
    }

    private double[] normalize2d(double[] vector) {
        return new double[]{vector[0] / Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1]), vector[1] / Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1])};
    }
    //calculates distance to center of closest magnet, threshold in uT tells when to use z or b as measurement for distance
    private float calcDistance(float z, float b, float threshold, ArrayList<Float[]> densdistlist) {
        //uses z value of |B| > 3000uT
        double previous_distance = 10000;
        float result = 0;
        if(b<threshold){
            Log.i("distance", "far enough, using B");
            for (Float[] f : densdistlist) {
                double distance = Math.abs(f[1] - b);
                //Log.i("Debug", "distance: "+distance);
                if (distance < previous_distance) {
                    previous_distance = distance;
                    result = f[2];
                }
            }
        } else {
            Log.i("distance", "too close, using z");
            for (Float[] f : densdistlist) {
                double distance = Math.abs(f[0] - Math.abs(z));
                //Log.i("Debug", "distance: "+distance);
                if (distance < previous_distance) {
                    previous_distance = distance;
                    result = f[2];
                }
            }
        }
        return result;
    }

    //returns z-density-distance list from csv in form of x,y,z,distance, density |B|
    ArrayList<Float[]> importDistanceDensity() {
        ArrayList<Float[]> result = new ArrayList<>();
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.densitydistance01mm)));
            String[] nextLine;
            boolean firstLine = true;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String z = nextLine[2].toString();
                String distance = nextLine[3].toString();
                String density = nextLine[4].toString();
                result.add(new Float[]{Float.parseFloat(z), Float.parseFloat(density), Float.parseFloat(distance)});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    ArrayList<Float[]> importMapping() {
        //loads a mapping from real coordinates x [0] y [1] to magnetic field strength values x [2] y [3]
        ArrayList<Float[]> result = new ArrayList<>();
        try {
            Log.i("Info", "test");
            CSVReader reader = new CSVReader(new InputStreamReader(getResources().openRawResource(R.raw.output)));
            String[] nextLine;
            boolean firstLine = true;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String xReal = nextLine[0].toString();
                String yReal = nextLine[1].toString();
                String xValue = nextLine[3].toString();
                String yValue = nextLine[4].toString();
                //Log.i("info", "values: "+xValue + ", "+yValue+", "+xReal+", "+yReal);
                result.add(new Float[]{Float.parseFloat(xReal), Float.parseFloat(yReal), Float.parseFloat(xValue), Float.parseFloat(yValue)});
            }
        } catch (IOException e) {

        }
        Log.i("Info", "imported " + result.size() + " rows");
        return result;
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
}