package com.example.foody;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserData.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "user_data";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_ITEM = "item";
    private static final String COLUMN_CALORIES = "calories";
    private static final String COLUMN_CO2 = "co2";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_DATE + " TEXT, "
                + COLUMN_ITEM + " TEXT, "
                + COLUMN_CALORIES + " REAL, "
                + COLUMN_CO2 + " REAL)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addUserData(String date, String item, float calories, float co2) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_ITEM, item);
        values.put(COLUMN_CALORIES, calories);
        values.put(COLUMN_CO2, co2);
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public Cursor getDataByDate(String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_DATE + " = ?", new String[]{date});
    }

    public Cursor getMonthlyData() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT " + COLUMN_DATE + ", SUM(" + COLUMN_CALORIES + ") as total_calories, SUM(" + COLUMN_CO2 + ") as total_co2 "
                + "FROM " + TABLE_NAME + " GROUP BY " + COLUMN_DATE, null);
    }
}

