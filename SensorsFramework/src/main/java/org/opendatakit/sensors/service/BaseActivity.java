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
package org.opendatakit.sensors.service;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * This may be extended by various sensor implementations.
 *
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 *
 */
@SuppressLint("Registered")
public class BaseActivity extends Activity {
	private static final int MAX_RETRY = 3;

	private static final String MIDDLEWARE_PROXY_FAILED_MSG = "Middleware Proxy Failed";

	private final static String TAG = "BaseActivity";

	protected final static int SENSOR_DISCOVERY_RETURN = 42123;

	private SensorServiceProxy mwProxy;
	private Uri contentProviderUri;

	private String columns[] = new String[] { "_id", "sensorid", "sensortype",
			"data", "msgtype" };

	protected static class SensorData {
		public static String rowid, sensorID, sensorType, csvdata, messageType,
				timestamp;
	}

	/** Called when the activity is first created. */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mwProxy = new SensorServiceProxy(this);
		contentProviderUri = Uri
				.parse("content://org.opendatakit.sensorsV2.usbsensordataprovider/sensors");

	}

	@Override
	protected void onDestroy() {
		if (mwProxy != null) {
			mwProxy.shutdown();
		}

		super.onDestroy();
	}

	private boolean verifyConnection()  {
		// verify a good connections, else retry MAX_RETRY times to establish connection
		for (int retry = 0; retry < MAX_RETRY; retry++) {
			if (mwProxy == null) {
				mwProxy = new SensorServiceProxy(this);
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// don't really care, just gives time for system to rebind
				}
			}

			if (mwProxy.isBoundToService()) {
				return true;
			} else {
				mwProxy = null; // cause connection to be restablished
			}

		}
		return false;
	}

	protected void sensorConnect(String id, String appForDatabase)
			throws RemoteException {
		if (verifyConnection()) {
			mwProxy.sensorConnect(id, appForDatabase);
			return;
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected boolean startSensor(String id) throws RemoteException {
		if (verifyConnection()) {
			return mwProxy.startSensor(id);
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected boolean stopSensor(String id) throws RemoteException {
		if (verifyConnection()) {
			return mwProxy.stopSensor(id);
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected void configure(String id, String setting, Bundle params)
			throws RemoteException {
		if (verifyConnection()) {
			mwProxy.configure(id, setting, params);
			return;
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected List<Bundle> getSensorData(String id, long maxNumReadings)
			throws RemoteException {
		if (verifyConnection()) {
			return mwProxy.getSensorData(id, maxNumReadings);
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected void sendDataToSensor(String id, Bundle dataToSend) throws RemoteException {
		if (verifyConnection()) {
			mwProxy.sendDataToSensor(id, dataToSend);
			return;
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected boolean isConnected(String id) throws RemoteException {
		if (verifyConnection()) {
			return mwProxy.isConnected(id);
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected boolean isBusy(String id) throws RemoteException {
		if (verifyConnection()) {
			return mwProxy.isBusy(id);
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected boolean addSensor(String id, String driverType, String commChannel)
			throws RemoteException {
		if (verifyConnection()) {
			return mwProxy.addSensor(id, driverType, commChannel);
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected boolean hasSensor(String id) throws RemoteException {
		if (verifyConnection()) {
			return mwProxy.hasSensor(id);
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected void removeAllSensors() throws RemoteException {
		if (verifyConnection()) {
			mwProxy.removeAllSensors();
			return;
		}
		throw new NullPointerException(MIDDLEWARE_PROXY_FAILED_MSG);
	}

	protected void launchSensorDiscovery() {
		Intent i = new Intent();
		i.setClassName("org.opendatakit.sensors",
				"org.opendatakit.sensors.ui.activity.AddSensorActivity");
		startActivityForResult(i, SENSOR_DISCOVERY_RETURN);
	}

	protected void configureUsbBridgeSendRate(String id, int sendRate) throws RemoteException {
		Bundle params = new Bundle();
		params.putInt("SR", sendRate);
		configure(id, "SR", params);
	}

	protected void configureUsbBridgeReadRate(String id, int readRate) throws RemoteException {
		Bundle params = new Bundle();
		params.putInt("RR", readRate);
		configure(id, "RR", params);
	}

	protected String getAllRecords() {
		String toReturn = null;
		Cursor cur = managedQuery(contentProviderUri, columns, null, null, null);
		if (cur.moveToFirst()) {
			do {
				SensorData.rowid = cur.getString(cur.getColumnIndex("_id"));
				SensorData.sensorID = cur.getString(cur
						.getColumnIndex("sensorid"));
				SensorData.sensorType = cur.getString(cur
						.getColumnIndex("sensortype"));
				SensorData.csvdata += cur.getString(cur.getColumnIndex("data"))
						+ ",";
				SensorData.messageType = cur.getString(cur
						.getColumnIndex("msgtype"));

			} while (cur.moveToNext());
		}
		this.stopManagingCursor(cur);
		cur.close();
		toReturn = SensorData.csvdata;
		return toReturn;
	}

	protected String getLastRecords() {
		int selectedSensorMode = 1;

		String toReturn = null;
		String[] maxIDColumn = new String[] { "MAX(_id)" };
		Cursor curForMaxID = managedQuery(contentProviderUri, maxIDColumn,
				"sensorid=" + selectedSensorMode, null, null);
		String maxID = "1";
		if (curForMaxID.moveToFirst())
			;
		{
			maxID = curForMaxID.getString(curForMaxID
					.getColumnIndex("MAX(_id)"));
		}

		Log.d(TAG, " querying for SID:" + selectedSensorMode + " max id: "
				+ maxID);
		Cursor cur = managedQuery(contentProviderUri, columns, "_ID=" + maxID,
				null, null);
		if (cur.moveToFirst()) {
			do {
				SensorData.rowid = cur.getString(cur.getColumnIndex("_id"));
				SensorData.sensorID = cur.getString(cur
						.getColumnIndex("sensorid"));
				SensorData.sensorType = cur.getString(cur
						.getColumnIndex("sensortype"));
				SensorData.csvdata = cur.getString(cur.getColumnIndex("data"));
				SensorData.messageType = cur.getString(cur
						.getColumnIndex("msgtype"));
				toReturn = SensorData.rowid + "," + SensorData.sensorID + ","
						+ SensorData.sensorType + "," + SensorData.csvdata
						+ "," + SensorData.messageType;
				Log.d(TAG, toReturn);
			} while (cur.moveToNext());
		}
		curForMaxID.close();
		cur.close();
		this.stopManagingCursor(cur);
		this.stopManagingCursor(curForMaxID);
		return toReturn;
	}
}
