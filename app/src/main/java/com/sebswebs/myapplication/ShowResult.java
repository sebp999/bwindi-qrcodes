package com.sebswebs.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.sebswebs.myapplication.databinding.ActivityBarcodeIdentifiedBinding;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class ShowResult extends AppCompatActivity {
    private ActivityBarcodeIdentifiedBinding binding;
    private boolean subscriptionExpired(String startDate, String duration){
        LocalDate date = LocalDate.parse(startDate);
        long numDays = Long.parseLong(duration);
        LocalDate now = LocalDate.now();
        return date.plusDays(numDays).isBefore(now);
    }

    private String getImagesDir() {
        SharedPreferences preferences = (SharedPreferences) PreferenceManager.getDefaultSharedPreferences(this);
        String imageDir = preferences.getString("deviceImageDir", null);
        return imageDir;
    }

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        String s = getImagesDir();
        Log.e("TaG", getImagesDir()); // /data/user/0/com.sebswebs.barcodescanner/files
        super.onCreate(savedInstanceState);
        this.binding = ActivityBarcodeIdentifiedBinding.inflate(this.getLayoutInflater());

        this.setContentView((View)binding.getRoot());
        String barcodeInfo = this.getIntent().getStringExtra("barcodeValue");
//        Id, name, dob
        List barcodeParts = Arrays.asList(barcodeInfo.split(","));
        String patientId = (String)barcodeParts.get(0);

        PatientDbHelper databaseHelper = new PatientDbHelper(this);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String[] cols = {"MemberId", "HouseholdId", "Client_Name", "MemberGender", "MemberDateOfBirth", "CurrentSubscriptionDate", "SubscriptionDuration", "MemberImagePath"};
        Cursor result = database.query("patient", cols, "MemberId='"+patientId+"'", null, null, null, null);
        Log.e("result", result.getCount()+"");
        try {
            result.moveToFirst();
            String memberId = result.getString(0);
            Log.e("memberId", memberId);
            int householdIdInt = result.getInt(1);
            String householdId = householdIdInt + "";
            String clientName = result.getString(2);
            String memberGender = result.getString(3);
            String memberDateOfBirth = result.getString(4);
            String currentSubscriptionDate = result.getString(5);
            String subscriptionDuration = (result.getInt(6) + "");
            String memberImagePath = result.getString(7);

            this.binding.tvHooray.setText((CharSequence) clientName);
            this.binding.patientNumber.setText((CharSequence) memberId);
            this.binding.HouseholdId.setText((CharSequence) householdId);
            this.binding.MemberGenderId.setText((CharSequence) memberGender);
            this.binding.MemberDateOfBirth.setText((CharSequence) memberDateOfBirth);
            this.binding.CurrentSubscriptionDate.setText((CharSequence) currentSubscriptionDate);
            this.binding.SubscriptionDuration.setText((CharSequence) subscriptionDuration);
            if (subscriptionExpired(currentSubscriptionDate, subscriptionDuration)) {
                this.binding.Messages.setVisibility(View.VISIBLE);
            } else {
                this.binding.Messages.setVisibility(View.INVISIBLE);
            }
            String imageDir = getImagesDir();
            File imgFile = new File(imageDir + "/" +memberImagePath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                binding.patientImage.setImageBitmap(myBitmap);
            } else {
                Log.e("aargh", "No image" + imageDir + "/patient_images/" + memberImagePath);
            }
        }
        catch (android.database.CursorIndexOutOfBoundsException noMemberInDB) {
            Log.e("error", noMemberInDB.toString());
            Log.e("barcodeParts", barcodeParts.toString());
//            Intent switchActivityIntent = new Intent((Context)this, NoSuchUser.class);
//            switchActivityIntent.putExtra("barcodeValue", barcodeInfo);
//            this.startActivity(switchActivityIntent);
        }


        this.binding.btScanAnother.setOnClickListener(new View.OnClickListener() {
            public void onClick(@Nullable View view) {Log.e("TAG", "messaged"); ShowResult.this.switchToMain();
            }
        });
    }

    private final void switchToMain() {
        Intent switchActivityIntent = new Intent((Context)this, MainActivity.class);
        this.startActivity(switchActivityIntent);
    }
}
