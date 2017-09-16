package com.hackzurich.raphael.googlemapstest;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.GeoApiContext;
import com.google.maps.model.TravelMode;
import com.google.maps.DirectionsApi;
import com.google.maps.model.DirectionsResult;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;
import android.util.Log;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.concurrent.TimeUnit;
import java.util.List;
import org.joda.time.DateTime;


public class MapsTest extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLngBounds latLngBounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_test);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String origin = "Zurich Technopark";
        String destination = "Zurich Mainstation";

        DateTime now = new DateTime();
        try {
            DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(true).mode(TravelMode.TRANSIT).origin(origin).destination(destination).departureTime(now).await();
            addPolylines(resultPT, mMap);

            List<LatLng> decodedPath = PolyUtil.decode(resultPT.routes[0].overviewPolyline.getEncodedPath());
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng latLngPoint : decodedPath)
                boundsBuilder.include(latLngPoint);

            latLngBounds = boundsBuilder.build();
            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    int routePadding = 100;
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,routePadding));
                }
            });

        } catch (Exception e) {
            Log.d("Error", e.toString());
        }

    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3).setApiKey(getString(R.string.google_maps_key)).setConnectTimeout(10, TimeUnit.SECONDS).setReadTimeout(10, TimeUnit.SECONDS).setWriteTimeout(1, TimeUnit.SECONDS);
    }

    private void addPolylines(DirectionsResult results, GoogleMap mMap) {
        for (int i = 0; i< results.routes.length; i++) {
            List<LatLng> decodedPath = PolyUtil.decode(results.routes[i].overviewPolyline.getEncodedPath());
            mMap.addPolyline(new PolylineOptions().addAll(decodedPath));
        }
    }

}
