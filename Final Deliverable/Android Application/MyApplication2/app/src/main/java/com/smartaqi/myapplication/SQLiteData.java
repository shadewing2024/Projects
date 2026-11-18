package com.smartaqi.myapplication;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class SQLiteData extends SQLiteOpenHelper{

    public static final String Database_name = "Smart_AQI";

    // For Users Table
    public static final String Table_name = "Users";
    public static final String col_FireAuthUID = "fireAuthUID";
    public static final String col_UserId = "id";
    public static final String col_UserNamed = "name";
    public static final String col_UserEmail = "email";
    public static final String col_UserPhone = "phone";

    // For AQI History
    public static final String Table_AQI = "AQI_History";
    public static final String col_AQI_ID = "aqiID";
    public static final String col_City = "city";
    public static final String col_AQI = "aqi";
    public static final String col_Temprature = "temprature";
    public static final String col_Humidity = "humidity";
    public static final String col_Wind = "wind";
    public static final String col_Time = "timestamp";
    public static final String col_FormattedTime = "formattedTime";

    // For Advice
    public static final String col_Color = "color";
    public static final String col_Category = "category";
    public static final String col_Impact = "impact";
    public static final String col_Action = "re_Action";
    public static final String col_Min = "mini";
    public static final String col_Max = "maxi";
    public static final String Table_Future = "Future_AQI";
    public static final String col_FutureID = "futureID";
    public static final String col_Hour = "hour";
    public static final String col_PredictedAQI = "predictedAQI";

    android.database.sqlite.SQLiteDatabase db;
    FutureAQIPredictor predictor =
            new FutureAQIPredictor();

    public SQLiteData(@Nullable Context context) {

        super(context, Database_name, null, 4);
        db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(android.database.sqlite.SQLiteDatabase db) {

        db.execSQL("create table " + Table_name +" (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "fireAuthUID TEXT UNIQUE, " +
                "name TEXT, " +
                "email TEXT, " +
                "phone TEXT)");

        db.execSQL("create table " + Table_AQI +"(" +
                "aqiID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "city TEXT," +
                "aqi REAL," +
                "temprature REAL," +
                "humidity REAL," +
                "wind REAL," +
                "timestamp INTEGER," +
                "formattedTime TEXT," +
                "color TEXT," +
                "category TEXT," +
                "impact TEXT," +
                "re_Action TEXT," +
                "mini INTEGER," +
                "maxi INTEGER)");

        db.execSQL("create table Future_AQI (" +
                "futureID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "hour INTEGER," +
                "predictedAQI REAL," +
                "city TEXT," +
                "timestamp INTEGER)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + Table_name);
        db.execSQL("DROP TABLE IF EXISTS " + Table_AQI);
        db.execSQL("DROP TABLE IF EXISTS " + Table_Future);

        onCreate(db);
    }

    // Inside SQLiteData.java

    // 1. Define the interface at the top of the class (under the fields)
    public interface OnSyncCompleteListener {
        void onSyncSuccess();
    }

    public void startLiveSync(String city, OnSyncCompleteListener listener) {
        FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

        fireDB.collection("AQI").document(city).collection("history")
                .orderBy("timstamp", Query.Direction.DESCENDING)
                .addSnapshotListener((query, e) -> {
                    if (e != null || query == null) return;

                    fireDB.collection("AQI_Collection").document("AQI_Doc").collection("Catalog").get()
                            .addOnSuccessListener(rangeSnapshot -> {

                                new Thread(() -> {
                                    SQLiteDatabase sqlDb = this.getWritableDatabase();
                                    sqlDb.beginTransaction();
                                    try {
                                        // Clear current local data to prevent duplicates
                                        sqlDb.delete(Table_AQI, null, null);
                                        sqlDb.delete("Future_AQI", null, null);

                                        List<DocumentSnapshot> historyDocs = query.getDocuments();
                                        if (historyDocs.isEmpty()) return;

                                        // 1. Prediction Logic for the Latest Record
                                        DocumentSnapshot latest = historyDocs.get(0);
                                        int latAqi = latest.getLong("aqi").intValue();
                                        float latTemp = latest.getDouble("temp").floatValue();
                                        float latHum = latest.getDouble("humidity").floatValue();
                                        float latWind = latest.getDouble("wind").floatValue();

                                        List<FuturePredictionModel> predictions = predictor.predictFutureAQI(latAqi, latTemp, latHum, latWind);

                                        for (FuturePredictionModel model : predictions) {
                                            ContentValues fcv = new ContentValues();
                                            fcv.put(col_Hour, model.getHour());
                                            fcv.put(col_PredictedAQI, model.getPredictedAQI());
                                            fcv.put(col_City, city);
                                            sqlDb.insert("Future_AQI", null, fcv);
                                        }

                                        // 2. History Sync Loop
                                        for (DocumentSnapshot doc : historyDocs) {
                                            int aqiValue = doc.getLong("aqi").intValue();
                                            ContentValues cv = new ContentValues();

                                            cv.put(col_City, city);
                                            cv.put(col_AQI, aqiValue);
                                            cv.put(col_Temprature, doc.getDouble("temp"));
                                            cv.put(col_Humidity, doc.getDouble("humidity"));
                                            cv.put(col_Wind, doc.getDouble("wind"));
                                            cv.put(col_Time, doc.getLong("timstamp"));
                                            cv.put(col_FormattedTime, doc.getString("formattedTime"));

                                            // Match AQI Range from Catalog
                                            for (DocumentSnapshot range : rangeSnapshot.getDocuments()) {
                                                int min = range.getLong("minimum").intValue();
                                                int max = range.getLong("maximum").intValue();

                                                if (aqiValue >= min && aqiValue <= max) {
                                                    cv.put(col_Color, range.getString("color"));
                                                    cv.put(col_Category, range.getString("category"));
                                                    cv.put(col_Impact, range.getString("health_impact"));
                                                    cv.put(col_Action, range.getString("recommendation"));
                                                    break;
                                                }
                                            }
                                            sqlDb.insert(Table_AQI, null, cv);
                                        }

                                        sqlDb.setTransactionSuccessful();
                                        Log.d("SYNC", "Database Updated Successfully");
                                    } catch (Exception ex) {
                                        Log.e("SYNC", "Sync Error: " + ex.getMessage());
                                    } finally {
                                        sqlDb.endTransaction();
                                    }

                                    // 3. Notify UI thread
                                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                        if (listener != null) listener.onSyncSuccess();
                                    });
                                }).start();
                            });
                });
    }

    // Insert Data in SQLite
    public void insertAndUpdateUser(String uid, String name, String email, String phone) {

        Cursor cursor = fetchUser(uid);

        ContentValues cv = new ContentValues();
        cv.put(col_FireAuthUID, uid);
        cv.put(col_UserNamed, name);
        cv.put(col_UserEmail, email);
        cv.put(col_UserPhone, phone);

        if (cursor != null && cursor.moveToFirst()) {

            db.update(Table_name, cv, "fireAuthUID = ?", new String[]{uid});

        } else {

            db.insert(Table_name, null, cv);
        }
    }

    // Fetch User Data from SQLite
    public Cursor fetchUser(String uid) {

        return db.rawQuery(
                "SELECT * FROM " + Table_name + " WHERE fireAuthUID = ?",
                new String[]{uid}
        );
    }

    // Update Data in SQLIte
    public boolean update(String id, String name, String phone) {

        ContentValues cv = new ContentValues();
        cv.put(col_UserId, id);
        cv.put(col_UserNamed, name);
        cv.put(col_UserPhone, phone);

        db.update(Table_name, cv, "id = ?", new  String[] {id});

        return true;
    }

    public boolean recordExists(Long timestamp) {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + Table_AQI + " WHERE " + col_Time + " = ?",
                new String[]{String.valueOf(timestamp)}
        );

        boolean exists = (cursor != null && cursor.moveToFirst());

        if (cursor != null) {
            cursor.close();
        }

        return exists;
    }

    public void insertAQI(String city) {
        FirebaseFirestore fireDB = FirebaseFirestore.getInstance();

        // STEP 1: Fetch both collections (Ranges and History)
        fireDB.collection("AQI_Collection").document("AQI_Doc").collection("Catalog").get()
                .addOnSuccessListener(rangeSnapshot -> {

                    fireDB.collection("AQI").document(city).collection("history")
                            .orderBy("timstamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(query -> {

                                // Run heavy DB operations in a background thread to keep UI fast
                                new Thread(() -> {
                                    db.beginTransaction();
                                    try {
                                        // Clear tables once at the start
                                        db.delete(Table_AQI, null, null);
                                        db.delete(Table_Future, null, null);

                                        List<DocumentSnapshot> historyDocs = query.getDocuments();
                                        if (historyDocs.isEmpty()) return;

                                        // ✅ SPEED FIX 1: Predict future ONLY for the LATEST record
                                        // This replaces the nested loop that was slowing everything down
                                        DocumentSnapshot latest = historyDocs.get(0);
                                        double latAqi = latest.getDouble("aqi");
                                        double latTemp = latest.getDouble("temp");
                                        double latHum = latest.getDouble("humidity");
                                        double latWind = latest.getDouble("wind");

                                        List<FuturePredictionModel> predictions = predictor.predictFutureAQI(
                                                (int) latAqi, (float) latTemp, (float) latHum, (float) latWind);

                                        for (FuturePredictionModel model : predictions) {
                                            ContentValues fcv = new ContentValues();
                                            fcv.put(col_Hour, model.getHour());
                                            fcv.put(col_PredictedAQI, model.getPredictedAQI());
                                            fcv.put(col_City, city);
                                            fcv.put(col_Time, System.currentTimeMillis());
                                            db.insert(Table_Future, null, fcv);
                                        }

                                        // ✅ SPEED FIX 2: Process History
                                        for (DocumentSnapshot doc : historyDocs) {
                                            int aqiValue = doc.getLong("aqi").intValue();

                                            ContentValues cv = new ContentValues();
                                            cv.put(col_City, city);
                                            cv.put(col_AQI, aqiValue);
                                            cv.put(col_Temprature, doc.getDouble("temp"));
                                            cv.put(col_Humidity, doc.getDouble("humidity"));
                                            cv.put(col_Wind, doc.getDouble("wind"));
                                            cv.put(col_Time, doc.getLong("timstamp"));
                                            cv.put(col_FormattedTime, doc.getString("formattedTime"));

                                            // Match Range (Optimization: use a local list to avoid re-fetching)
                                            for (DocumentSnapshot range : rangeSnapshot.getDocuments()) {
                                                int min = range.getLong("minimum").intValue();
                                                int max = range.getLong("maximum").intValue();

                                                if (aqiValue >= min && aqiValue <= max) {
                                                    cv.put(col_Color, range.getString("color"));
                                                    cv.put(col_Category, range.getString("category"));
                                                    cv.put(col_Impact, range.getString("health_impact"));
                                                    cv.put(col_Action, range.getString("recommendation"));
                                                    cv.put(col_Min, min);
                                                    cv.put(col_Max, max);
                                                    break;
                                                }
                                            }
                                            db.insert(Table_AQI, null, cv);
                                        }

                                        db.setTransactionSuccessful();
                                        Log.d("SYNC", "Fast Sync Complete");
                                    } catch (Exception e) {
                                        Log.e("SYNC", "Error: " + e.getMessage());
                                    } finally {
                                        db.endTransaction();
                                    }
                                }).start();
                            });
                });
    }

    // Fetch AQL From SQLite
    public Cursor getLatestAQI(String city) {

        return db.rawQuery(
                "SELECT * FROM " + Table_AQI + " WHERE city = ? ORDER BY timestamp DESC LIMIT 1",
                new String[]{city}
        );
    }

    // Fetch All AQI Data for History
    public Cursor getAllAQI(String city) {

        return db.rawQuery(
                "SELECT * FROM " + Table_AQI + " WHERE city = ? ORDER BY timestamp DESC",
                new String[]{city}
        );
    }

    public Cursor getUnsyncedData() {
        return db.rawQuery(
                "SELECT * FROM AQI_History WHERE formattedTime = 'Offline'",
                null
        );
    }

    // ================= FUTURE AQI =================

    public Cursor getFutureAQI(String city) {
        return db.rawQuery(
                "SELECT * FROM " + Table_Future +
                        " WHERE city = ? ORDER BY hour ASC",
                new String[]{city}
        );
    }

    // ================= SAFETY =================

    @Override
    public synchronized void close() {
        db.close();
        super.close();
    }

}
