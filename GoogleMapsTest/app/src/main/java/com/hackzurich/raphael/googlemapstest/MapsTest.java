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

    Map<String, String> originStations = new HashMap<String, String>();
    Map<String, String> destinationStations =  new HashMap<String, String>();

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

        String origin = "Giessereistrasse 18, 8005 Zürich";
        String destination = "Tannenstrasse 17, 8006 Zurich";

        //originStations.add(origin);
        //destinationStations.add(destination);

        DateTime now = new DateTime(2017,9,18,15,30);
        try {
            DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(false).mode(TravelMode.TRANSIT).origin(origin).destination(destination).departureTime(now).await();
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
                if (!originStations.containsKey(stationName)) {
                    originStations.put(stationName, googlePlace.get("lat") +","+googlePlace.get("lng"));
                    //Log.d("Station", resultSplit[0]+" "+stationName);
                }
            } else {
                if (!destinationStations.containsKey(stationName)) {
                    destinationStations.put(stationName, googlePlace.get("lat") +","+googlePlace.get("lng"));
                    //Log.d("Station", resultSplit[0]+" "+stationName);
                }
            }

        }
        String[] originNames = originStations.keySet().toArray(new String[originStations.keySet().size()]);
        String[] destinationNames = destinationStations.keySet().toArray(new String[destinationStations.keySet().size()]);
        // Get routes between all stations of both list
        if ((destinationStations.size() > 1) && (originStations.size() > 1)) {
            ArrayList<DirectionsResult> results = new ArrayList<DirectionsResult>();
            DateTime now = new DateTime(2017,9,18,15,30);
            for (int i = 0; i < originStations.size(); i++) {
                for (int j = 0; j < destinationStations.size(); j++) {
                    try {
                        results.add(DirectionsApi.newRequest(getGeoContext()).alternatives(true).mode(TravelMode.TRANSIT).origin(originStations.get(originNames[i])).destination(destinationStations.get(destinationNames[j])).departureTime(now).await());
                        //addPolylines(resultPT, mMap);
                    } catch (Exception e) {
                        Log.e("Critical", e.getMessage());
                    }
                }
            }
            final ArrayList<Float> distances = new ArrayList<Float>();
            ArrayList<Integer> indices_i = new ArrayList<Integer>();
            ArrayList<Integer> indices_k = new ArrayList<Integer>();
            for (int i = 0; i < results.size(); i++) {
                // Rank and select routes
                for (int k = 0; k < results.get(i).routes.length; k++) {
                    float dist = Float.parseFloat(results.get(i).routes[k].legs[0].distance.humanReadable.split(" ")[0]);
                    indices_i.add(i);
                    indices_k.add(k);
                    distances.add(dist);
                }

            }
            //Collections.sort(distances);
            List<Integer> indices = new ArrayList<Integer>(distances.size());
            for (int l = 0; l < distances.size(); l++) {
                indices.add(l);
            }
            Comparator<Integer> comparator = new Comparator<Integer>() {
                public int compare(Integer i, Integer j) {
                    return Float.compare(distances.get(i), distances.get(j));
                }
            };
            Collections.sort(indices, comparator);
            float[] sortedDistances = new float[distances.size()];
            for (int l = 0; l < distances.size(); l++) {
                sortedDistances[l] = distances.get(indices.get(l));
            }
            ArrayList<List<LatLng>> filteredResults = new ArrayList<List<LatLng>>();
            filteredResults.add(PolyUtil.decode(results.get(indices_i.get(indices.get(0))).routes[indices_k.get(indices.get(0))].overviewPolyline.getEncodedPath()));
            mMap.addPolyline(new PolylineOptions().addAll(filteredResults.get(0)).color(Color.RED));
            filteredResults.add(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].overviewPolyline.getEncodedPath()));
            mMap.addPolyline(new PolylineOptions().addAll(filteredResults.get(1)).color(Color.BLUE));
            filteredResults.add(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-1))).routes[indices_k.get(indices.get(indices.size()-1))].overviewPolyline.getEncodedPath()));
            mMap.addPolyline(new PolylineOptions().addAll(filteredResults.get(2)).color(Color.GREEN));
            //mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(7).routes[0].legs[0].steps[0].polyline.getEncodedPath())).color(Color.GREEN));

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
        //Log.d("onClick", url);
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
