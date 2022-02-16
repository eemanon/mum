package com.example.mum;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Queue;

public class CustomView extends View {
    public synchronized void setCoords(double[] coords) {
        this.coords = coords;
    }

    private double[] coords;

    public CustomView(Context context, AttributeSet attr) {
        super(context, attr);
        coords = new double[]{0.0f,0.0f};
    }
    @Override
    public void onDraw(Canvas canvas){
        canvas.drawColor(Color.rgb(32,32,32));

        Paint redPaint = new Paint();
        redPaint.setColor(Color.rgb(255, 0, 0));

        Paint greenPaint = new Paint();
        greenPaint.setColor(Color.rgb(0, 255, 0));

        Paint bluePaint = new Paint();
        bluePaint.setColor(Color.rgb(0, 0, 255));
        canvas.drawLine(canvas.getWidth()/2+0.0f,0.0f, canvas.getWidth()/2+0.0f, canvas.getHeight()+0.0f, redPaint);
        canvas.drawLine(0.0f,canvas.getHeight()/2+0.0f, canvas.getWidth()+0.0f,canvas.getHeight()/2+0.0f, redPaint);
        canvas.drawLine(canvas.getWidth()/2+0.0f, canvas.getHeight()/2+0.0f, (float) (canvas.getWidth()/2+coords[0]*4), (float) (canvas.getHeight()/2+coords[1]*4), greenPaint);
    }
}