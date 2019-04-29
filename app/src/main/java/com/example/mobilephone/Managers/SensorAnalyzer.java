package com.example.mobilephone.Managers;

import com.example.mobilephone.Models.SensorReading;

import java.util.ArrayList;
import java.util.List;

public class SensorAnalyzer {
    private double threshold;
    private ArrayList<SensorReading> samples;
    private PressureState state;

    private OnPatternDetectedEventListener onPatternDetectedEventListener;

    public SensorAnalyzer(double threshold, OnPatternDetectedEventListener eventListener) {
        this.threshold = threshold;
        this.samples = new ArrayList<>();

        this.state = PressureState.INIT;

        this.onPatternDetectedEventListener = eventListener;
    }

    public void submitSamples(List<SensorReading> samples) {
        for (SensorReading sample: samples) {
            changeState(sample.getPressure());

            if (state == PressureState.PRESSURE_DOWN || state == PressureState.PRESSED) {
                this.samples.add(sample);
            }

            if (state == PressureState.PRESSURE_UP) {
                patternDetected();
                this.samples.clear();
            }
        }
    }

    private double getAverage() {
        double average = 0;

        for (SensorReading sample: samples) {
            average += sample.getPressure();
        }

        if (average != 0) {
            average /= samples.size();
        }

        return average;
    }

    private void patternDetected() {
        SensorPattern detectedPattern = new SensorPattern(samples.size(), getAverage());

        onPatternDetectedEventListener.onPatternDetected(detectedPattern);
    }

    private void changeState(double pressure) {
        switch (state) {
            case INIT:
                if (pressure >= threshold) {
                    state = PressureState.PRESSURE_DOWN;
                } else {
                    state = PressureState.RELEADED;
                }

                break;

            case PRESSURE_DOWN:
                if (pressure >= threshold) {
                    state = PressureState.PRESSED;
                } else {
                    state = PressureState.PRESSURE_UP;
                }

                break;

            case PRESSED:
                if (pressure < threshold) {
                    state = PressureState.PRESSURE_UP;
                }

                break;

            case PRESSURE_UP:
                if (pressure >= threshold) {
                    state = PressureState.PRESSURE_DOWN;
                } else  {
                    state = PressureState.RELEADED;
                }

                break;

            case RELEADED:
                if (pressure >= threshold) {
                    state = PressureState.PRESSURE_DOWN;
                }

                break;
        }
    }
}
