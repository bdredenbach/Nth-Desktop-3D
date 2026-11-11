package com.nth.desktop3d;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

/** Foreground-only relative tilt, with gravity/accelerometer fallbacks. */
public final class MotionController implements SensorEventListener {
    public interface Listener { void onTilt(float x, float y); int displayRotation(); }
    private final SensorManager manager;
    private final Sensor sensor;
    private final Listener listener;
    private final float[] matrix = new float[9], remapped = new float[9], angles = new float[3], gravity = new float[3];
    private float basePitch, baseRoll, x, y;
    private boolean centered, enabled, running;
    public MotionController(Context context, Listener listener) {
        this.listener = listener;
        manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor found = manager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (found == null) found = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (found == null) found = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensor = found;
    }
    public boolean available() { return sensor != null; }
    public String sensorName() { return sensor == null ? "Touch controls · no tilt sensor" : sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR ? "Fused orientation" : sensor.getType() == Sensor.TYPE_GRAVITY ? "Gravity sensor" : "Filtered accelerometer"; }
    public void resume(boolean useTilt) { enabled = useTilt; if (!useTilt) { pause(); return; } if (!running && sensor != null) { recenter(); running = manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME); } }
    public void pause() { manager.unregisterListener(this); running = false; x = y = 0; listener.onTilt(0, 0); }
    public void recenter() { centered = false; x = y = 0; listener.onTilt(0, 0); }
    @Override public void onSensorChanged(SensorEvent e) {
        if (!enabled || !running) return;
        float pitch, roll;
        int rotation = listener.displayRotation();
        if (sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(matrix, e.values);
            int axisX = SensorManager.AXIS_X, axisY = SensorManager.AXIS_Y;
            if (rotation == Surface.ROTATION_90) { axisX = SensorManager.AXIS_Y; axisY = SensorManager.AXIS_MINUS_X; }
            else if (rotation == Surface.ROTATION_180) { axisX = SensorManager.AXIS_MINUS_X; axisY = SensorManager.AXIS_MINUS_Y; }
            else if (rotation == Surface.ROTATION_270) { axisX = SensorManager.AXIS_MINUS_Y; axisY = SensorManager.AXIS_X; }
            SensorManager.remapCoordinateSystem(matrix, axisX, axisY, remapped);
            SensorManager.getOrientation(remapped, angles);
            pitch = angles[1]; roll = angles[2];
        } else {
            for (int i = 0; i < 3; i++) gravity[i] += .14f * (e.values[i] - gravity[i]);
            float gx = gravity[0], gy = gravity[1], gz = gravity[2];
            if (rotation == Surface.ROTATION_90) { float swap = gx; gx = -gy; gy = swap; }
            else if (rotation == Surface.ROTATION_180) { gx = -gx; gy = -gy; }
            else if (rotation == Surface.ROTATION_270) { float swap = gx; gx = gy; gy = -swap; }
            roll = (float) Math.atan2(-gx, Math.sqrt(gy * gy + gz * gz));
            pitch = (float) Math.atan2(-gy, Math.sqrt(gx * gx + gz * gz));
        }
        if (!centered) { basePitch = pitch; baseRoll = roll; centered = true; }
        // Wrap the angular difference so crossing the +/-pi boundary is harmless.
        float dr = (float) Math.atan2(Math.sin(roll - baseRoll), Math.cos(roll - baseRoll));
        float dp = (float) Math.atan2(Math.sin(pitch - basePitch), Math.cos(pitch - basePitch));
        x += (SceneMath.tilt(dr) - x) * .12f;
        y += (SceneMath.tilt(dp) - y) * .12f;
        listener.onTilt(x, y);
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
