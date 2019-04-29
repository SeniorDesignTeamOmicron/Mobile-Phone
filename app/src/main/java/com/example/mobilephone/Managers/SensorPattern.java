package com.example.mobilephone.Managers;

public class SensorPattern {
    private int length;
    private double avgPressure;

    public SensorPattern(int length, double avgPressure) {
        this.length = length;
        this.avgPressure = avgPressure;
    }

    public int getLength() {
        return length;
    }

    public double getAvgPressure() {
        return avgPressure;
    }
}
