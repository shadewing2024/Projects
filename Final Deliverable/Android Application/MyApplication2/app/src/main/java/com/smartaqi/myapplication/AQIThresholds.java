package com.smartaqi.myapplication;

import android.graphics.Color;

public class AQIThresholds {

    // AQI Levels
    public static final int GOOD = 50;
    public static final int MODERATE = 100;
    public static final int SENSITIVE = 150;
    public static final int UNHEALTHY = 200;
    public static final int VERY_UNHEALTHY = 300;

    // Get AQI category level
    public static int getAQILevel(int aqi) {

        if (aqi <= GOOD) return 0;
        else if (aqi <= MODERATE) return 1;
        else if (aqi <= SENSITIVE) return 2;
        else if (aqi <= UNHEALTHY) return 3;
        else if (aqi <= VERY_UNHEALTHY) return 4;
        else return 5;
    }

    // Get label for UI
    public static String getAQILabel(int aqi) {

        switch (getAQILevel(aqi)) {
            case 0:
                return "Good";
            case 1:
                return "Moderate";
            case 2:
                return "Sensitive";
            case 3:
                return "Unhealthy";
            case 4:
                return "Very Unhealthy";
            default:
                return "Hazardous";
        }
    }

    // Optional: color logic for UI
    public static int getAQIColor(int aqi) {

        switch (getAQILevel(aqi)) {
            case 0:
                return Color.argb(255, 103, 170, 1); // green
            case 1:
                return Color.argb(255, 255, 230, 17); // yellow
            case 2:
                return Color.argb(255, 255, 152, 0); // orange
            case 3:
                return Color.argb(255, 251, 53, 39); // red
            case 4:
                return Color.argb(255, 156, 39, 176); // purple
            default:
                return Color.argb(255, 111, 20, 20); // dark red
        }
    }
}