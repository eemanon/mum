package com.example.mum;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.LinkedList;
import java.util.Queue;

public class CustomView2 extends View {
    private double[] equation;

    private Queue<double[]> vectorHistory;

    public synchronized void setPoints(Queue<double[]> vectorHistory) {
        this.vectorHistory = vectorHistory;
    }

    public synchronized void setEquation(double[] equation) {
        this.equation = equation;
    }


    public CustomView2(Context context, AttributeSet attr) {
        super(context, attr);
        vectorHistory = new LinkedList<double[]>();
        equation = new double[]{0.0f, 0.0f};
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawColor(Color.rgb(32, 32, 32));

        Paint redPaint = new Paint();
        redPaint.setColor(Color.rgb(255, 0, 0));

        Paint greenPaint = new Paint();
        greenPaint.setColor(Color.rgb(0, 255, 0));

        Paint bluePaint = new Paint();
        bluePaint.setColor(Color.rgb(100, 100, 255));
        canvas.drawLine(canvas.getWidth() / 2 + 0.0f, 0.0f, canvas.getWidth() / 2 + 0.0f, canvas.getHeight() + 0.0f, redPaint);
        canvas.drawLine(0.0f, canvas.getHeight() / 2 + 0.0f, canvas.getWidth() + 0.0f, canvas.getHeight() / 2 + 0.0f, redPaint);
        for (double[] coord : vectorHistory) {
            canvas.drawPoint(canvas.getWidth() / 2 + (float) coord[0], canvas.getHeight() / 2 + (float) coord[1], greenPaint);
            Log.i("drawing", "point");
        }
        canvas.drawLine(0.0f, canvas.getHeight() / 2 + (float)equation[1] * (-100) + (float)equation[0], canvas.getWidth(), canvas.getHeight() / 2 + (float)equation[1] * 100 + (float)equation[0], bluePaint);

    }
}