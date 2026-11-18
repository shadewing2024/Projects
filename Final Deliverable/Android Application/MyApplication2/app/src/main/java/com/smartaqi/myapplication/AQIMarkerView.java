package com.smartaqi.myapplication;

import android.content.Context;
import android.widget.TextView;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class AQIMarkerView extends MarkerView {

    private final TextView tvContent;

    public AQIMarkerView(Context context) {
        super(context, R.layout.view_marker); // Ensure you have this layout file
        tvContent = findViewById(R.id.tvContent);
    }

    // This callback runs every time the marker is redrawn
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        tvContent.setText("AQI: " + (int) e.getY());
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Centers the marker above the point
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}