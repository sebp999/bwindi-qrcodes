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
import android.util.Log;
import android.view.View;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.preference.PreferenceManager;

import com.sebswebs.myapplication.databinding.ActivityBarcodeIdentifiedBinding;

import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ShowResult extends AppCompatActivity {
    private ActivityBarcodeIdentifiedBinding binding;

    private boolean subscriptionExpired(String startDate, String duration) {
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

    //    public void rescanImages() {
//        MediaScannerConnection.scanFile(this, );
//
//    }
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
                Log.e("xxxx", "No data from bar code ?!");
                return;
            }
        } catch (NullPointerException noDataFromBarcode) {
            Log.e("xxxx", "No data from bar code ?!");
            return;
        }

        String barcodePatientId = (String) barcodeParts.get(0);
        String barcodeMemberName = (String) barcodeParts.get(1);
        String barcodeDate = (String) barcodeParts.get(2);

        PatientDbHelper databaseHelper = new PatientDbHelper(this);
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        String[] cols = {"MemberId", "HouseholdId", "Client_Name", "MemberGender", "MemberDateOfBirth", "CurrentSubscriptionDate", "SubscriptionDuration", "MemberImagePath"};
        Cursor result = database.query("patient", cols, "MemberId='" + barcodePatientId + "'", null, null, null, null);
        Log.e("result", result.getCount() + "");
        try {
            if (result.moveToFirst()) {
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
                    this.binding.SubscriptionExpired.setVisibility(View.VISIBLE);
                } else {
                    this.binding.SubscriptionExpired.setVisibility(View.INVISIBLE);
                }
                //            String imageDir = getImagesDir();
                //            File imgFile = new File(imageDir + "/" +memberImagePath);

                //            GET IMAGE FILE NOW!


                //            Uri collection;
                //            ContentResolver contentResolver = this.getContentResolver();
                //            ArrayList photos = new ArrayList();
                //            Cursor cursor;
                //
                //            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                //                // api 29 and up
                //
                //                cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                //                        new String[]{MediaStore.Images.Media._ID},
                //                        null,
                //                        null,
                //                        MediaStore.Images.Media.DATE_TAKEN + " DESC"
                //                );
                //
                //                if (null == cursor) {
                //                    Log.e("xxxx", String.valueOf(photos));
                //                }
                //
                //                if (cursor.moveToFirst()) {
                //                    do {
                //                        String photoUrl = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))).toString();
                //                        photos.add(photoUrl);
                //
                //                    } while (cursor.moveToNext());
                //                }
                //            } else {
                //
                //                cursor = contentResolver.query(
                //                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                //                        null,
                //                        null,
                //                        null,
                //                        MediaStore.Images.Media.DATA + " DESC"
                //                );
                //
                //                if (null == cursor) {
                //                    Log.e("xxxx", String.valueOf(photos));
                //                }
                //
                //                if (cursor.moveToFirst()) {
                //                    do {
                //                        String fullPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                //                        photos.add(fullPath);
                //                    } while (cursor.moveToNext());
                //                }
                //            }
                //            cursor.close();
                Log.e("permission?", String.valueOf(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)));

                log_images();
                Bitmap image = get_image(memberId);
                this.binding.patientImage.setImageBitmap(image);
            } else { // nothing in database
                this.binding.tvHooray.setText(barcodeMemberName);
                this.binding.patientNumber.setText(barcodePatientId);

                this.binding.CurrentSubscriptionDate.setText("");
                this.binding.MemberGenderId.setText("");
                this.binding.MemberDateOfBirth.setText("");
                this.binding.SubscriptionDuration.setText("");
                this.binding.HouseholdId.setText("");

                this.binding.NotInDatabase.setVisibility(View.VISIBLE);

            }
        } catch (Exception e) {
            Log.e("ShowResult", String.valueOf(e));
        } finally {
            result.close();
            database.close();
        }

        this.binding.btScanAnother.setOnClickListener(new View.OnClickListener() {
            public void onClick(@Nullable View view) {
                Log.e("TAG", "messaged");
                ShowResult.this.switchToMain();
            }
        });
    }

    private Bitmap get_image(String memberId) {
        List<Uri> imgUris = ImageHelper.getImageUris(memberId + ".jpg", this);
        if (imgUris.size() > 0) {
            Uri imgUri = imgUris.get(0);
            try {
                return BitmapFactory.decodeStream(getContentResolver().openInputStream(imgUri));
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    private final void switchToMain() {
        @OptIn(markerClass = ExperimentalGetImage.class) Intent switchActivityIntent = new Intent((Context) this, MainActivity.class);
        this.startActivity(switchActivityIntent);
    }

    private void log_images() {
        ImageHelper.rescanMediaStore(this);
    }
}

