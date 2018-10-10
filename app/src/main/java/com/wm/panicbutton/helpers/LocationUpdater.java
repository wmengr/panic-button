package com.wm.panicbutton.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.wm.panicbutton.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wm on 3/27/18.
 */

public class LocationUpdater {

    private static final String TAG = "LOCATION";
    private static final int INTERVAL_TIME_MILLISECONDS = 60000;
    private static final int INTERVAL_DISTANCE_METERS = 5;

    private static final String KEY_LATITUDE_LONGITUDE = "${latlng}";
    private static final String URL_GOOGLE_API_GEOCODE = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + KEY_LATITUDE_LONGITUDE + "&key=" + BuildConfig.GoogleSecureAPIKey;
    private static final String URL_GOOGLE_MAPS_SEARCH = "https://www.google.com/maps/search/?api=1&query=";

    private Context context;
    private LocationManager lm;
    private static double latitude;
    private static double longitude;
    private static String address;
    private static String mapsURL;

    @SuppressLint("MissingPermission")
    public LocationUpdater(Context context) {
        this.context = context;
        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(android.location.Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            Log.i(TAG, "lat: " + Double.toString(latitude) + ", long: " + Double.toString(longitude));
            requestGeocode();
            mapsURL = URL_GOOGLE_MAPS_SEARCH + latitude + "," + longitude;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @SuppressLint("MissingPermission")
    public void activate() {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_TIME_MILLISECONDS, INTERVAL_DISTANCE_METERS, locationListener);
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, INTERVAL_TIME_MILLISECONDS, INTERVAL_DISTANCE_METERS, locationListener);
    }

    @SuppressLint("MissingPermission")
    public void deactivate() {
        lm.removeUpdates(locationListener);
    }

    @SuppressLint("MissingPermission")
    private void requestGeocode() {
        String latlng = latitude + "," + longitude;
        String url = URL_GOOGLE_API_GEOCODE.replace(KEY_LATITUDE_LONGITUDE, latlng);
        Log.i(TAG, "request geocode url: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        address = getAddress(response);
                        Log.i(TAG, "address: " + address);
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        address = null;
                        Log.e(TAG, "error: " + error.getMessage());
                    }
                });

        Singleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    private String getAddress(JSONObject response) {
        try {
            Log.i(TAG, "response geocode: " + response.toString());
            return response.getJSONArray("results").getJSONObject(0).getString("formatted_address");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static  double getLatitude() {
        return latitude;
    }

    public static double getLongitude() {
        return  longitude;
    }

    public static String getAddress() {
        return address;
    }

    public static String getMapsURL() {
        return mapsURL;
    }

}
