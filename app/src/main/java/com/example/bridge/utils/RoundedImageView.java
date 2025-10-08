package com.example.bridge.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.BlurMaskFilter;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import com.example.bridge.R;

public class RoundedImageView extends AppCompatImageView {
    
    private Paint glowPaint;
    private Path clipPath;
    private RectF rectF;
    private float radius;
    private int glowColor;
    private float glowRadius = 20f;
    
    public RoundedImageView(Context context) {
        super(context);
        init();
    }
    
    public RoundedImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public RoundedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        
        glowColor = ContextCompat.getColor(getContext(), R.color.primary);
        
        glowPaint = new Paint();
        glowPaint.setColor(glowColor);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(4f);
        glowPaint.setAntiAlias(true);
        glowPaint.setMaskFilter(new BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.OUTER));
        
        clipPath = new Path();
        rectF = new RectF();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        radius = Math.min(w, h) / 2f;
        rectF.set(glowRadius, glowRadius, w - glowRadius, h - glowRadius);
        
        clipPath.reset();
        clipPath.addRoundRect(rectF, radius - glowRadius, radius - glowRadius, Path.Direction.CW);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        // Draw glow effect
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius - glowRadius, glowPaint);
        
        // Clip to circle and draw image
        canvas.clipPath(clipPath);
        super.onDraw(canvas);
    }
}