package com.smartaqi.myapplication;

import java.util.ArrayList;
import java.util.List;

public class FutureAQIPredictor {

    public List<FuturePredictionModel> predictFutureAQI(
            int currentAQI,
            float temp,
            float humidity,
            float wind
    ) {
        List<FuturePredictionModel> list = new ArrayList<>();
        float movingAQI = currentAQI;

        for (int i = 1; i <= 10; i++) {

            // 1. Weather Impact (Normalized)
            // High wind significantly drops AQI, high temp raises it slightly
            float weatherImpact = (temp * 0.02f) + (humidity * 0.01f) - (wind * 0.15f);

            // 2. Mean Reversion (The "Anchor")
            // If the AQI gets too far from the starting point, this pulls it back.
            // This prevents the graph from flying off the top of the screen.
            float pullToCenter = (currentAQI - movingAQI) * 0.1f;

            // 3. Random Noise
            float noise = (float) (Math.random() * 8) - 4; // -4 to +4

            // Update the value
            movingAQI += (weatherImpact + pullToCenter + noise);

            // 4. Realistic Bounds
            if (movingAQI < 20) movingAQI = 20; // AQI rarely hits 0 in cities
            if (movingAQI > 500) movingAQI = 500;

            list.add(new FuturePredictionModel(i, Math.round(movingAQI)));
        }
        return list;
    }
}