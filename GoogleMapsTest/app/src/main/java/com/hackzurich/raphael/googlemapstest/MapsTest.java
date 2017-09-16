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
import com.google.maps.android.*;
import com.google.android.gms.maps.model.Polyline;
import android.util.Log;
import android.graphics.*;
import com.google.android.gms.maps.model.LatLngBounds;

import com.google.android.gms.maps.model.MapStyleOptions;
import android.content.res.Resources;


import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import java.util.*;


public class MapsTest extends FragmentActivity implements OnMapReadyCallback, AsyncResponse {

    private GoogleMap mMap;
    private LatLngBounds latLngBounds;

    ArrayList<String> originStations = new ArrayList<String>();
    ArrayList<String> destinationStations = new ArrayList<String>();

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
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));

            if (!success) {
                Log.e("Style", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("Style", "Can't find style. Error: ", e);
        }
        String origin = "Zurich Technopark";
        String destination = "Tannenstrasse 17, 8006 Zurich";

        originStations.add(origin);
        destinationStations.add(destination);

        DateTime now = new DateTime();
        try {
            DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(true).mode(TravelMode.TRANSIT).origin(origin).destination(destination).departureTime(now).await();
            //addPolylines(resultPT, mMap);
            int radius = 300;
            getNearbyStations(resultPT.routes[0].legs[0].startLocation.lat,resultPT.routes[0].legs[0].startLocation.lng,radius, "origin");
            getNearbyStations(resultPT.routes[0].legs[resultPT.routes[0].legs.length-1].endLocation.lat,resultPT.routes[0].legs[resultPT.routes[0].legs.length-1].endLocation.lng,radius, "destination");

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

    public void processFinish(String output){
        String[] resultSplit = output.split("///",2);
        List<HashMap<String, String>> nearbyPlacesList = null;
        DataParser dataParser = new DataParser();
        nearbyPlacesList =  dataParser.parse(resultSplit[1]);

        // Combine stations with same name and add them to a list of strings. 1 List for origin and one for destination
        for (int i = 0; i < nearbyPlacesList.size(); i++) {
            HashMap<String, String> googlePlace = nearbyPlacesList.get(i);
            String stationName = googlePlace.get("place_name");
            if (resultSplit[0].equals("origin")) {
                if (!originStations.contains(stationName)) {
                    originStations.add(stationName);
                    Log.d("Station", resultSplit[0]+" "+stationName);
                }
            } else {
                if (!destinationStations.contains(stationName)) {
                    destinationStations.add(stationName);
                    Log.d("Station", resultSplit[0]+" "+stationName);
                }
            }

        }
        // Get routes between all stations of both list
        if ((destinationStations.size() > 1) && (originStations.size() > 1)) {
            DateTime now = new DateTime();
            for (int i = 0; i < originStations.size(); i++) {
                for (int j = 0; j < destinationStations.size(); j++) {
                    try {
                        DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(false).mode(TravelMode.TRANSIT).origin(originStations.get(i)).destination(destinationStations.get(j)).departureTime(now).await();
                        // Rank and select routes

                        addPolylines(resultPT, mMap);
                    } catch (Exception e) {
                        Log.e("Critical", e.getMessage());
                    }
                }
            }

        }
    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3).setApiKey(getString(R.string.google_maps_key)).setConnectTimeout(10, TimeUnit.SECONDS).setReadTimeout(10, TimeUnit.SECONDS).setWriteTimeout(1, TimeUnit.SECONDS);
    }

    private void addMarkersToMap(DirectionsResult results, GoogleMap mMap) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0].startLocation.lat,results.routes[0].legs[0].startLocation.lng)).title(results.routes[0].legs[0].startAddress));
        mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0].endLocation.lat,results.routes[0].legs[0].endLocation.lng)).title(results.routes[0].legs[0].startAddress).snippet(getEndLocationTitle(results)));
    }

    private String getEndLocationTitle(DirectionsResult results){
        return  "Time :"+ results.routes[0].legs[0].duration.humanReadable + " Distance :" + results.routes[0].legs[0].distance.humanReadable;
    }

    private void addCustomizedPolyline(DirectionsResult result, GoogleMap mMap) {
        List<LatLng> decodedPath = PolyUtil.decode(result.routes[0].overviewPolyline.getEncodedPath());

        Polyline line = mMap.addPolyline(new PolylineOptions()
                .clickable(true)
                .addAll(decodedPath)
                .width(15)
                .color(Color.RED));
    }

    private void addPolylines(DirectionsResult results, GoogleMap mMap) {
        for (int i = 0; i< results.routes.length; i++) {
            List<LatLng> decodedPath = PolyUtil.decode(results.routes[i].overviewPolyline.getEncodedPath());
            mMap.addPolyline(new PolylineOptions().addAll(decodedPath).color(Color.argb(125, 5*counter, 5*counter, 5*counter)));
            counter++;
        }
    }

    private int counter = 0;

    private void getNearbyStations(double latitude, double longitude, int radius, String stationName) {
        String url = getUrl(latitude, longitude, "transit_station", radius);
        Object[] DataTransfer = new Object[3];
        DataTransfer[0] = mMap;
        DataTransfer[1] = url;
        DataTransfer[2] = stationName;
        Log.d("onClick", url);
        GetNearByPlacesData getNearbyPlacesData = new GetNearByPlacesData();
        getNearbyPlacesData.delegate = this;
        getNearbyPlacesData.execute(DataTransfer);

    }

    private String getUrl(double latitude, double longitude, String nearbyPlace, int radius) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        googlePlacesUrl.append("&radius=" + radius);
        googlePlacesUrl.append("&type=" + nearbyPlace);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + getString(R.string.google_maps_key));
        Log.d("getUrl", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }

}
