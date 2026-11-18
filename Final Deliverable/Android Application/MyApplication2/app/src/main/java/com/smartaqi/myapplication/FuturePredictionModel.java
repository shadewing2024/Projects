package com.smartaqi.myapplication;

public class FuturePredictionModel {

    private int hour;
    private int predictedAQI;

    public FuturePredictionModel(int hour, int predictedAQI) {
        this.hour = hour;
        this.predictedAQI = predictedAQI;
    }

    public int getHour() {
        return hour;
    }

    public int getPredictedAQI() {
        return predictedAQI;
    }
}