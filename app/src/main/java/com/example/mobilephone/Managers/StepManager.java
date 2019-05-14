package com.example.mobilephone.Managers;

import android.app.Application;
import android.content.Context;

import com.example.mobilephone.Models.Location;
import com.example.mobilephone.Models.SensorReading;
import com.example.mobilephone.Models.Step;
import com.example.mobilephone.Repositories.LocationRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class StepManager {
    private final double PATTERN_TIMEOUT = 5;    //5 seconds
    private final int SAMPLES_PER_SECOND = 15;
    private final int MIN_SAMPLES = SAMPLES_PER_SECOND / 3;
    private final int MAX_SAMPLES = (int) PATTERN_TIMEOUT * SAMPLES_PER_SECOND;

    @Inject LocationRepository locationRepository;

    private final double PRESSURE_THRESHOLD = 50.0;
    private final long TIME_THRESHOLD = 500;

    private SensorAnalyzer topSensorAnalyzer;
    private SensorAnalyzer bottomSensorAnalyzer;

    private enum SensorLocations {TOP, BOTTOM};

    private PatternEvent lastTopSensorPattern;
    private PatternEvent lastBottomSensorPattern;

    //TODO Should have some location manager

    private OnStepCreatedEventListener onStepCreatedEventListener;

    public StepManager(Context context) {
        locationRepository = new LocationRepository(context);
        topSensorAnalyzer = new SensorAnalyzer(PRESSURE_THRESHOLD, pattern -> onSensorDataPatternReceived(pattern, SensorLocations.TOP));
        bottomSensorAnalyzer = new SensorAnalyzer(PRESSURE_THRESHOLD, pattern -> onSensorDataPatternReceived(pattern, SensorLocations.BOTTOM));
    }

    public void setOnStepCreatedEventListener(OnStepCreatedEventListener eventListener) {
        onStepCreatedEventListener = eventListener;
    }

    public void addTopSensorReadings(List<SensorReading> sensorReadings) {
        topSensorAnalyzer.submitSamples(sensorReadings);
    }

    public void addBottomSensorReadings(List<SensorReading> sensorReadings) {
        bottomSensorAnalyzer.submitSamples(sensorReadings);
    }

    private void onSensorDataPatternReceived(SensorPattern pattern, SensorLocations sensor) {
        if (pattern.getLength() > MIN_SAMPLES && pattern.getLength() < MAX_SAMPLES) {
            cachePattern(pattern, sensor);

            // Either two patterns have been discovered and a step should be created,
            // or a new pattern has been discovered for the same sensor, and a step should be
            // created with only a single sensor reading

            //If both top and bottom patterns have been discovered and the time between them is
            //small enough, create a step using both. Otherwise, a step should be created with the
            //older sample and the newer sample should wait to see if it can be sent with the other
            //sensor pattern
            if (lastBottomSensorPattern != null && lastTopSensorPattern != null) {
                if (areSensorPatternsCorrelated()) {
                    //create step with both
                    createStep(Arrays.asList(lastTopSensorPattern, lastBottomSensorPattern));

                    clearCache();
                } else {
                    //create step with oldest
                    PatternEvent oldest = getOldestPattern();
                    createStep(Arrays.asList(oldest));

                    if (oldest.location == SensorLocations.TOP) lastTopSensorPattern = null;
                    else lastBottomSensorPattern = null;
                }
            } else if (lastTopSensorPattern != null && sensor == SensorLocations.TOP) {
                createStep(Arrays.asList(lastTopSensorPattern));
                overwriteCache(pattern, sensor);
            } else if (lastBottomSensorPattern != null && sensor == SensorLocations.BOTTOM) {
                createStep(Arrays.asList(lastBottomSensorPattern));
                overwriteCache(pattern, sensor);
            }
        }
    }

    private PatternEvent getOldestPattern() {
        if (lastBottomSensorPattern.timestamp < lastTopSensorPattern.timestamp) {
            return lastBottomSensorPattern;
        }

        return lastTopSensorPattern;
    }

    private boolean areSensorPatternsCorrelated() {
        boolean correlated = false;

        if(lastBottomSensorPattern != null && lastTopSensorPattern != null) {
            if (Math.abs(lastTopSensorPattern.timestamp - lastBottomSensorPattern.timestamp) < TIME_THRESHOLD) {
                correlated = true;
            }
        }

        return correlated;
    }

    private void overwriteCache(SensorPattern pattern, SensorLocations sensor) {
        if (sensor == SensorLocations.TOP) {
            lastTopSensorPattern = new PatternEvent(System.currentTimeMillis(), pattern, sensor);
        } else {
            lastBottomSensorPattern = new PatternEvent(System.currentTimeMillis(), pattern, sensor);

        }
    }

    /**
     * Only Cache if no patten exists yet.
     * @param pattern
     * @param sensor
     */
    private void cachePattern(SensorPattern pattern, SensorLocations sensor) {
        if (sensor == SensorLocations.TOP) {
            if (lastTopSensorPattern == null) {
                lastTopSensorPattern = new PatternEvent(System.currentTimeMillis(), pattern, sensor);
            }
        } else {
            if (lastBottomSensorPattern == null) {
                lastBottomSensorPattern = new PatternEvent(System.currentTimeMillis(), pattern, sensor);
            }
        }
    }

    private void clearCache() {
        lastBottomSensorPattern = null;
        lastTopSensorPattern = null;
    }

    private List<SensorReading> createPressureList(List<PatternEvent> patternEvents) {
        ArrayList<SensorReading> pressureList = new ArrayList<>();

        SensorReading topReading = null;
        SensorReading bottomReading = null;

        for (PatternEvent pattern: patternEvents) {
            if (pattern.location == SensorLocations.TOP) {
                topReading = new SensorReading("T", pattern.pattern.getAvgPressure());
                pressureList.add(topReading);
            } else if (pattern.location == SensorLocations.BOTTOM) {
                bottomReading = new SensorReading("B", pattern.pattern.getAvgPressure());
                pressureList.add(bottomReading);
            }
        }

        return pressureList;
    }

    private void createStep(List<PatternEvent> patternEvents) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        String datetime = sdf.format(new Date());

        android.location.Location lastLocation = locationRepository.getLastLocation();
        Location location;
        if (lastLocation != null) {
            location = new Location(lastLocation.getLatitude(), lastLocation.getLongitude());
        } else {
            location = new Location(0, 0);
        }

        Step step = new Step(datetime, createPressureList(patternEvents), location);

        onStepCreatedEventListener.onStepCreated(step);
    }

    private class PatternEvent {
        public long timestamp;
        public SensorLocations location;
        public SensorPattern pattern;

        public PatternEvent(long timestamp, SensorPattern pattern, SensorLocations location) {
            this.timestamp = timestamp;
            this.pattern = pattern;
            this.location = location;
        }
    }
}
