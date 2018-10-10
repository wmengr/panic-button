package com.wm.panicbutton.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.wm.panicbutton.interfaces.Processor;

import org.json.JSONObject;


public class SignalProcessor implements Processor {

    private static final String TAG = "PROCESSOR";
    private static final String EMERGENCY_NUMBER = "6692219715";
    private static final String MESSAGE_FOOTER = "\nSent from Panic Button application";
    private static final String SIGNAL_1 = "1";
    private static final String SIGNAL_2 = "2";
    private static final String SIGNAL_3 = "3";
    private static final String SEVERITY_MESSAGE_SIGNAL_1 = "INFO\n";
    private static final String SEVERITY_MESSAGE_SIGNAL_2 = "WARNING!\n";
    private static final String SEVERITY_MESSAGE_SIGNAL_3 = "DANGER!!\n";
    private static final String BLUEMIX_URL = "https://nrupatest1.mybluemix.net/log?";
    private static final String BLUEMIX_PARAM_1 = "lat=";
    private static final String BLUEMIX_PARAM_2 = "&long=";
    private static final String BLUEMIX_PARAM_3 = "&sev=";

    private Context context;
    private Sms sms;
    private ContactManager contactManager;
    private static String severity;

    public SignalProcessor(Context context) {
        this.context = context;
        sms = new Sms(this.context);
        contactManager = new ContactManager(context);
    }

    @Override
    public void process(String signal) {
        Log.i(TAG, signal);
        severity = signal;
        String message = LocationUpdater.getAddress() == null? null:getSeverityMessage() + LocationUpdater.getAddress() + "\n" + MESSAGE_FOOTER + " " + LocationUpdater.getMapsURL();
        switch (severity) {
            case SIGNAL_1: {
                if (contactManager.hasFavoriteContact()) {
                    sms.sendSMSs(contactManager.getFavoriteContacts(), message);
                } else {
                    sms.sendSMSs(contactManager.getContacts(), message);
                }
                updateLocation();
                break;
            }
            case SIGNAL_2: {
                sms.sendSMSs(contactManager.getContacts(), message);
                updateLocation();
                break;
            }
            case SIGNAL_3: {
                sms.sendSMSs(contactManager.getContacts(), message);
                emergencyCall();
                updateLocation();
                break;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void emergencyCall() {
        Log.i(TAG, "emergency call activated");
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + EMERGENCY_NUMBER));
        context.startActivity(intent);
    }

    private void updateLocation() {
        String url = BLUEMIX_URL + BLUEMIX_PARAM_1 + LocationUpdater.getLatitude() + BLUEMIX_PARAM_2 + LocationUpdater.getLongitude() + BLUEMIX_PARAM_3 + severity;
        Log.i(TAG, "bluemix url: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "response: " + response.toString());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "error: " + error.getMessage());
                    }
                });

        Singleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    public static String getSeverityMessage() {
        switch (severity) {
            case SIGNAL_1: {
                return SEVERITY_MESSAGE_SIGNAL_1;
            }
            case SIGNAL_2: {
                return SEVERITY_MESSAGE_SIGNAL_2;
            }
            case SIGNAL_3: {
                return SEVERITY_MESSAGE_SIGNAL_3;
            }
            default: {
                return SEVERITY_MESSAGE_SIGNAL_1;
            }
        }
    }

}
