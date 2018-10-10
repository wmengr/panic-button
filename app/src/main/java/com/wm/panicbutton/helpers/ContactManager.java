package com.wm.panicbutton.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.wm.panicbutton.R;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wm on 3/26/18.
 */

public class ContactManager {

    private Context context;
    private SharedPreferences contactStorage;
    private static HashMap<String, String> favoriteContactsMap;
    private static HashMap<String, String> contactsMap;

    public ContactManager(Context context) {
        this.context = context;
        contactStorage = context.getSharedPreferences(Constants.SHARED_PREFERENCES_KEY_CONTACTS, Context.MODE_PRIVATE);
    }

    public HashMap<String, String> getContacts() {
        contactsMap = new HashMap<>();
        Map<String, ?> contacts = contactStorage.getAll();
        for (Map.Entry<String, ?> entry : contacts.entrySet()) {
            String name = entry.getKey();
            String contact = (String)entry.getValue();
            contactsMap.put(name, contact);
        }
        return contactsMap;
    }

    public HashMap<String, String> getFavoriteContacts() {
        favoriteContactsMap = new HashMap<>();
        Map<String, ?> contacts = contactStorage.getAll();
        for (Map.Entry<String, ?> entry : contacts.entrySet()) {
            String name = entry.getKey();
            String contact = (String)entry.getValue();
            if(name.contains(context.getResources().getString(R.string.contact_favorite))) {
                favoriteContactsMap.put(name, contact);
            }
        }
        return favoriteContactsMap;
    }

    public boolean hasFavoriteContact() {
        return getFavoriteContacts().size() != 0;
    }
}
