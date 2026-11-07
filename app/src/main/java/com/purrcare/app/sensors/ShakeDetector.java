package com.purrcare.app.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * SENSOR: uses the accelerometer (SensorManager + SensorEventListener) to detect a shake.
 *
 * How it avoids accidental triggers:
 *  1. A movement only counts if it is stronger than SHAKE_THRESHOLD_G (2.7 x gravity).
 *  2. Two counted movements must happen (REQUIRED_SHAKES) - a single bump is ignored.
 *  3. Movements closer than MIN_TIME_BETWEEN_SHAKES_MS are treated as the same shake.
 *  4. If more than SHAKE_RESET_MS passes, the counter starts again from 0.
 *  5. After a successful shake nothing can trigger again for COOLDOWN_MS.
 */
public class ShakeDetector implements SensorEventListener {

    public interface OnShakeListener {
        void onShake();
    }

    private static final float SHAKE_THRESHOLD_G = 2.7f;
    private static final int MIN_TIME_BETWEEN_SHAKES_MS = 500;
    private static final int SHAKE_RESET_MS = 3000;
    private static final int REQUIRED_SHAKES = 2;
    private static final int COOLDOWN_MS = 4000;

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final OnShakeListener listener;

    private long lastShakeTime = 0;
    private long lastTriggerTime = 0;
    private int shakeCount = 0;

    public ShakeDetector(Context context, OnShakeListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    /** Start listening (call from onResume). Returns false if the phone has no accelerometer. */
    public boolean start() {
        if (accelerometer == null) return false;
        return sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    /** Stop listening (call from onPause) to save battery. */
    public void stop() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Convert the acceleration to "g-force" (1 g = normal gravity when the phone is still)
        float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;
        double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (gForce < SHAKE_THRESHOLD_G) return;

        long now = System.currentTimeMillis();
        if (now - lastShakeTime < MIN_TIME_BETWEEN_SHAKES_MS) return;   // same shake
        if (now - lastShakeTime > SHAKE_RESET_MS) shakeCount = 0;       // too slow, start again
        lastShakeTime = now;
        shakeCount++;

        if (shakeCount >= REQUIRED_SHAKES && now - lastTriggerTime > COOLDOWN_MS) {
            lastTriggerTime = now;
            shakeCount = 0;
            listener.onShake();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not needed
    }
}
