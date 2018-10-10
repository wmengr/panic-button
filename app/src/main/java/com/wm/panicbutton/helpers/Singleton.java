package com.wm.panicbutton.helpers;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


public class Singleton {
    private static Singleton mInstance;
    private RequestQueue mRequestQueue;
    private static Context mContext;

    private Singleton(Context context){
        // Specify the application context
        mContext = context;
        // Get the request queue
        mRequestQueue = getRequestQueue();
    }

    public static synchronized Singleton getInstance(Context context){
        // If Instance is null then initialize new Instance
        if(mInstance == null){
            mInstance = new Singleton(context);
        }
        // Return MySingleton new Instance
        return mInstance;
    }

    public RequestQueue getRequestQueue(){
        // If RequestQueue is null the initialize new RequestQueue
        if(mRequestQueue == null){
            mRequestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        }

        // Return RequestQueue
        return mRequestQueue;
    }

    public<T> void addToRequestQueue(Request<T> request){
        // Add the specified request to the request queue
        getRequestQueue().add(request);
    }
}