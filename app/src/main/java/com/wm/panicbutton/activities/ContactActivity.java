package com.wm.panicbutton.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.wm.panicbutton.R;
import com.wm.panicbutton.helpers.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactActivity extends AppCompatActivity {

    private static final String TAG = "CONTACT";
    private final int ACTIVITY_REQUEST_CODE = 1;
    private final String KEY_ITEM_CONTACT_LINE_1 = "line_1";
    private final String KEY_ITEM_CONTACT_LINE_2 = "line_2";
    private String ICON_FAVORITE;

    private SharedPreferences contactStorage;
    private SharedPreferences.Editor contactStorageEditor;
    private ListView contactList;
    private Button addContactButton;
    private List<HashMap<String, String>> contactItems;
    private SimpleAdapter contactAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        ICON_FAVORITE = getResources().getString(R.string.contact_favorite);

        // every contact saved will be put in here
        contactStorage = this.getSharedPreferences(Constants.SHARED_PREFERENCES_KEY_CONTACTS, Context.MODE_PRIVATE);
        contactStorageEditor = contactStorage.edit();

        // bring up list of contacts
        contactItems = new ArrayList<>();
        contactAdapter = new SimpleAdapter(this, contactItems, R.layout.item_contact, new String[]{KEY_ITEM_CONTACT_LINE_1, KEY_ITEM_CONTACT_LINE_2}, new int[]{R.id.name, R.id.number});
        contactList = findViewById(R.id.contactList);
        HashMap<String, String> contactsMap = new HashMap<>();
        Map<String, ?> contacts = contactStorage.getAll();
        for (Map.Entry<String, ?> entry : contacts.entrySet()) {
            Log.i(TAG, "name: " + entry.getKey() + ", contact: " + entry.getValue().toString());
            contactsMap.put(KEY_ITEM_CONTACT_LINE_1, entry.getKey());
            contactsMap.put(KEY_ITEM_CONTACT_LINE_2, entry.getValue().toString());
            this.contactItems.add((HashMap)contactsMap.clone());
        }
        Collections.sort(contactItems, mapComparator);
        contactList.setAdapter(contactAdapter);

        // set listener on each contact item
        contactList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> a, View v, final int position, long id) {

                // get name and contact from the clicked item
                final String name = ContactActivity.this.contactItems.get(position).get(KEY_ITEM_CONTACT_LINE_1);
                final String contact = ContactActivity.this.contactItems.get(position).get(KEY_ITEM_CONTACT_LINE_2);

                // bring up Contact Options dialog
                final String favoriteButtonText = name.contains(ICON_FAVORITE)? "Unfavorite":"Favorite!";
                AlertDialog.Builder adb = new AlertDialog.Builder(ContactActivity.this);
                adb.setTitle("Contact Options");
                adb.setMessage("Either delete or make favorite/unfavorite for " + name.replace(ICON_FAVORITE, "") + "'s contact!");
                final int positionToRemove = position;

                // delete button
                adb.setNegativeButton("Delete", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        contactStorageEditor.remove(name);
                        contactStorageEditor.commit();

                        ContactActivity.this.contactItems.remove(positionToRemove);
                        contactAdapter.notifyDataSetChanged();
                    }
                });

                // make favorite/unfavorite button
                adb.setPositiveButton(favoriteButtonText, new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        toggleFavoriteContact(name, contact, positionToRemove);
                    }
                });

                // cancel button
                adb.setNeutralButton("Cancel", null);
                adb.show();
            }
        });

        // initialize add contact button
        addContactButton = findViewById(R.id.addContactButton);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (ACTIVITY_REQUEST_CODE):
                if (resultCode == Activity.RESULT_OK) {

                    // successfully selected contact to add
                    Uri contactData = data.getData();
                    Cursor c = managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {

                        // add item to storage and update contact list
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        String contact = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        HashMap<String, String> contactsMap = new HashMap<>();
                        contactsMap.put(KEY_ITEM_CONTACT_LINE_1, name);
                        contactsMap.put(KEY_ITEM_CONTACT_LINE_2, contact);
                        if (!isContactPresent(name, contact)) {
                            contactItems.add(contactsMap);
                            Collections.sort(contactItems, mapComparator);
                            contactAdapter.notifyDataSetChanged();

                            contactStorageEditor.putString(name, contact);
                            contactStorageEditor.commit();

                            Log.i(TAG, "added - name: " + name + ", contact: " + contact);
                        }
                    }
                }
                break;
        }
    }

    private boolean isContactPresent(String name, String contact) {
        HashMap<String, String> contactsMap = new HashMap<>();
        contactsMap.put(KEY_ITEM_CONTACT_LINE_1, name);
        contactsMap.put(KEY_ITEM_CONTACT_LINE_2, contact);

        HashMap<String, String> favoriteContactsMap = new HashMap<>();
        favoriteContactsMap.put(KEY_ITEM_CONTACT_LINE_1, name + ICON_FAVORITE);
        favoriteContactsMap.put(KEY_ITEM_CONTACT_LINE_2, contact);

        return contactItems.contains(contactsMap) || contactItems.contains(favoriteContactsMap);
    }

    // mark/unmark favorite icon
    private void toggleFavoriteContact(String name, String contact, int positionToRemove) {
        contactStorageEditor.remove(name);
        contactStorageEditor.commit();
        ContactActivity.this.contactItems.remove(positionToRemove);

        if(!name.contains(ICON_FAVORITE)) {
            name = name + ICON_FAVORITE;
        } else {
            name = name.replace(ICON_FAVORITE, "");
        }

        contactStorageEditor.putString(name, contact);
        contactStorageEditor.commit();

        HashMap<String, String> contactsMap = new HashMap<>();
        contactsMap.put(KEY_ITEM_CONTACT_LINE_1, name);
        contactsMap.put(KEY_ITEM_CONTACT_LINE_2, contact);
        ContactActivity.this.contactItems.add(positionToRemove, contactsMap);

        contactAdapter.notifyDataSetChanged();
    }

    private Comparator<Map<String, String>> mapComparator = new Comparator<Map<String, String>>() {
        public int compare(Map<String, String> m1, Map<String, String> m2) {
            return m1.get(KEY_ITEM_CONTACT_LINE_1).compareTo(m2.get(KEY_ITEM_CONTACT_LINE_1));
        }
    };
}
