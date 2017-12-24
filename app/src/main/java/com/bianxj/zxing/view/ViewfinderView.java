package com.bianxj.zxing.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.view.View;

import com.bianxj.zxing.R;

/**
 * 作者:边贤君
 * 描述:
 * 创建日期:2017/12/24 09:46
 */

public class ViewfinderView extends View {

    private Context context;

    private final int borderColor;
    private final int borderSize;
    private final int borderLength;
    private final Rect scanInActivity;
    private final Rect scanRect;
    private final Rect borderRect;
    private Paint paint;

    public ViewfinderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        TypedArray array = context.getTheme().obtainStyledAttributes(attrs,R.styleable.ViewfinderView,defStyleAttr,0);

        borderColor = array.getColor(R.styleable.ViewfinderView_borderColor, Color.WHITE);
        borderSize = array.getDimensionPixelSize(R.styleable.ViewfinderView_borderSize,0);
        borderLength = array.getDimensionPixelOffset(R.styleable.ViewfinderView_borderLength,0);

        int centX = array.getDimensionPixelSize(R.styleable.ViewfinderView_centerX,-1);
        int centY = array.getDimensionPixelSize(R.styleable.ViewfinderView_centerY,-1);
        int frameLenght = array.getDimensionPixelSize(R.styleable.ViewfinderView_frameLength,-1);
        if ( (centX == -1 && centY == -1) || frameLenght == -1 ){
            scanRect = null;
            borderRect = null;
            scanInActivity = null;
        } else {
            scanRect = new Rect();
            scanRect.top = centY - frameLenght/2;
            scanRect.bottom = scanRect.top + frameLenght;
            scanRect.left = centX - frameLenght/2;
            scanRect.right = scanRect.left + frameLenght;

            borderRect = new Rect();
            borderRect.top = scanRect.top - borderSize;
            borderRect.left = scanRect.left - borderSize;
            borderRect.right = scanRect.right + borderSize;
            borderRect.bottom = scanRect.bottom + borderSize;

            scanInActivity = new Rect();
            scanInActivity.top = scanRect.top + getTop();
            scanInActivity.bottom = scanRect.bottom + getTop();
            scanInActivity.left = scanRect.left + getLeft();
            scanInActivity.right = scanRect.right + getLeft();
        }
        paint = new Paint();
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCover(canvas);
        drawBorder(canvas);
    }

    private void drawCover(Canvas canvas){
        if ( scanRect == null || borderRect == null ){
            return;
        }
        paint.setColor(ActivityCompat.getColor(context,R.color.translucence));
        paint.setAntiAlias(true);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        canvas.drawRect(0,0,width,scanRect.top,paint);
        canvas.drawRect(0,scanRect.top,scanRect.left,scanRect.bottom,paint);
        canvas.drawRect(0,scanRect.bottom,width,height,paint);
        canvas.drawRect(scanRect.right,scanRect.top,width,scanRect.bottom,paint);
    }

    private void drawBorder(Canvas canvas){
        if ( scanRect == null || borderRect == null ){
            return;
        }
        paint.setColor(borderColor);

        canvas.drawRect(borderRect.left,borderRect.top,scanRect.top,borderRect.top+borderLength,paint);
        canvas.drawRect(borderRect.left,borderRect.top,borderRect.left+borderLength,scanRect.left,paint);

        canvas.drawRect(scanRect.right,borderRect.top,borderRect.right,borderRect.top+borderLength,paint);
        canvas.drawRect(borderRect.right-borderLength,borderRect.top,borderRect.right,scanRect.top,paint);

        canvas.drawRect(borderRect.left,borderRect.bottom-borderLength,scanRect.left,borderRect.bottom,paint);
        canvas.drawRect(borderRect.left,scanRect.bottom,borderRect.left+borderLength,borderRect.bottom,paint);

        canvas.drawRect(scanRect.right,borderRect.bottom-borderLength,borderRect.right,borderRect.bottom,paint);
        canvas.drawRect(borderRect.right-borderLength,scanRect.bottom,borderRect.right,borderRect.bottom,paint);
    }

    public Rect getScanRect(){
        return scanInActivity;
    }

}
