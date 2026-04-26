package com.haoyinrui.campusattendance.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.haoyinrui.campusattendance.R;

/**
 * 轻量月度统计图。
 * 使用横向条形分布替代竖向柱状图，避免小样本数据时画面失衡。
 */
public class MonthlyStatsChartView extends View {
    private static final String[] LABELS = {"正常", "迟到", "早退", "缺卡", "请假"};

    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int[] values = new int[5];
    private final int[] colors = new int[5];

    public MonthlyStatsChartView(Context context) {
        this(context, null);
    }

    public MonthlyStatsChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MonthlyStatsChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        colors[0] = ContextCompat.getColor(context, R.color.primary);
        colors[1] = ContextCompat.getColor(context, R.color.accent);
        colors[2] = ContextCompat.getColor(context, R.color.warning);
        colors[3] = ContextCompat.getColor(context, R.color.danger);
        colors[4] = ContextCompat.getColor(context, R.color.text_secondary);

        labelPaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        labelPaint.setTextSize(sp(14));
        labelPaint.setTextAlign(Paint.Align.LEFT);

        valuePaint.setColor(ContextCompat.getColor(context, R.color.text_secondary));
        valuePaint.setTextSize(sp(14));
        valuePaint.setFakeBoldText(true);
        valuePaint.setTextAlign(Paint.Align.RIGHT);

        trackPaint.setColor(ContextCompat.getColor(context, R.color.border));
        trackPaint.setAlpha(70);
    }

    public void setData(int normal, int late, int earlyLeave, int missing, int leave) {
        values[0] = Math.max(0, normal);
        values[1] = Math.max(0, late);
        values[2] = Math.max(0, earlyLeave);
        values[3] = Math.max(0, missing);
        values[4] = Math.max(0, leave);
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = (int) dp(176);
        int height = resolveSize(desiredHeight, heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        float paddingHorizontal = dp(12);
        float topPadding = dp(2);
        float rowGap = dp(7);
        float trackHeight = dp(9);
        float rowBlockHeight = dp(26);
        float trackWidth = width - paddingHorizontal * 2f;

        int maxValue = 0;
        for (int value : values) {
            maxValue = Math.max(maxValue, value);
        }
        if (maxValue == 0) {
            maxValue = 1;
        }

        Paint.FontMetrics labelMetrics = labelPaint.getFontMetrics();
        Paint.FontMetrics valueMetrics = valuePaint.getFontMetrics();
        float labelOffset = -labelMetrics.ascent;
        float valueOffset = -valueMetrics.ascent;

        for (int i = 0; i < LABELS.length; i++) {
            float rowTop = topPadding + i * (rowBlockHeight + rowGap);
            float textBaseline = rowTop + Math.max(labelOffset, valueOffset);
            float trackTop = rowTop + dp(17);

            canvas.drawText(LABELS[i], paddingHorizontal, textBaseline, labelPaint);
            canvas.drawText(String.valueOf(values[i]), width - paddingHorizontal, textBaseline, valuePaint);

            rect.set(paddingHorizontal, trackTop, width - paddingHorizontal, trackTop + trackHeight);
            canvas.drawRoundRect(rect, trackHeight / 2f, trackHeight / 2f, trackPaint);

            if (values[i] > 0) {
                float fillWidth = trackWidth * values[i] / (float) maxValue;
                fillWidth = Math.max(fillWidth, dp(24));
                fillPaint.setColor(colors[i]);
                fillPaint.setAlpha(210);
                rect.set(paddingHorizontal, trackTop, paddingHorizontal + fillWidth, trackTop + trackHeight);
                canvas.drawRoundRect(rect, trackHeight / 2f, trackHeight / 2f, fillPaint);
            }
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
