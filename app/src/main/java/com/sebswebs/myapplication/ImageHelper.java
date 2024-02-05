package com.sebswebs.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ImageHelper {

    private static final String TAG = "ImageHelper";

    public static void rescanMediaStore(Context context) {
        // Specify the file or directory to be scanned (for all images, you can use Environment.getExternalStorageDirectory())
        String filePath = Environment.getExternalStorageDirectory().toString();

        // Trigger a media scan for the specified file or directory
        MediaScannerConnection.scanFile(
                context,
                new String[]{filePath},
                null,
                (path, uri) -> {
                    // Media scan completed, you can log the latest images here
                    if (uri != null) {
                        Log.d(TAG, "Media scan completed. Updated URI: " + uri.toString());
                        // Log the latest images using your previous logic

                    } else {
                        Log.e(TAG, "Media scan failed for path: " + path);
                    }
                    logAllImages(context);
                }
        );
    }

    private static void logAllImages(Context context) {

        // Define the columns you want to retrieve from the MediaStore.Images database
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DISPLAY_NAME };  //, MediaStore.Images.Media.INTERNAL_CONTENT_URI, MediaStore.Images.Media.EXTERNAL_CONTENT_URI};

        // Get the ContentResolver
        ContentResolver contentResolver = context.getContentResolver();

        // Construct the URI for querying images
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        // Perform the query
        Cursor cursor = contentResolver.query(imageUri, projection, null, null, null);

        if (cursor != null) {
            try {
                // Iterate through the cursor to log information about each image
                while (cursor.moveToNext()) {
                    @SuppressLint("Range") long imageId = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                    @SuppressLint("Range") String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    @SuppressLint("Range") String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));

                    // Log the information
                    Log.d(TAG, "Image ID: " + imageId);
                    Log.d(TAG, "Image Path: " + imagePath);
                    Log.d(TAG, "Display Name: " + displayName);
                    Uri imgUri = Uri.parse(imagePath);
                    Log.d(TAG, imgUri.toString());
                    Log.d(TAG, "------------------------");
                }
            } finally {
                // Close the cursor to release resources
                cursor.close();
            }
        }
    }

    public static List<Uri> getImageUris(String memberId, Context context) {
        String[] projection = {MediaStore.Images.Media.DATA};
        ContentResolver contentResolver = context.getContentResolver();
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "=?";
        String[] selectionArgs = {memberId};
        // Perform the query
        Cursor cursor = contentResolver.query(imageUri, projection, selection, selectionArgs, null);
        List<Uri> uris = new ArrayList<Uri>();
        try {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Uri imgUri = Uri.fromFile(new java.io.File(imagePath));
                uris.add(imgUri);
            }

        } finally {
            cursor.close();
        }
        return uris;
    }
}