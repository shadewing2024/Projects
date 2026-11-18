package com.smartaqi.myapplication;

public class WeatherResponse {

    private Main main;
    private Wind wind;
    private String name; // city name (optional but useful)

    public Main getMain() {
        return main;
    }

    public Wind getWind() {
        return wind;
    }

    public String getName() {
        return name;
    }

    // MAIN (temperature, humidity, pressure)
    public static class Main {

        private double temp;
        private int humidity;
        private double pressure;

        public double getTemp() {
            return temp;
        }

        // Convert Kelvin → Celsius (very useful)
        public double getTempCelsius() {
            return temp - 273.15;
        }

        public int getHumidity() {
            return humidity;
        }

        public double getPressure() {
            return pressure;
        }
    }

    // WIND DATA
    public static class Wind {

        private double speed;

        public double getSpeed() {
            return speed;
        }
    }
}