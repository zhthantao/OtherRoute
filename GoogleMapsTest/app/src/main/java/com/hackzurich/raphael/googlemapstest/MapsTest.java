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
import com.google.maps.android.ui.IconGenerator;
import com.google.maps.model.TravelMode;
import com.google.maps.DirectionsApi;
import com.google.maps.model.DirectionsResult;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.*;

import com.google.maps.android.*;
import android.widget.Toast;
import android.content.res.Resources;

import android.util.Log;
import android.graphics.*;
import com.google.android.gms.maps.model.LatLngBounds;

import com.google.android.gms.maps.model.MapStyleOptions;
import android.content.res.Resources;


import java.util.concurrent.TimeUnit;
import org.joda.time.DateTime;
import java.util.*;
import android.widget.*;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;



public class MapsTest extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnPolylineClickListener, GoogleMap.OnMarkerClickListener, AsyncResponse {


    private GoogleMap mMap;
    private LatLngBounds latLngBounds;


    private static final int PATTERN_GAP_LENGTH_PX = 50;
    private static final int PATTERN_DASH_LENGTH_PX = 20;
    private static Polyline[] polylines = new Polyline[3];
    private static DirectionsResult results;
    TextView text1;
    long startTime = 0;
    Map<String, String> originStations = new HashMap<String, String>();
    Map<String, String> destinationStations =  new HashMap<String, String>();
    Handler timerHandler = new Handler();

    String origin = "Giessereistrasse 18, 8005 Zürich";
    String destination = "Tannenstrasse 17, 8006 Zurich";
          
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            text1.setText("Score = " + String.format("%d", (int)(seconds*166.66)));
            timerHandler.postDelayed(this, 500);
            if(seconds>=6)
            {
                text1.setText("Score = " + String.format("%d", 1000));
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_test);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        text1=(TextView)findViewById(R.id.textView1);
        text1.setVisibility(View.GONE);
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

        //mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.mapstyle_night));

            if (!success) {
            }
        } catch (Resources.NotFoundException e) {
        }
/*
        DateTime now = new DateTime();
        try {
            DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(true).mode(TravelMode.TRANSIT).origin(origin).destination(destination).departureTime(now).await();
            results = resultPT;
            //addPolylines(resultPT, mMap);
            addCustomizedPolyline(resultPT, mMap,"Bike", Color.rgb(253,174,97));
            addCustomizedPolyline(resultPT, mMap,"Walk", Color.rgb(215,25,28));
            addCustomizedPolyline(resultPT, mMap,"Bus", Color.rgb(44,200,182));

            addMarkersToMapNew(resultPT,mMap, 0, "200", 0.5);
            addMarkersToMapNew(resultPT,mMap, 1, "1000", 0.5);
            addMarkersToMapNew(resultPT,mMap, 2, "0", 0.1);*/


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

        googleMap.setOnPolylineClickListener(this);
        googleMap.setOnMarkerClickListener(this);
        googleMap.setOnInfoWindowClickListener(this);

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
            /*ArrayList<List<LatLng>> filteredResults = new ArrayList<List<LatLng>>();
            filteredResults.add(PolyUtil.decode(results.get(indices_i.get(indices.get(0))).routes[indices_k.get(indices.get(0))].overviewPolyline.getEncodedPath()));
            mMap.addPolyline(new PolylineOptions().addAll(filteredResults.get(0)).color(Color.RED));
            filteredResults.add(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].overviewPolyline.getEncodedPath()));
            mMap.addPolyline(new PolylineOptions().addAll(filteredResults.get(1)).color(Color.BLUE));
            filteredResults.add(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-1))).routes[indices_k.get(indices.get(indices.size()-1))].overviewPolyline.getEncodedPath()));
            mMap.addPolyline(new PolylineOptions().addAll(filteredResults.get(2)).color(Color.GREEN));*/
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(0))).routes[0].legs[0].steps[0].polyline.getEncodedPath())).color(Color.RED)).setPattern(PATTERN_POLYLINE_DOTTED);
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(0))).routes[0].legs[0].steps[1].polyline.getEncodedPath())).color(Color.RED));
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(0))).routes[0].legs[0].steps[2].polyline.getEncodedPath())).color(Color.RED)).setPattern(PATTERN_POLYLINE_DOTTED);

            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].legs[0].steps[0].polyline.getEncodedPath())).color(Color.BLUE));
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].legs[0].steps[1].polyline.getEncodedPath())).color(Color.BLUE)).setPattern(PATTERN_POLYLINE_DOTTED);
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].legs[0].steps[2].polyline.getEncodedPath())).color(Color.BLUE));
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].legs[0].steps[3].polyline.getEncodedPath())).color(Color.BLUE)).setPattern(PATTERN_POLYLINE_DOTTED);

            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-1))).routes[indices_k.get(indices.get(indices.size()-1))].legs[0].steps[0].polyline.getEncodedPath())).color(Color.GREEN));
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-1))).routes[indices_k.get(indices.get(indices.size()-1))].legs[0].steps[1].polyline.getEncodedPath())).color(Color.GREEN)).setPattern(PATTERN_POLYLINE_DOTTED);
            mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(results.get(indices_i.get(indices.get(indices.size()-1))).routes[indices_k.get(indices.get(indices.size()-1))].legs[0].steps[2].polyline.getEncodedPath())).color(Color.GREEN));

            String originEnd = results.get(indices_i.get(indices.get(0))).routes[0].legs[0].steps[0].startLocation.toString();
            String dest = results.get(indices_i.get(indices.get(0))).routes[0].legs[0].steps[2].endLocation.toString();
            try {
                DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(false).mode(TravelMode.WALKING).origin(origin).destination(originEnd).departureTime(now).await();
                mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(resultPT.routes[0].overviewPolyline.getEncodedPath())).color(Color.RED)).setPattern(PATTERN_POLYLINE_DOTTED);
                resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(false).mode(TravelMode.WALKING).origin(dest).destination(destination).departureTime(now).await();
                mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(resultPT.routes[0].overviewPolyline.getEncodedPath())).color(Color.RED)).setPattern(PATTERN_POLYLINE_DOTTED);

            } catch (Exception e){
                Log.e("Error", e.getMessage());
            }

            originEnd = results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].legs[0].steps[0].startLocation.toString();
            dest = results.get(indices_i.get(indices.get(indices.size()-3))).routes[indices_k.get(indices.get(indices.size()-3))].legs[0].steps[3].endLocation.toString();
            try {
                DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(false).mode(TravelMode.WALKING).origin(origin).destination(originEnd).departureTime(now).await();
                mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(resultPT.routes[0].overviewPolyline.getEncodedPath())).color(Color.BLUE)).setPattern(PATTERN_POLYLINE_DOTTED);
                resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(false).mode(TravelMode.WALKING).origin(dest).destination(destination).departureTime(now).await();
                mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(resultPT.routes[0].overviewPolyline.getEncodedPath())).color(Color.BLUE)).setPattern(PATTERN_POLYLINE_DOTTED);

            } catch (Exception e){
                Log.e("Error", e.getMessage());
            }

            originEnd = results.get(indices_i.get(indices.get(indices.size()-1))).routes[indices_k.get(indices.get(indices.size()-1))].legs[0].steps[0].startLocation.toString();
            dest = results.get(indices_i.get(indices.get(indices.size()-1))).routes[indices_k.get(indices.get(indices.size()-1))].legs[0].steps[3].endLocation.toString();
            try {
                DirectionsResult resultPT = DirectionsApi.newRequest(getGeoContext()).alternatives(false).mode(TravelMode.WALKING).origin(origin).destination(originEnd).departureTime(now).await();
                mMap.addPolyline(new PolylineOptions().addAll(PolyUtil.decode(resultPT.routes[0].overviewPolyline.getEncodedPath())).color(Color.GREEN)).setPattern(PATTERN_POLYLINE_DOTTED);

            } catch (Exception e){
                Log.e("Error", e.getMessage());
            }
        }
    }

    private GeoApiContext getGeoContext() {
        GeoApiContext geoApiContext = new GeoApiContext();
        return geoApiContext.setQueryRateLimit(3).setApiKey(getString(R.string.google_maps_key)).setConnectTimeout(5, TimeUnit.SECONDS).setReadTimeout(5, TimeUnit.SECONDS).setWriteTimeout(1, TimeUnit.SECONDS);
    }

    private void addMarkersToMap(DirectionsResult results, GoogleMap mMap,int index,String reward, double percentage) {
        LatLng middlePoint = computeCentroid(PolyUtil.decode(results.routes[index].overviewPolyline.getEncodedPath()), percentage);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(middlePoint)
                .title(results.routes[0].legs[0].startAddress)
                .visible(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                .title("Reward")
                .snippet(reward));
        marker.showInfoWindow();
        marker.setInfoWindowAnchor(.5f,1.0f);
    }

    private void addMarkersToMapNew(DirectionsResult results, GoogleMap mMap,int index,String reward, double percentage) {
        LatLng middlePoint = computeCentroid(PolyUtil.decode(results.routes[index].overviewPolyline.getEncodedPath()), percentage);
//        Marker marker = mMap.addMarker(new MarkerOptions()
//                .position(middlePoint)
//                .title(results.routes[0].legs[0].startAddress)
//                .visible(true)
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
//                .title("Reward")
//                .snippet(reward));
//        marker.showInfoWindow();
//        marker.setInfoWindowAnchor(.5f,1.0f);

        IconGenerator iconFactory = new IconGenerator(this);
        iconFactory.setStyle(IconGenerator.STYLE_GREEN);
        addIcon(iconFactory, reward, middlePoint);
    }

    private String getEndLocationTitle(DirectionsResult results){
        return  "Time :"+ results.routes[0].legs[0].duration.humanReadable + " Distance :" + results.routes[0].legs[0].distance.humanReadable;
    }

    private void addCustomizedPolyline(DirectionsResult results, GoogleMap mMap, String trafficType, int routeColor) {
    switch (trafficType){
        case "Bike" :
            List<LatLng> decodedPath = PolyUtil.decode(results.routes[0].overviewPolyline.getEncodedPath());
            polylines[0] = mMap.addPolyline(new PolylineOptions()
                    .clickable(true)
                    .addAll(decodedPath)
                    .width(15)
                    .color(routeColor));
            polylines[0].setTag("Bike");
            polylines[0].setPattern(PATTERN_POLYLINE_DOTTED);
            break;
        case "Walk" :
            List<LatLng> decodedPath2 = PolyUtil.decode(results.routes[1].overviewPolyline.getEncodedPath());
            polylines[1] = mMap.addPolyline(new PolylineOptions()
                    .clickable(true)
                    .addAll(decodedPath2)
                    .width(15)
                    .color(routeColor));
            polylines[1].setTag("Walk");
            polylines[1].setPattern(PATTERN_POLYLINE_DASHED);
            break;
        case "Bus" :
            List<LatLng> decodedPath3 = PolyUtil.decode(results.routes[2].overviewPolyline.getEncodedPath());
            polylines[2] = mMap.addPolyline(new PolylineOptions()
                    .clickable(true)
                    .addAll(decodedPath3)
                    .width(15)
                    .color(routeColor));
            polylines[2].setTag("Bus");
            polylines[2].setPattern(PATTERN_POLYLINE_GAPED);
            break;
    }
    }

    private void addPolylines(DirectionsResult results, GoogleMap mMap) {
        for (int i = 0; i< results.routes.length; i++) {
            List<LatLng> decodedPath = PolyUtil.decode(results.routes[i].overviewPolyline.getEncodedPath());
            mMap.addPolyline(new PolylineOptions().addAll(decodedPath).color(Color.argb(125, 5*counter, 5*counter, 5*counter)));
            counter++;
        }
    }
    private static final PatternItem DOT = new Dot();
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    private static final PatternItem DASH = new Dash(PATTERN_DASH_LENGTH_PX);


    // Create a stroke pattern of a gap followed by a dot.
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(DOT);
    private static final List<PatternItem> PATTERN_POLYLINE_GAPED = Arrays.asList(GAP, DOT);
    private static final List<PatternItem> PATTERN_POLYLINE_DASHED = Arrays.asList(DASH);


    @Override
    public void onPolylineClick(Polyline polyline) {
        int reward = 0;
        int time = 0;

        switch (polyline.getTag().toString()) {
            case "Bike":
                reward = 200;
                time = 15;
                break;
            case "Bus":
                reward = 0;
                time = 10;
                break;
            case "Walk":
                reward = 1000;
                time = 25;
                break;
        }
        if (polyline.getWidth() < 20) {
            for (Polyline polylineX : polylines) {
                polylineX.setWidth(15);
            }
            polyline.setWidth(50);
            Toast.makeText(this,"Choose to " + polyline.getTag().toString() + ", it will take " + time + " mins, and get reward of " +  Integer.toString(reward),
                    Toast.LENGTH_LONG).show();
        } else {
            // The default pattern is a solid stroke.
            polyline.setWidth(15);

        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.setTitle("Go!");
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        //Toast.makeText(this, "Info window clicked",Toast.LENGTH_SHORT).show();
        marker.setVisible(false);
        animatedWalk();
    }
    private LatLng computeCentroid(List<LatLng> points, double percentage) {
        int n = points.size();
        return new LatLng(points.get((int)(percentage*n)).latitude,points.get((int)(percentage*n)).longitude);
    }

    private void addIcon(IconGenerator iconFactory, CharSequence text, LatLng position) {
        MarkerOptions markerOptions = new MarkerOptions().
                icon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(text))).
                position(position).
                anchor(iconFactory.getAnchorU(), iconFactory.getAnchorV());

        mMap.addMarker(markerOptions);
    }

    private void animatedWalk() {
        Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(results.routes[0].legs[0]
                .startLocation.lat, results.routes[0].legs[0].startLocation.lng))
                .title(results.routes[0].legs[0].startAddress));

        LatLngInterpolator mLatLngInterpolator = new LatLngInterpolator.Spherical();

        MarkerAnimation.animateMarkerToGB(marker, new LatLng(results.routes[0].legs[0]
                .endLocation.lat, results.routes[0].legs[0].endLocation.lng), mLatLngInterpolator);
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
        text1.setTextColor(Color.WHITE);
        text1.setVisibility(View.VISIBLE);
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
