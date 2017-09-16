package com.hackzurich.raphael.googlemapstest;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public final class LatLngUtils {

    private LatLngUtils() {
    }

    public static float distanceBetween(LatLng first, LatLng second) {
        float[] distance = new float[1];
        Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, distance);
        return distance[0];
    }

    public static LatLng fromLocation(Location location) {
        return new LatLng(location.getLatitude(), location.getLongitude());
    }
}
