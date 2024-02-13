package com.sebswebs.myapplication;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.ExperimentalGetImage;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.sebswebs.myapplication.databinding.ActivityBarcodeIdentifiedBinding;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import androidx.appcompat.widget.Toolbar;

public final class ShowResult extends BaseMenus {
    private ActivityBarcodeIdentifiedBinding binding;

    private boolean subscriptionExpired(String startDate, String duration) {
        LocalDate date = LocalDate.parse(startDate);
        date = date.plusMonths(Integer.parseInt(duration)).minusDays(1);
        return date.isBefore(LocalDate.now());
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        this.binding = ActivityBarcodeIdentifiedBinding.inflate(this.getLayoutInflater());

        this.setContentView((View) binding.getRoot());
        String barcodeInfo = this.getIntent().getStringExtra("barcodeValue");
//        Id, name, dob
        List<String> barcodeParts = new ArrayList<String>();
        try {
            if (barcodeInfo != null) {
                barcodeParts = Arrays.asList(barcodeInfo.split(","));
            } else {
                return;
            }
        } catch (NullPointerException noDataFromBarcode) {
            return;
        }

        String barcodePatientId = (String) barcodeParts.get(0);
        String barcodeMemberName = (String) barcodeParts.get(1);
        String barcodeDate = (String) barcodeParts.get(2);

        PatientDbHelper databaseHelper = new PatientDbHelper(this);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String[] cols = {"MemberId", "HouseholdId", "Client_Name", "MemberGender", "MemberDateOfBirth", "CurrentSubscriptionDate", "SubscriptionDuration", "MemberImagePath"};
        Cursor result = database.query("patient", cols, "MemberId='" + barcodePatientId + "'", null, null, null, "SubscriptionDuration DESC");
        try {
            if (result.moveToFirst()) {
                String memberId = result.getString(0);
                int householdIdInt = result.getInt(1);
                String householdId = householdIdInt + "";
                String clientName = result.getString(2);
                String memberGender = result.getString(3);
                String memberDateOfBirth = result.getString(4);
                String currentSubscriptionDate = result.getString(5);
                String subscriptionDuration = (result.getInt(6) + "");
                String memberImagePath = result.getString(7);

                this.binding.memberName.setText((CharSequence) clientName);
                this.binding.patientNumber.setText((CharSequence) memberId);
                this.binding.HouseholdId.setText((CharSequence) householdId);
                this.binding.MemberGenderId.setText((CharSequence) memberGender);
                this.binding.MemberDateOfBirth.setText((CharSequence) memberDateOfBirth);
                this.binding.CurrentSubscriptionDate.setText((CharSequence) currentSubscriptionDate);
                this.binding.SubscriptionDuration.setText((CharSequence) subscriptionDuration);
                if (subscriptionExpired(currentSubscriptionDate, subscriptionDuration)) {
                    this.binding.SubscriptionExpired.setVisibility(View.VISIBLE);
                } else {
                    this.binding.SubscriptionExpired.setVisibility(View.INVISIBLE);
                }

                Bitmap image = get_image(memberId);
                this.binding.patientImage.setImageBitmap(image);
            } else { // nothing in database
                this.binding.memberName.setText(barcodeMemberName);
                this.binding.patientNumber.setText(barcodePatientId);

                this.binding.CurrentSubscriptionDate.setText("");
                this.binding.MemberGenderId.setText("");
                this.binding.MemberDateOfBirth.setText("");
                this.binding.SubscriptionDuration.setText("");
                this.binding.HouseholdId.setText("");

                this.binding.NotInDatabase.setVisibility(View.VISIBLE);

            }
        } catch (Exception e) {
        } finally {
            result.close();
            database.close();
        }

        this.binding.btScanAnother.setOnClickListener(new View.OnClickListener() {
            public void onClick(@Nullable View view) {
                ShowResult.this.switchToMain();
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private Bitmap get_image(String memberId) {

        File primaryExternalStorage = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File appImagesDir = new File(primaryExternalStorage, "members");
        File image = new File(appImagesDir, memberId+".jpg");
        try {
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(Uri.fromFile(image)));
        } catch (IOException noSuchFile){
            return null;
        }
    }

    private final void switchToMain() {
        @OptIn(markerClass = ExperimentalGetImage.class) Intent switchActivityIntent = new Intent((Context) this, MainActivity.class);
        this.startActivity(switchActivityIntent);
    }
}

