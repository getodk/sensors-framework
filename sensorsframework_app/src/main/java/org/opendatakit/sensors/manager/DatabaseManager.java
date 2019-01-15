/*
 * Copyright (C) 2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.sensors.manager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import org.opendatakit.sensors.CommunicationChannelType;
import org.opendatakit.sensors.ServiceConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class DatabaseManager {

   // logging
   private static final String LOGTAG = "SensorServiceDatabase";

   // database metadata
   private static final String DATABASE_NAME = "sensors.db";
   private static final int DATABASE_VERSION = 5;

   // database helper
   private DatabaseHelper mOpenHelper;

   /**
    * External Sensor Table
    */
   private class ExternalSensorTable {
      // cannot instantiate class
      private ExternalSensorTable() {
      }

      public static final String TABLE_NAME = "externalsensors";
      public static final String ID = "id"; // sensor id (primary key)
      public static final String NAME = "name"; // sensor name
      public static final String COMM_TYPE = "comm_type"; // communication type (Bluetooth, USB, etc.)
      public static final String TYPE = "type"; // sensor type
      public static final String STATE = "state"; // sensor state
      public static final String APP_NAME = "app_name"; // the app name space the sensor readings should be stored
      public static final String DB_TRANSFER = "db_transfer"; // transfer data to DB automatically
   }

   /**
    * Internal Sensor Table
    */
   private class InternalSensorTable {
      // cannot instantiate class
      private InternalSensorTable() {
      }

      public static final String TABLE_NAME = "internalsensors";
      public static final String ID = "id"; // sensor id (primary key)
      public static final String APP_NAME = "app_name"; // the app name space the sensor readings should be stored
      public static final String DB_TRANSFER = "db_transfer"; // transfer data to DB automatically
   }

   /**
    * Open, create, and upgrade the database file.
    */
   private static class DatabaseHelper extends SQLiteOpenHelper {

      /**
       * Constructor
       *
       * @param context application context
       */
      DatabaseHelper(Context context) {
         // initialize
         super(context, DATABASE_NAME, null, DATABASE_VERSION);
      }

      /**
       * Called when database created for the first time
       */
      @Override public void onCreate(SQLiteDatabase db) {
         Log.w(LOGTAG, "DatabaseHelper onCreate");

         try {
            // create sensor table
            db.execSQL("CREATE TABLE " + ExternalSensorTable.TABLE_NAME + " (" +
                ExternalSensorTable.ID + " TEXT PRIMARY KEY," +
                ExternalSensorTable.NAME + " TEXT," +
                ExternalSensorTable.TYPE + " TEXT," +
                ExternalSensorTable.STATE + " TEXT," +
                ExternalSensorTable.COMM_TYPE + " TEXT," +
                ExternalSensorTable.APP_NAME + " TEXT," +
                ExternalSensorTable.DB_TRANSFER + " TEXT" + ");");

            // create sensor table
            db.execSQL("CREATE TABLE " + InternalSensorTable.TABLE_NAME + " (" +
                InternalSensorTable.ID + " TEXT PRIMARY KEY," +
                InternalSensorTable.APP_NAME + " TEXT," +
                InternalSensorTable.DB_TRANSFER + " TEXT" + ");");

         } catch (SQLException e) {
            Log.w(LOGTAG, "DatabaseHelper onCreate Failed!");
            Log.w(LOGTAG, e.getMessage());
            Log.w(LOGTAG, Log.getStackTraceString(e));
         }
      }

      /**
       * Called when the database needs to be upgraded (version changed)
       */
      @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
         Log.w(LOGTAG,
             "DatabaseHelper onUpgrade: Upgrading database from version " + oldVersion + " to "
                 + newVersion + ", which will destroy all old data");

         // drop tables (recordings first)
         try {
            db.execSQL("DROP TABLE IF EXISTS " + ExternalSensorTable.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + InternalSensorTable.TABLE_NAME);
         } catch (SQLException e) {
            Log.w(LOGTAG, "DatabaseHelper onUpgrade Failed!");
            Log.w(LOGTAG, e.getMessage());
            Log.w(LOGTAG, Log.getStackTraceString(e));
         }

         // recreate tables
         onCreate(db);
      }
   }

   /**
    * Constructor
    */
   public DatabaseManager(Context context) {
      // initialize database
      mOpenHelper = new DatabaseHelper(context);
   }

   public synchronized void closeDb() {
      mOpenHelper.close();
   }

   // ---------------------------------------------------------------------------------------------
   //  INTERNAL SENSORS
   // ---------------------------------------------------------------------------------------------

   /**
    * Add new internal sensor. Replaces any existing sensor with the same id.
    *
    * @param id         sensor id
    * @param appName    db appname to store sensor data
    * @param dbTransfer transfer data to DB automatically
    */
   public synchronized void insertInternalSensor(String id, String appName, boolean dbTransfer) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store column values
      ContentValues values = new ContentValues();
      values.put(InternalSensorTable.ID, id);
      values.put(InternalSensorTable.APP_NAME, appName);
      values.put(InternalSensorTable.DB_TRANSFER, Boolean.toString(dbTransfer));

      // insert (replace on conflicts)
      try {
         db.insertWithOnConflict(InternalSensorTable.TABLE_NAME, null, values,
             SQLiteDatabase.CONFLICT_REPLACE);
      } catch (SQLException e) {
         Log.w(LOGTAG, e.getMessage());
         Log.w(LOGTAG, Log.getStackTraceString(e));
      }

   }

   /**
    * Check if internal sensor has metadata in db
    *
    * @param id sensor id
    * @return true if internal sensor has metadata in db, false otherwise
    */
   public synchronized boolean internalSensorMetadataInDb(String id) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();
      boolean result = false;

      // query columns
      String[] cols = { InternalSensorTable.ID };
      String[] args = { id };

      // run query
      Cursor cursor = db
          .query(InternalSensorTable.TABLE_NAME, cols, InternalSensorTable.ID + "=?", args, null,
              null, null);

      // return first registration state
      if (cursor.getCount() > 0) {
         result = true;
      }

      cursor.close();
      return result;
   }

   /**
    * Query internal sensor app name
    *
    * @param id sensor id
    * @return database app name
    */
   public synchronized String internalSensorAppName(String id) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();
      String name = "Unknown Name";

      // query columns
      String[] cols = { InternalSensorTable.ID, InternalSensorTable.APP_NAME };
      String[] args = { id };

      // run query
      Cursor cursor = db
          .query(InternalSensorTable.TABLE_NAME, cols, InternalSensorTable.ID + "=?", args, null,
              null, null);

      // return first registration state
      if (cursor.getCount() > 0) {
         cursor.moveToNext();
         name = cursor.getString(1);
      }

      if (name == null) {
         name = "Unknown Name";
      }
      cursor.close();
      return name;
   }

   /**
    * Update existing database app name
    *
    * @param id      sensor id
    * @param appName database app name
    */
   public synchronized void internalSensorUpdateAppName(String id, String appName) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store new column value

      ContentValues values = new ContentValues();
      values.put(InternalSensorTable.APP_NAME, appName);

      String[] args = { id };

      db.update(InternalSensorTable.TABLE_NAME, values, InternalSensorTable.ID + "=?", args);
   }

   /**
    * Update whether to transfer data to DB automatically
    *
    * @param id         sensor id
    * @param dbTransfer transfer data to DB automatically
    */
   public synchronized void internalSensorUpdateDbTransfer(String id, boolean dbTransfer) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store new column value

      ContentValues values = new ContentValues();
      values.put(InternalSensorTable.DB_TRANSFER, Boolean.toString(dbTransfer));

      String[] args = { id };

      db.update(InternalSensorTable.TABLE_NAME, values, InternalSensorTable.ID + "=?", args);
   }

   /**
    * Query list of all internal sensors with metadata
    *
    * @return list of known sensor
    */
   public synchronized List<InternalSensorMetadata> internalSensorList() {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();

      // query columns
      String[] cols = { InternalSensorTable.ID, InternalSensorTable.APP_NAME,
          InternalSensorTable.DB_TRANSFER };

      // run query
      Cursor cursor = db.query(InternalSensorTable.TABLE_NAME, cols, null, null, null, null, null);

      // parse results
      ArrayList<InternalSensorMetadata> results = new ArrayList<InternalSensorMetadata>();
      while (cursor.moveToNext()) {
         InternalSensorMetadata data = new InternalSensorMetadata();
         data.id = cursor.getString(0);
         data.appName = cursor.getString(1);
         data.dbTransfer = Boolean.getBoolean(cursor.getString(2));
         results.add(data);
      }

      cursor.close();
      return results;
   }

   /**
    * Query internal sensor by id
    *
    * @param sensorId the id of sensor to return
    * @return list of known sensor
    */
   public synchronized InternalSensorMetadata getInternalSensorDataForId(String sensorId) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();

      // query columns
      String[] cols = { InternalSensorTable.ID, InternalSensorTable.APP_NAME,
          InternalSensorTable.DB_TRANSFER };
      String[] args = { sensorId };

      // run query
      Cursor cursor = db
          .query(InternalSensorTable.TABLE_NAME, cols, InternalSensorTable.ID + "=?", args, null,
              null, null);

      // parse results
      cursor.moveToFirst();
      InternalSensorMetadata data = new InternalSensorMetadata();
      data.id = cursor.getString(0);
      data.appName = cursor.getString(1);
      data.dbTransfer = Boolean.getBoolean(cursor.getString(2));

      cursor.close();
      return data;
   }

   /**
    * Delete an internal sensor metadata
    *
    * @param id sensor id to delete
    */
   public synchronized void internalSensorDelete(String id) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      String[] args = { id };

      db.delete(InternalSensorTable.TABLE_NAME, InternalSensorTable.ID + "=?", args);
   }

   public synchronized void deleteAllInternalSensorsMetadata() {
      for (InternalSensorMetadata sensor : internalSensorList()) {
         internalSensorDelete(sensor.id);
      }

   }

   // ---------------------------------------------------------------------------------------------
   //  EXTERNAL SENSORS
   // ---------------------------------------------------------------------------------------------

   /**
    * Add new external sensor. Replaces any existing sensor with the same id.
    *
    * @param id         sensor id
    * @param name       sensor name
    * @param type       sensor type
    * @param state      sensor state
    * @param commType   sensor communication type
    * @param appName    db appname to store sensor data
    * @param dbTransfer transfer data to DB automatically
    */
   public synchronized void insertExternalSensor(String id, String name, String type,
       DetailedSensorState state, CommunicationChannelType commType, String appName,
       boolean dbTransfer) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store column values
      ContentValues values = new ContentValues();
      values.put(ExternalSensorTable.ID, id);
      values.put(ExternalSensorTable.NAME, name);
      values.put(ExternalSensorTable.TYPE, type);
      values.put(ExternalSensorTable.STATE, state.name());
      values.put(ExternalSensorTable.COMM_TYPE, commType.name());
      values.put(ExternalSensorTable.APP_NAME, appName);
      values.put(ExternalSensorTable.DB_TRANSFER, Boolean.toString(dbTransfer));

      // insert (replace on conflicts)
      try {
         db.insertWithOnConflict(ExternalSensorTable.TABLE_NAME, null, values,
             SQLiteDatabase.CONFLICT_REPLACE);
      } catch (SQLException e) {
         Log.w(LOGTAG, e.getMessage());
         Log.w(LOGTAG, Log.getStackTraceString(e));
      }

   }

   /**
    * Update existing sensor state
    *
    * @param id    sensor id
    * @param state sensor state
    */
   public synchronized void externalSensorUpdateState(String id, DetailedSensorState state) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store new column value
      ContentValues values = new ContentValues();
      values.put(ExternalSensorTable.STATE, state.name());

      String[] args = { id };

      db.update(ExternalSensorTable.TABLE_NAME, values, ExternalSensorTable.ID + "=?", args);
   }

   /**
    * Update existing sensor name
    *
    * @param id   sensor id
    * @param name sensor name
    */
   public synchronized void externalSensorUpdateName(String id, String name) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store new column value

      ContentValues values = new ContentValues();
      values.put(ExternalSensorTable.NAME, name);

      String[] args = { id };

      db.update(ExternalSensorTable.TABLE_NAME, values, ExternalSensorTable.ID + "=?", args);
   }

   /**
    * Update existing database app name
    *
    * @param id      sensor id
    * @param appName database app name
    */
   public synchronized void externalSensorUpdateAppName(String id, String appName) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store new column value

      ContentValues values = new ContentValues();
      values.put(ExternalSensorTable.APP_NAME, appName);

      String[] args = { id };

      db.update(ExternalSensorTable.TABLE_NAME, values, ExternalSensorTable.ID + "=?", args);
   }

   /**
    * Update whether to transfer data to DB automatically
    *
    * @param id         sensor id
    * @param dbTransfer transfer data to DB automatically
    */
   public synchronized void externalSensorUpdateDbTransfer(String id, boolean dbTransfer) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      // store new column value

      ContentValues values = new ContentValues();
      values.put(ExternalSensorTable.DB_TRANSFER, Boolean.toString(dbTransfer));

      String[] args = { id };

      db.update(ExternalSensorTable.TABLE_NAME, values, ExternalSensorTable.ID + "=?", args);
   }

   /**
    * Query existing sensor state
    *
    * @param id sensor id
    * @return sensor state
    */
   public synchronized DetailedSensorState externalSensorQuerySensorState(String id) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();
      DetailedSensorState state = DetailedSensorState.DISCONNECTED;

      // query columns
      String[] cols = { ExternalSensorTable.ID, ExternalSensorTable.STATE };
      String[] args = { id };

      // run query
      Cursor cursor = db
          .query(ExternalSensorTable.TABLE_NAME, cols, ExternalSensorTable.ID + "=?", args, null,
              null, null);

      // return first state
      if (cursor.getCount() > 0) {
         cursor.moveToNext();
         state = DetailedSensorState.valueOf(cursor.getString(1));
      }

      cursor.close();
      return state;
   }

   /**
    * Query existing sensor type
    *
    * @return sensor type
    * @id sensor id
    */
   public synchronized String externalSensorQueryType(String id) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();
      String type = ServiceConstants.UNKNOWN;

      // query columns
      String[] cols = { ExternalSensorTable.ID, ExternalSensorTable.TYPE };
      String[] args = { id };

      // run query
      Cursor cursor = db
          .query(ExternalSensorTable.TABLE_NAME, cols, ExternalSensorTable.ID + "=?", args, null,
              null, null);

      // return first registration state
      if (cursor.getCount() > 0) {
         cursor.moveToNext();
         type = cursor.getString(1);
      }

      cursor.close();
      return type;
   }

   /**
    * Query sensor name
    *
    * @param id sensor id
    * @return sensor name
    */
   public synchronized String externalSensorName(String id) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();
      String name = "Unknown Name";

      // query columns
      String[] cols = { ExternalSensorTable.ID, ExternalSensorTable.NAME };
      String[] args = { id };

      // run query
      Cursor cursor = db
          .query(ExternalSensorTable.TABLE_NAME, cols, ExternalSensorTable.ID + "=?", args, null,
              null, null);

      // return first registration state
      if (cursor.getCount() > 0) {
         cursor.moveToNext();
         name = cursor.getString(1);
      }

      if (name == null) {
         name = "Unknown Name";
      }
      cursor.close();
      return name;
   }

   /**
    * Query external sensor by id
    *
    * @param sensorId the id of sensor to return
    * @return list of known sensor
    */
   public synchronized ExternalSensorData getExternalSensorDataForId(String sensorId) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();

      // query columns
      String[] cols = { ExternalSensorTable.ID, ExternalSensorTable.NAME, ExternalSensorTable.TYPE,
          ExternalSensorTable.STATE, ExternalSensorTable.APP_NAME };
      String[] args = { sensorId };

      // run query
      Cursor cursor = db
          .query(ExternalSensorTable.TABLE_NAME, cols, ExternalSensorTable.ID + "=?", args, null,
              null, null);

      // parse results
      ExternalSensorData data = new ExternalSensorData();
      while (cursor.moveToNext()) {
         data.id = cursor.getString(0);
         data.name = cursor.getString(1);
         data.type = cursor.getString(2);
         data.state = DetailedSensorState.valueOf(cursor.getString(3));
         data.appName = cursor.getString(4);
         data.dbTransfer = Boolean.getBoolean(cursor.getString(5));
      }

      cursor.close();
      return data;
   }

   /**
    * Query list of all available external sensors by communication type
    *
    * @param type communication channel type
    * @return list of known sensor
    */
   public synchronized List<ExternalSensorData> externalSensorList(CommunicationChannelType type) {
      SQLiteDatabase db = mOpenHelper.getReadableDatabase();

      // query columns
      String[] cols = { ExternalSensorTable.ID, ExternalSensorTable.NAME, ExternalSensorTable.TYPE,
          ExternalSensorTable.STATE, ExternalSensorTable.APP_NAME, ExternalSensorTable.DB_TRANSFER };
      String[] args = { type.name() };

      // run query
      Cursor cursor = db
          .query(ExternalSensorTable.TABLE_NAME, cols, ExternalSensorTable.COMM_TYPE + "=?", args,
              null, null, null);

      // parse results
      ArrayList<ExternalSensorData> results = new ArrayList<ExternalSensorData>();
      while (cursor.moveToNext()) {
         ExternalSensorData data = new ExternalSensorData();
         data.id = cursor.getString(0);
         data.name = cursor.getString(1);
         data.type = cursor.getString(2);
         data.state = DetailedSensorState.valueOf(cursor.getString(3));
         data.appName = cursor.getString(4);
         data.dbTransfer = Boolean.getBoolean(cursor.getString(5));
         results.add(data);
      }

      cursor.close();
      return results;
   }

   /**
    * Delete an external sensor
    *
    * @param id sensor id to delete
    */
   public synchronized void externalSensorDelete(String id) {
      SQLiteDatabase db = mOpenHelper.getWritableDatabase();

      String[] args = { id };

      db.delete(ExternalSensorTable.TABLE_NAME, ExternalSensorTable.ID + "=?", args);
   }

   public synchronized void deleteAllExternalSensors() {
      for (CommunicationChannelType type : CommunicationChannelType.values()) {
         for (ExternalSensorData sensor : externalSensorList(type)) {
            externalSensorDelete(sensor.id);
         }
      }
   }

}
