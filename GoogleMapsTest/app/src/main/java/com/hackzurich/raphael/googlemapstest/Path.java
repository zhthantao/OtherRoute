package com.hackzurich.raphael.googlemapstest;
import com.google.android.gms.maps.model.LatLng;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import java.util.*;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import static java.lang.Math.*;


public class Path implements LatLngInterpolator {

    private List<LatLng> mPath;
    private int mFromIdx;
    private int mToIdx;
    private float mFromFraction;
    private float mToFraction;
    private LinearFixed mOffPathInterpolator = new LinearFixed();
    private boolean mComputed;

    public Path(List<LatLng> path) {
        mPath = path;
        mComputed = false;
    }

    @Override
    public LatLng interpolate(float fraction, LatLng a, LatLng b) {
        if(!mComputed) {
            compute(a, b);
            mComputed = true;
        }

        if(fraction < mFromFraction) {
            // linear interpolation from
            // origin to first point on path
            return mOffPathInterpolator.interpolate(fraction / mFromFraction, a, mPath.get(mFromIdx));
        } else //
            if(fraction > mToFraction) {
                // linear interpolation
                // off path to destination
                return mOffPathInterpolator.interpolate((fraction - mToFraction) / (1f - mToFraction), mPath.get(mToIdx), b);
            } else {
                int idx = Math.round((fraction - mFromFraction) * (mToIdx - mFromIdx));
                return mPath.get(idx);
            }
    }

    private void compute(LatLng a, LatLng b) {
       // mFromIdx = LatLngUtils.getNearestPointIdx(mPath, a);
       // mToIdx = LatLngUtils.getNearestPointIdx(mPath, b);
        mFromIdx = 0;
        mToIdx = mPath.size();
        //int distancePath = (int) LatLngUtils.calculatePathDistance(mPath, mFromIdx, mToIdx);
        int distanceA = (int) LatLngUtils.distanceBetween(a, mPath.get(mFromIdx));
        int distanceB = (int) LatLngUtils.distanceBetween(b, mPath.get(mToIdx));
        //int distance = distanceA + distancePath + distanceB;
        int distance = distanceA  + distanceB;

        mFromFraction = (float) (((double) distanceA) / ((double) distance));
        mToFraction = (float) (1f - ((double) distanceB) / ((double) distance));

    }
}