package com.wm.panicbutton.helpers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Sms {

    private Context context;

    private static final String TAG = "MESSAGE";
    private static final int INTENT_REQUEST_CODE = 0;


    public Sms(Context context) {
        this.context = context;
    }

    public void sendSMSs(HashMap<String, String> contacts, String message) {
        for (Map.Entry<String, ?> entry : contacts.entrySet()) {
            sendSMS((String)entry.getValue(), message);
        }
    }

    public void sendSMS(String phoneNumber, String message) {
        Log.i(TAG, "phone number: " + phoneNumber + ", message: " + message);
        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<>();
        PendingIntent sentPI = PendingIntent.getBroadcast(context, INTENT_REQUEST_CODE,
                new Intent(context, SmsSentReceiver.class), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, INTENT_REQUEST_CODE,
                new Intent(context, SmsDeliveredReceiver.class), 0);
        try {
            android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
            ArrayList<String> mSMSMessage = sms.divideMessage(message);
            for (int i = 0; i < mSMSMessage.size(); i++) {
                sentPendingIntents.add(i, sentPI);
                deliveredPendingIntents.add(i, deliveredPI);
            }
            sms.sendMultipartTextMessage(phoneNumber, null, mSMSMessage,
                    sentPendingIntents, deliveredPendingIntents);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "SMS sending failed...", Toast.LENGTH_SHORT).show();
        }
    }
}
