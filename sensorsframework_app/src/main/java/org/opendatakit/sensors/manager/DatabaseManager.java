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
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class DatabaseManager {

    // logging
	private static final String LOGTAG = "SensorServiceDatabase";

	// database metadata
	private static final String DATABASE_NAME = "sensors.db";
	private static final int DATABASE_VERSION = 4;

    // database helper
	private DatabaseHelper mOpenHelper;
	
	/**
	 * Sensor Table
	 */
	private class SensorTable {
		// cannot instantiate class
		private SensorTable() {
		}

		public static final String TABLE_NAME = "sensor";
		public static final String ID = "id"; // sensor id (primary key)
		public static final String NAME = "name"; // sensor name
		public static final String COMM_TYPE = "comm_type"; // communication type (Bluetooth, USB, etc.)
		public static final String TYPE = "type"; // sensor type
		public static final String STATE = "state"; // sensor state
		public static final String APP_NAME = "app_name"; // the app name space the sensor readings should be stored
	}

	/**
	 * Open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		/**
		 * Constructor
		 * @param context application context
		 */
		DatabaseHelper(Context context) {
			// initialize
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * Called when database created for the first time
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.w(LOGTAG, "DatabaseHelper onCreate");

			try {
				// create sensor table
				db.execSQL("CREATE TABLE " + SensorTable.TABLE_NAME + " (" + 
						SensorTable.ID	+ " TEXT PRIMARY KEY," + 
						SensorTable.NAME + " TEXT," + 
						SensorTable.TYPE + " TEXT," + 
						SensorTable.STATE + " TEXT," +
						SensorTable.COMM_TYPE + " TEXT," +
						SensorTable.APP_NAME + " TEXT"	+ ");");
				
			} catch (SQLException e) {
				Log.w(LOGTAG, "DatabaseHelper onCreate Failed!");
				Log.w(LOGTAG, e.getMessage());
				Log.w(LOGTAG, Log.getStackTraceString(e));
			}
		}

		/**
		 * Called when the database needs to be upgraded (version changed)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(LOGTAG, "DatabaseHelper onUpgrade: Upgrading database from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");

			// drop tables (recordings first)
			try {
				db.execSQL("DROP TABLE IF EXISTS " + SensorTable.TABLE_NAME);
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
	// SENSORS
	// ---------------------------------------------------------------------------------------------

    /**
     * Add new sensor. Replaces any existing sensor with the same id.
     * @param id sensor id
     * @param name sensor name
     * @param type sensor type
     * @param state sensor state
     * @param commType sensor communication type
     *
     */
    public synchronized void sensorInsert(String id, String name, String type, DetailedSensorState state,
                                          CommunicationChannelType commType, String appName) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        // store column values
        ContentValues values = new ContentValues();
        values.put(SensorTable.ID, id);
        values.put(SensorTable.NAME, name);
        values.put(SensorTable.TYPE, type);
        values.put(SensorTable.STATE, state.name());
        values.put(SensorTable.COMM_TYPE, commType.name());
        values.put(SensorTable.APP_NAME, appName);

        // insert (replace on conflicts)
        try {
            db.insertWithOnConflict(SensorTable.TABLE_NAME, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            Log.w(LOGTAG, e.getMessage());
            Log.w(LOGTAG, Log.getStackTraceString(e));
        }

    }

	/**
	 * Update existing sensor state
	 * @param id sensor id
	 * @param state sensor state
	 */
	public synchronized void sensorUpdateState(String id, DetailedSensorState state) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		// store new column value
		ContentValues values = new ContentValues();
		values.put(SensorTable.STATE, state.name());

		String[] args = { id };

		db.update(SensorTable.TABLE_NAME, values, SensorTable.ID + "=?", args);
	}
	
	/**
	 * Update existing sensor name
	 * @param id sensor id
	 * @param name sensor name
	 */
	public synchronized void sensorUpdateName(String id, String name) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		// store new column value

		ContentValues values = new ContentValues();
		values.put(SensorTable.NAME, name);

		String[] args = { id };

		db.update(SensorTable.TABLE_NAME, values, SensorTable.ID + "=?", args);
	}

	/**
	 * Query existing sensor state
	 * @param id sensor id
	 * @return sensor state
	 */
	public synchronized DetailedSensorState sensorQuerySensorState(String id) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		DetailedSensorState state = DetailedSensorState.DISCONNECTED;
		
		// query columns
		String[] cols = { SensorTable.ID, SensorTable.STATE };
		String[] args = { id };
		
		// run query
		Cursor cursor = db.query(SensorTable.TABLE_NAME, cols, SensorTable.ID + "=?", args, null,
				null, null);
		
		// return first state
		if( cursor.getCount() > 0 ) {
			cursor.moveToNext();
			state = DetailedSensorState.valueOf( cursor.getString(1) );
		}
		
		cursor.close();
		return state;
	}

	/**
	 * Query existing sensor type
	 * @id sensor id
	 * @return sensor type
	 */
	public synchronized String sensorQueryType(String id) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		String type = ServiceConstants.UNKNOWN;

		// query columns
		String[] cols = { SensorTable.ID, SensorTable.TYPE };
		String[] args = { id };

		// run query
		Cursor cursor = db.query(SensorTable.TABLE_NAME, cols, SensorTable.ID + "=?", args, null,
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
	 * @param id sensor id
	 * @return sensor name
	 */
	public synchronized String sensorName(String id) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		String name = "Unknown Name";

		// query columns
		String[] cols = { SensorTable.ID, SensorTable.NAME };
		String[] args = { id };

		// run query
		Cursor cursor = db.query(SensorTable.TABLE_NAME, cols, SensorTable.ID + "=?", args, null,
				null, null);

		// return first registration state
		if (cursor.getCount() > 0) {
			cursor.moveToNext();
			name = cursor.getString(1);
		}

		if (name == null)
		{
			name = "Unknown Name";
		}
		cursor.close();
		return name;
	}

	/**
	 * Determines if a sensor is in the database.
	 * @param id Sensor Id
	 * @return True if sensor is in DB, false Otherwise
	 */
	public synchronized boolean sensorIsInDatabase( String id ) {
	
		if( sensorName(id).compareTo("Unknown Name") == 0 )
			return false;
		else
			return true;
	}
	
	/**
	 * Query list of all available sensors
	 * @return list of known sensor
	 */
	public synchronized SensorData getSensorDataForId( String sensorId ) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		// query columns
		String[] cols = { SensorTable.ID, SensorTable.NAME, SensorTable.TYPE, SensorTable.STATE, SensorTable.APP_NAME};
		String[] args = { sensorId };
		
		// run query
		Cursor cursor = db.query(SensorTable.TABLE_NAME, cols, SensorTable.ID + "=?", args, null, null, null);

		// parse results
		SensorData data = new SensorData();
		while (cursor.moveToNext()) {
			data.id = cursor.getString(0);
			data.name = cursor.getString(1);
			data.type = cursor.getString(2);
			data.state = DetailedSensorState.valueOf(cursor.getString(3));
            data.appName = cursor.getString(4);
		}

		cursor.close();
		return data;
	}

	/**
	 * Query list of all available sensors
	 * @return list of known sensor
	 */
	public synchronized List<SensorData> sensorList( CommunicationChannelType type ) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();

		// query columns
		String[] cols = { SensorTable.ID, SensorTable.NAME, SensorTable.TYPE, SensorTable.STATE, SensorTable.APP_NAME};
		String[] args = { type.name() };
		
		// run query
		Cursor cursor = db.query(SensorTable.TABLE_NAME, cols, SensorTable.COMM_TYPE + "=?", args, null, null, null);

		// parse results
		ArrayList<SensorData> results = new ArrayList<SensorData>();
		while (cursor.moveToNext()) {
			SensorData data = new SensorData();
			data.id = cursor.getString(0);
			data.name = cursor.getString(1);
			data.type = cursor.getString(2);
			data.state = DetailedSensorState.valueOf(cursor.getString(3));
            data.appName = cursor.getString(4);
			results.add(data);
		}

		cursor.close();
		return results;
	}

	/**
	 * Delete a sensor and associated recordings
	 * @param id sensor id to delete
	 */
	public synchronized void sensorDelete(String id) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		String[] args = { id };

		db.delete(SensorTable.TABLE_NAME, SensorTable.ID + "=?", args);
	}
	
	
	public synchronized void deleteAllSensors() {
		for(CommunicationChannelType type : CommunicationChannelType.values()) {
			for(SensorData sensor : sensorList(type)) {
				sensorDelete(sensor.id);
			}
		}
	}
	
}
