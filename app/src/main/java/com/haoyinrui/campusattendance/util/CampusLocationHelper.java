package com.haoyinrui.campusattendance.util;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * 校园范围定位校验工具。
 *
 * 初版采用“模拟校园中心点 + 半径”的方式，不接入地图 SDK。
 * 用户点击签到时才检查权限和当前位置，避免默认后台定位。
 */
public class CampusLocationHelper {
    private static final String PREF_NAME = "campus_location_pref";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_RADIUS = "radius";

    private static final String DEFAULT_LATITUDE = "39.9042";
    private static final String DEFAULT_LONGITUDE = "116.4074";
    private static final String DEFAULT_RADIUS = "1000";

    private final Context context;
    private final SharedPreferences preferences;

    public CampusLocationHelper(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return preferences.getBoolean(KEY_ENABLED, false);
    }

    public void saveSettings(boolean enabled, String latitude, String longitude, String radius) {
        preferences.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putString(KEY_LATITUDE, latitude)
                .putString(KEY_LONGITUDE, longitude)
                .putString(KEY_RADIUS, radius)
                .apply();
    }

    public void resetDefaults() {
        saveSettings(false, DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_RADIUS);
    }

    public String getLatitude() {
        return preferences.getString(KEY_LATITUDE, DEFAULT_LATITUDE);
    }

    public String getLongitude() {
        return preferences.getString(KEY_LONGITUDE, DEFAULT_LONGITUDE);
    }

    public String getRadius() {
        return preferences.getString(KEY_RADIUS, DEFAULT_RADIUS);
    }

    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public Location getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        Location bestLocation = null;
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            try {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null && (bestLocation == null
                        || location.getTime() > bestLocation.getTime())) {
                    bestLocation = location;
                }
            } catch (SecurityException ignored) {
                return null;
            }
        }
        return bestLocation;
    }

    public boolean isInCampus(Location location) {
        if (location == null) {
            return false;
        }
        try {
            double campusLatitude = Double.parseDouble(getLatitude());
            double campusLongitude = Double.parseDouble(getLongitude());
            float radius = Float.parseFloat(getRadius());
            float[] results = new float[1];
            Location.distanceBetween(
                    campusLatitude,
                    campusLongitude,
                    location.getLatitude(),
                    location.getLongitude(),
                    results);
            return results[0] <= radius;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public String getDescription() {
        return "校园中心：" + getLatitude() + ", " + getLongitude()
                + "，允许半径：" + getRadius() + "米";
    }
}
