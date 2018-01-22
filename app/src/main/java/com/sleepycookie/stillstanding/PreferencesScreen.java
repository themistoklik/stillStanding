package com.sleepycookie.stillstanding;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sleepycookie.stillstanding.data.StillStandingPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the activity that gets launched when a user goes to pick a contact.
 * It inflates the proper xml UI and also handles the logic behind picking and saving
 * contact.
 */

public class PreferencesScreen extends AppCompatActivity {

    Button phoneContactsButtton;
    TextView emergencyNumber;
    TextView emergencyContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        //TODO in activity_preferences: Refine the UI.

        phoneContactsButtton = (Button) findViewById(R.id.set_contact);
        emergencyContact = (TextView) findViewById(R.id.contact_name);
        emergencyNumber = (TextView) findViewById(R.id.contact_phone);


        /**
         * Show the saved preferences or the placeholder text if there is nothing saved.
         */

        SharedPreferences sharedPrefName = getSharedPreferences("PREF_NAME", Context.MODE_PRIVATE);
        String mName = sharedPrefName.getString(getString(R.string.emergency_name), null);
        SharedPreferences sharedPrefPhone = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
        String mNumber = sharedPrefPhone.getString(getString(R.string.emergency_number), null);

        if(mName != null) emergencyContact.setText(mName);
        if(mNumber != null) emergencyNumber.setText(mNumber);

        phoneContactsButtton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // The below two line is needed to open the contact list of  mobile
                Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(contactPickerIntent,1);

            }
        });
    }


    /**
     * Method triggered after clicking "Pick a contact". It shows the user the contact list UI,
     * depending on their phone, and they can pick a desired contact. After that there is a dialog
     * that lets them pick one of the phone numbers from that contact, which is then saved for
     * future use. The dialog is shown even if there is only one number stored.
     *
     * Issues: - contacts with no phone number are shown in the list
     *         - contacts almost always show the same number more than once (probably viber's fault)
     *
     * This piece of code was found in parts on stackoverflow.
     *
     * @param reqCode
     * @param resultCode
     * @param data
     */

    //TODO Show unique numbers in the dialog pop-up

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (1) :
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c =  getContentResolver().query(contactData, null, null, null, null);
                    if (c.moveToFirst()) {
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String no = "";

                        Cursor phoneCur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[] { id }, null);

                        String phoneNumber = "";
                        List<String> allNumbers = new ArrayList<String>();
                        int phoneIdx = 0;

                        while (phoneCur.moveToNext()) {
                            no = phoneCur.getString(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            int type = phoneCur.getInt(phoneCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                            switch (type) {
                                case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                    // do something with the Home number here...
                                    Log.v("Home", name + ": " + no);
                                    allNumbers.add(no);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                    // do something with the Mobile number here...
                                    Log.v("Mobile", name + ": " + no);
                                    allNumbers.add(no);
                                    break;
                                case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                                    // do something with the Work number here...
                                    Log.v("Work", name + ": " + no);
                                    allNumbers.add(no);
                                    break;
                            }
                        }

                        StillStandingPreferences.setSafetyContactName(name);


                        final CharSequence[] items = allNumbers.toArray(new String[allNumbers.size()]);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Choose a number");
                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                String selectedNumber = items[item].toString();
                                selectedNumber = selectedNumber.replace("-", "");
                                Log.v("Selected Number:", selectedNumber);

                                SharedPreferences sharedPrefName = getSharedPreferences("PREF_NAME", Context.MODE_PRIVATE);
                                SharedPreferences sharedPrefPhone = getSharedPreferences("PREF_PHONE", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editorN = sharedPrefName.edit();
                                SharedPreferences.Editor editorP = sharedPrefPhone.edit();
                                editorP.putString(getString(R.string.emergency_number), selectedNumber);
                                editorP.commit();
                                editorN.putString(getString(R.string.emergency_name), StillStandingPreferences.getSafetyContactName());
                                editorN.commit();
                                emergencyNumber.setText(selectedNumber);
                                emergencyContact.setText(StillStandingPreferences.getSafetyContactName());
                            }
                        });
                        AlertDialog alert = builder.create();
                        if(allNumbers.size() > 0) {
                            alert.show();
                        } else {
                            String selectedNumber = phoneNumber.toString();
                            selectedNumber = selectedNumber.replace("-", "");
                            Log.e("Sel:", selectedNumber);
                        }

                        if (phoneNumber.length() == 0) {
                            //no numbers found actions
                            Log.v("Phone Number", "None");
                        }
                    }
                }
                break;
        }
    }
}
