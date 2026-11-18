package com.smartaqi.myapplication;

import java.util.List;

public class AqiResponse {

    public List<Data> list;

    public static class Data {
        public Main main;
    }

    public static class Main {
        public int aqi;
    }
}