package com.sebswebs.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class PatientDbEntries {
    private PatientDbEntries() {}

    public static class PatientEntry implements BaseColumns{
        public static final String TABLE_NAME = "patient";

        public static final String MEMBER_ID_COLUMN = "MemberId";
        public static final String HOUSEHOLD_ID_COLUMN = "HouseholdId";
        public static final String CLIENT_NAME_COLUMN = "Client_Name";
        public static final String MEMBER_GENDER_COLUMN = "MemberGender";
        public static final String MEMBER_DATE_OF_BIRTH_COLUMN = "MemberDateOfBirth";
        public static final String CURRENT_SUBSCRIPTION_DATE_COLUMN = "CurrentSubscriptionDate";
        public static final String SUBSCRIPTION_DURATION_COLUMN = "SubscriptionDuration";
        public static final String MEMBER_IMAGE_PATH = "MemberImagePath";
    }
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PatientEntry.TABLE_NAME + " (" +
                    PatientEntry.MEMBER_ID_COLUMN + " TEXT PRIMARY KEY," +
                    PatientEntry.HOUSEHOLD_ID_COLUMN + " TEXT NOT NULL," +
                    PatientEntry.CLIENT_NAME_COLUMN + " TEXT NOT NULL," +
                    PatientEntry.MEMBER_GENDER_COLUMN + " TEXT NOT NULL," +
                    PatientEntry.MEMBER_DATE_OF_BIRTH_COLUMN + " TEXT NOT NULL," +
                    PatientEntry.CURRENT_SUBSCRIPTION_DATE_COLUMN + " TEXT NOT NULL, " +
                    PatientEntry.SUBSCRIPTION_DURATION_COLUMN + " TEXT NOT NULL,  " +
                    PatientEntry.MEMBER_IMAGE_PATH + " TEXT NOT NULL)"
            ;
    public static final String SQL_DELETE_ENTRIES =
            "DELETE FROM " + PatientEntry.TABLE_NAME + " WHERE 1=1";
}
