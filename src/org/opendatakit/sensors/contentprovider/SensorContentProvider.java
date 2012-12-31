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
package org.opendatakit.sensors.contentprovider;

import org.opendatakit.sensors.Constants;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorContentProvider extends ContentProvider {

	public static final String AUTHORITY = "org.opendatakit.sensorsV2.usbsensordataprovider";
	public static final Uri CONTENT_URI_ALL = Uri
			.parse("content://org.opendatakit.sensorsV2.usbsensordataprovider/sensors");

	@Override
	public boolean onCreate() {
		Context context = getContext();

		DatabaseHelper dbHelper = new DatabaseHelper(context, DATABASE_NAME,
				null, DATABASE_VERSION);

		sensorDataDB = dbHelper.getWritableDatabase(); // ok to get handle here?
														// or get it in each
														// call, use, then
														// close?
		return (sensorDataDB == null) ? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sort) {

		// Log.e(TAG,"CP query entered");
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(SENSORDATA_TABLE);

		// TODO: uncomment and test this when needed. not getting into this just
		// yet

		// // If this is a row query, limit the result set to the passed in row.
		// switch (uriMatcher.match(uri)) {
		// case SENSOR_ID: qb.appendWhere(KEY_ID + "=" +
		// uri.getPathSegments().get(1));
		// break;
		// default : break;
		// }

		// If no sort order is specified sort by date / time
		String orderBy;
		if (TextUtils.isEmpty(sort)) {
			orderBy = Constants.KEY_TIMESTAMP;
		} else {
			orderBy = sort;
		}

		// Log.e(TAG,"execing query");
		// Apply the query to the underlying database.
		Cursor c = qb.query(sensorDataDB, projection, selection, selectionArgs,
				null, null, orderBy);

		// Log.e(TAG,"executed query");
		// Register the contexts ContentResolver to be notified if
		// the cursor result set changes.
		c.setNotificationUri(getContext().getContentResolver(), uri);

		// Log.e(TAG,"returning");
		// Return a cursor to the query result.
		return c;
	}

	@Override
	public Uri insert(Uri _uri, ContentValues _initialValues) {
		// Log.e(TAG,"CP entered insert");
		// Insert the new row, will return the row number if
		// successful.
		long rowID = sensorDataDB
				.insert(SENSORDATA_TABLE, null, _initialValues);
		// Log.e(TAG,"CP execed insert");

		// Return a URI to the newly inserted row on success.
		if (rowID > 0) {
			Uri uri = ContentUris.withAppendedId(CONTENT_URI_ALL, rowID);
			// Log.e(TAG,"CP returning new URI: "+ uri.toString());
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		}
		throw new SQLException("Failed to insert row into " + _uri);
	}

	// TODO: delete not tested at all!
	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		int count;

		switch (uriMatcher.match(uri)) {
		case SENSORS:
			count = sensorDataDB.delete(SENSORDATA_TABLE, where, whereArgs);
			break;

		// TODO: uncomment and test this when needed. not getting into this just
		// yet
		// case SENSOR_ID:
		// String segment = uri.getPathSegments().get(1);
		// count = sensorDataDB.delete(SENSORDATA_TABLE, KEY_ID + "="
		// + segment
		// + (!TextUtils.isEmpty(where) ? " AND ("
		// + where + ')' : ""), whereArgs);
		// break;

		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	// TODO: update not tested at all!
	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		int count;
		switch (uriMatcher.match(uri)) {
		case SENSORS:
			count = sensorDataDB.update(SENSORDATA_TABLE, values, where,
					whereArgs);
			break;

		case SENSOR_ID:
			String segment = uri.getPathSegments().get(1);
			count = sensorDataDB.update(SENSORDATA_TABLE, values,
					Constants.KEY_ID
							+ "="
							+ segment
							+ (!TextUtils.isEmpty(where) ? " AND (" + where
									+ ')' : ""), whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		Log.e(TAG, "CP entered getType");
		switch (uriMatcher.match(uri)) {
		case SENSORS:
			return "vnd.android.cursor.dir/vnd.opendatakit.sensors";
		case SENSOR_ID:
			return "vnd.android.cursor.item/vnd.opendatakit.sensors";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	// Create the constants used to differentiate between the different URI
	// requests.
	private static final int SENSORS = 1; // all sensors
	private static final int SENSOR_ID = 2; // 1 particular sensor

	private static final UriMatcher uriMatcher;

	// Allocate the UriMatcher object, where a URI ending in 'sensors' will
	// correspond to a request for data from all sensors, and 'sensors' with a
	// trailing '/[rowID]' will represent a single sensor row.
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(AUTHORITY, "sensors", SENSORS);
		uriMatcher.addURI(AUTHORITY, "sensors/#", SENSOR_ID);
	}

	// The underlying database
	private SQLiteDatabase sensorDataDB;
	private static final String TAG = "USBSensorDataProvider";
	private static final String DATABASE_NAME = "sensordata.db";
	private static final int DATABASE_VERSION = 1;
	private static final String SENSORDATA_TABLE = "sensordata";

	// Helper class for opening, creating, and managing database version control
	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_CREATE = "create table "
				+ SENSORDATA_TABLE + " (" + Constants.KEY_ID
				+ " integer primary key autoincrement, " + Constants.KEY_TIMESTAMP
				+ " INTEGER, " + Constants.KEY_SENSOR_ID + " TEXT, " + Constants.KEY_SENSOR_TYPE
				+ " TEXT, " + Constants.KEY_MSG_TYPE + " TEXT, " + Constants.KEY_DATA + " TEXT);";

		public DatabaseHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");

			db.execSQL("DROP TABLE IF EXISTS " + SENSORDATA_TABLE);
			onCreate(db);
		}
	}
}