package com.sebswebs.myapplication;

import static com.sebswebs.myapplication.PatientDbEntries.SQL_DELETE_ENTRIES;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class SyncData extends BaseMenus {
    private static final String FILENAME = "last_updated";
    private TextView resultsTextView;
    private Button fullSyncButton;
    private Button cancelUpdate;
    private Button updateSyncButton;
    private int myProgressNum = 0;

    private ProgressBar myProgressBar;
    private TextView myProgressText;
    private UpdateThread myUpdateThread;
    private Handler handler = new Handler();

    private boolean addPatientToDb(JSONObject some_patient) {
        try {
            PatientDbHelper dbHelper = new PatientDbHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(PatientDbEntries.PatientEntry.CLIENT_NAME_COLUMN, some_patient.getString("Client_Name"));
            values.put(PatientDbEntries.PatientEntry.MEMBER_ID_COLUMN, some_patient.getString("MemberId"));
            values.put(PatientDbEntries.PatientEntry.HOUSEHOLD_ID_COLUMN, some_patient.getString("HouseholdId"));
            values.put(PatientDbEntries.PatientEntry.MEMBER_GENDER_COLUMN, some_patient.getString("MemberGender"));
            values.put(PatientDbEntries.PatientEntry.MEMBER_DATE_OF_BIRTH_COLUMN, some_patient.getString("MemberDateOfBirth"));
            values.put(PatientDbEntries.PatientEntry.CURRENT_SUBSCRIPTION_DATE_COLUMN, some_patient.getString("CurrentSubscriptionDate"));
            values.put(PatientDbEntries.PatientEntry.SUBSCRIPTION_DURATION_COLUMN, some_patient.getString("SubscriptionDuration"));
            values.put(PatientDbEntries.PatientEntry.MEMBER_IMAGE_PATH, some_patient.getString("MemberImagePath"));
            long newRowId = db.insert(PatientDbEntries.PatientEntry.TABLE_NAME, null, values);
            db.close();
            return true;
        } catch (Exception e) {
            Log.e("argh", e.toString());
        }
        return false;
    }

    private boolean recreateDatabase() {
        // Remove database and start again
        Log.e("BarcodeScanner", "Deleting data from database");
        PatientDbHelper dbHelper = new PatientDbHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ENTRIES);
        Log.e("BarcodeScanner", "Finished deleting data from database");
        return true;
    }

    private String getPref(String key) {
        SharedPreferences preferences = (SharedPreferences) PreferenceManager.getDefaultSharedPreferences(this);
        String val = preferences.getString(key, null);
        Log.e("preference", "key: "+ key + "val: "+ val);
        return val;
    }

    private String getImageServerURL() {
        return getPref("imageServerURL");
    }
    private String getPatientDatabaseURL(){
        return getPref("patientDatabaseURL");
    }

    private class UpdateThread extends Thread {
        public boolean cancel = false;
        // A thread that updates the database
        private String myDatabaseURL = null;
        private boolean thisIsUpdate = false;
        String TAG = "jjjjj";
        public UpdateThread(String dbUrl, boolean isUpdate){
            // constructor
            myDatabaseURL = dbUrl;
            thisIsUpdate = isUpdate;
        }
        private void showMessage(String s) {
            final Snackbar snack = Snackbar.make(findViewById(R.id.updatedText), (CharSequence) s, Snackbar.LENGTH_INDEFINITE);
            snack.setAction(
                "ok",
                new View.OnClickListener(){
                    @Override public void onClick(View view){
                        Log.e(TAG, "CLICKED");
                        snack.dismiss();
                    }
                }).show();
        }
        @Override
        public void run() {

            if (thisIsUpdate){
                try {
                    Context context = SyncData.this;
                    FileInputStream fis = context.openFileInput(FILENAME);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                    String line = reader.readLine();
                    String justDate = line.substring(0,10);
                    myDatabaseURL = myDatabaseURL+"?since="+justDate;

                    Log.e(TAG, "this is an update so I'm using url "+ myDatabaseURL);

                } catch (FileNotFoundException | NullPointerException e) {
                    //This is the first time it has been run, so report that you have to do a full sync first.
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "no date file found");
                            showMessage("Please run a full sync before updating");
                        }
                    });
                    return;
                } catch (IOException ioe){
                    Log.e(TAG, "ioexecption on open file");
                    showMessage("Unexpected error getting last-updated date");
                }
            }
            String feedContents = null;
            try {
                Log.e("argh", "Reading feed contents");
                feedContents = readFeedContents();
                Log.e("argh", "Read feed contents");

            } catch (InterruptedException e) {
                Log.e(TAG, "interruptedexception");
                showMessage("Unexpected error getting data: try again");
            } catch (MalformedURLException m) {
                Log.e(TAG, "malformed");
                showMessage("Couldn't connect to "+myDatabaseURL+ ", check URL in settings");
            } catch (IOException io) {
                Log.e(TAG, "IOException");
                showMessage("Can't get data from "+myDatabaseURL+ ", check URL in settings");
            }

            Log.e(TAG, "Adding data to database from API " + Instant.now().toString());

            if (feedContents!=null) {
                try {
                    JSONArray patients = new JSONArray(feedContents);
                    if (!thisIsUpdate && patients.length() > 0) {
                        recreateDatabase();
                    }
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            myProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                            myProgressBar.setMax(patients.length());
                            myProgressText = (TextView) findViewById(R.id.updatedText);
                            myProgressText.setText("0/" + myProgressBar.getMax());
                        }
                    });
                    int num_patients = patients.length();
                    Log.e(TAG, "Adding "+num_patients+" members to DB");
                    for (int i = 0; i < num_patients; i++) {
                        if (!cancel) {
                            JSONObject patient = patients.getJSONObject(i);
                            addPatientToDb(patient);
                            if (thisIsUpdate) {
                                try {
                                    downloadPatientImg(patient);
                                } catch (IOException failed) {
                                    String error = "Unable to find image on server " + patient.getString("MemberId") + ".jpg";
                                    showMessage(error);
                                } catch (JSONException jsonException){
                                    showMessage("Record for Patient " +patient.getString("MemberId")+ " corrupted in feed");
                                }
                            }
                            if (i % 100 == 0) {
                                Log.e(TAG, "updating");
                                myProgressNum = i;
                                update_progress_display(myProgressNum, myProgressBar);
                            }
                        } else {
                            Log.e(TAG, "Cancelled: breaking");
                            break;
                        }
                    }

                    myProgressBar = (ProgressBar) findViewById(R.id.progressBar);
                    update_progress_display(myProgressBar.getMax(), myProgressBar); // final
                    if (num_patients == 0){
                        update_progress_display(0, myProgressBar);
                        showMessage("No new members added since last update");
                    } else {
                        update_progress_display(myProgressBar.getMax(), myProgressBar);
                    }
                    if (!cancel) {
                        String filename = "last_updated";
                        String fileContents = Instant.now().toString();
                        Context context = SyncData.this;
                        try {
                            FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
                            fos.write(fileContents.getBytes());
                            FileInputStream fis = context.openFileInput(filename);
                            String contents = String.valueOf(fis.read());
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        showMessage("User cancelled");
                    }
                } catch (JSONException e) {
                    showMessage("Can't read the database feed: check the URL in settings");
                } catch (IOException e) {
                    showMessage("Update succeeded but unable to save the last-updated date");
                }
            }
        }

        private void downloadPatientImg(JSONObject patient) throws JSONException, IOException {
            Log.e("argh", "about to get image");
            int count;
            String patientId = patient.getString("MemberId");
            URL imgDownloadUrl = new URL(getImageServerURL() + patientId + ".jpg");
            InputStream input = new BufferedInputStream(imgDownloadUrl.openStream(), 8192);

            OutputStream outputStream;

            File primaryExternalStorage = SyncData.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File appImagesDir = new File(primaryExternalStorage, "members");
            File image = new File(appImagesDir, patientId + ".jpg");

            outputStream = Files.newOutputStream(image.toPath());

            byte[] data = new byte[1024];

            long total = 0;

            while ((count = input.read(data)) != -1) {
                total += count;
                outputStream.write(data, 0, count);
            }
            outputStream.flush();
            outputStream.close();
        }

        private void update_progress_display(int progressNum, ProgressBar progressBar) {
            handler.post(new Runnable() {
                public void run() {
                    Log.e(TAG, (progressNum + 1) + " records added to database" + Instant.now().toString());
                    myProgressBar.setProgress(progressNum);
                    myProgressText.setText(progressNum + "/" + progressBar.getMax());
                }
            });
        }

        private String readFeedContents() throws MalformedURLException, IOException, InterruptedException {

            StringBuilder feedContents = new StringBuilder();
            HttpURLConnection urlConnection = null;
            try {
                urlConnection = (HttpURLConnection) new URL(myDatabaseURL).openConnection();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    feedContents.append(line);
                }
                Log.e("BarcodeScanner", "Finished reading data from API " + new Date());
            }  finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return feedContents.toString();
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        Log.e("DEBUGX", "onCreate");
        super.onCreate(savedInstanceState);
        Log.e("xxxx", String.valueOf( getExternalFilesDir(null)));
        Log.e("xxxx", String.valueOf(getExternalFilesDir(Environment.DIRECTORY_PICTURES)));
        setContentView(R.layout.activity_read_database);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        resultsTextView = (TextView) findViewById(R.id.textView);
        fullSyncButton = (Button) findViewById(R.id.displayData);
        cancelUpdate = (Button) findViewById(R.id.cancelUpdate);
        String db_url = getPatientDatabaseURL();


        fullSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myUpdateThread = new UpdateThread(db_url, false);
                myUpdateThread.start();
            }
        });
        cancelUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myUpdateThread.cancel = true;
            }
        });

        updateSyncButton = (Button) findViewById(R.id.updateSync);
        updateSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)  {
                myUpdateThread = new UpdateThread(db_url, true);
                myUpdateThread.start();
            }
        });
    }
}
