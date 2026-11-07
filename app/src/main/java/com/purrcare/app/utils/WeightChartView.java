package com.purrcare.app.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.purrcare.app.models.WeightRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A very simple line chart drawn with Canvas (no chart library needed). Records must be sorted oldest -> newest. */
public class WeightChartView extends View {
    private List<WeightRecord> data = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    public WeightChartView(Context context) {
        this(context, null);
    }

    public WeightChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        linePaint.setColor(Color.parseColor("#8E6BD8"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3 * density);
        pointPaint.setColor(Color.parseColor("#F48FB1"));
        gridPaint.setColor(Color.parseColor("#EEE8F7"));
        gridPaint.setStrokeWidth(1 * density);
        textPaint.setColor(Color.parseColor("#7A6F8A"));
        textPaint.setTextSize(11 * density);
    }

    public void setData(List<WeightRecord> records) {
        data = new ArrayList<>(records);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (data.isEmpty()) {
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Add weight records to see the chart", w / 2, h / 2, textPaint);
            return;
        }
        float left = 44 * density, right = 16 * density, top = 16 * density, bottom = 30 * density;
        float chartW = w - left - right;
        float chartH = h - top - bottom;

        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (WeightRecord r : data) {
            min = Math.min(min, r.getWeightKg());
            max = Math.max(max, r.getWeightKg());
        }
        if (max - min < 0.2) {
            min -= 0.5;
            max += 0.5;
        } else {
            double pad = (max - min) * 0.15;
            min -= pad;
            max += pad;
        }

        // horizontal grid lines with kg labels
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i < 4; i++) {
            float y = top + i * chartH / 3f;
            canvas.drawLine(left, y, w - right, y, gridPaint);
            double value = max - i * (max - min) / 3.0;
            canvas.drawText(String.format(Locale.US, "%.1f", value), left - 6 * density, y + 4 * density, textPaint);
        }

        // line + points
        int n = data.size();
        Path path = new Path();
        for (int i = 0; i < n; i++) {
            float x = n == 1 ? left + chartW / 2 : left + i * chartW / (n - 1);
            float y = top + (float) ((max - data.get(i).getWeightKg()) / (max - min)) * chartH;
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);
        for (int i = 0; i < n; i++) {
            float x = n == 1 ? left + chartW / 2 : left + i * chartW / (n - 1);
            float y = top + (float) ((max - data.get(i).getWeightKg()) / (max - min)) * chartH;
            canvas.drawCircle(x, y, 4.5f * density, pointPaint);
        }

        // first and last dates under the chart
        textPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(DateTimeUtils.shortDate(data.get(0).getDate()), left, h - 8 * density, textPaint);
        if (n > 1) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(DateTimeUtils.shortDate(data.get(n - 1).getDate()), w - right, h - 8 * density, textPaint);
        }
    }
}
