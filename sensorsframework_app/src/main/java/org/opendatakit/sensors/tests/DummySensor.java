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
package org.opendatakit.sensors.tests;

import android.util.Log;
import org.opendatakit.sensors.ODKSensor;
import org.opendatakit.sensors.SensorDataPacket;
import org.opendatakit.sensors.ServiceConstants;
import org.opendatakit.sensors.manager.DatabaseManager;
import org.opendatakit.sensors.manager.DetailedSensorState;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class DummySensor extends Thread {

	// logging
	private static final String LOGTAG = "DummySensor";

	// Protocol W/ Simulated Phone Sensors
	public static final String GET_TYPE = "GET_TYPE";
	public static final String STOP_DATA = "STOP_DATA";
	public static final String START_DATA = "START_DATA";
	private static final boolean DEBUG = true;

	private final AtomicBoolean mIsActivated = new AtomicBoolean(false);
	private AtomicBoolean mKillMe = new AtomicBoolean(false);
	
	// data
	private String mId;
	private ODKSensor mSensor;
	private DatabaseManager mDbMan;

	boolean notConnected = false;
	boolean sensorActivated = false;
	String sensorType = ServiceConstants.UNKNOWN;
	InputStream sockReader = null;
	BufferedWriter sockWriter = null;
	long startTime = 0;

	private int mSendDelay = 10;
	private int mPacketSize = 1;


	/**
	 * Sensor Constructor
	 * 
	 * @param id
	 *            Sensor ID
   *
   * @param dbm
   * 						database manager
   *
   * @param sensor
   *            TODO
	 */
	public DummySensor(String id, DatabaseManager dbm, ODKSensor sensor) {
		mDbMan = dbm;
		mId = id;
		mSensor = sensor;
     mDbMan.externalSensorUpdateState(id, DetailedSensorState.CONNECTED);
  }

	/**
	 * Attempt Connection To Sensor
	 */
	public void connect() {
		// query current state
     DetailedSensorState ss = mDbMan.externalSensorQuerySensorState(mId);
     if (DEBUG)
        Log.d(LOGTAG, "In connect sensor");

		if (ss == DetailedSensorState.CONNECTED) {
			// update state to connecting
			this.start();
		} else {
			Log.e(LOGTAG, "Will Not Connect Unpaired || Unregistered Sensor "
					+ mId);
       mDbMan.externalSensorUpdateState(mId, DetailedSensorState.CONNECTED);
       this.start();
    }
	}

	/**
	 * Start Data Read
	 */
	public void activate() {
		if (DEBUG) Log.d(LOGTAG, "Activated start data collection");
		mIsActivated.set(true);
	}

	/**
	 * Stop Data Read
	 */
	public void deactivate() {
		if (DEBUG) Log.d(LOGTAG, "Deactivated start data collection");
		mIsActivated.set(false);
	}

	public void helperResetConnection() {
     mDbMan.externalSensorUpdateState(mId, DetailedSensorState.DISCONNECTED);
     mSensor.dataBufferReset();
  }

	public void write(String data) {
		Log.d(LOGTAG, "Dummy sensor write: " + data);
		if (data.startsWith("SendDelay")) {
			String t = data.substring("SendDelay".length());
			mSendDelay = Integer.parseInt(t);
			Log.d(LOGTAG, "Set send delay: " + mSendDelay);
		} else if (data.startsWith("PacketSize")) {
			String t = data.substring("PacketSize".length());
			mPacketSize = Integer.parseInt(t);
		} else if (data.startsWith("S")) {
			activate();
		} else if (data.startsWith("E")) {
			deactivate();
		}
	}

	/**
	 * Thread Main Loop, Handles Data Connection
	 */
	public void run() {

		if (DEBUG) Log.d(LOGTAG, "Sensor Connection Thread Created: " + mId);

		while (true) {
			
			if (mKillMe.get() == true) {
				mIsActivated.set(false);
				break;
			}
			
			// SENSOR ACTIVATED
			if (mIsActivated.get() == true) {

				// LOOK FOR TOGGLE TO ACTIVATED
				if (sensorActivated == false) {
					startTime = System.currentTimeMillis();
					sensorActivated = true;
				}

				if (readSensorData() == false) {
					mIsActivated.set(false);
				}
			}
			// SENSOR DEACTIVATED
			else {
				if (sensorActivated == true) {

					// Real -- Command Sensor To Stop
					sensorActivated = false;
				}

			}
		}
	}

	/**
	 * Reads Sensor Data And Passes To DataProvider
	 */
	private boolean readSensorData() {
		try {
			Thread.sleep(mSendDelay);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		byte[] data = new byte[mPacketSize];
		for (int i = 0; i < mPacketSize; i++) {
			data[i] = 1;
		}
		SensorDataPacket sdp = new SensorDataPacket(data, 0);
		mSensor.addSensorDataPacket(sdp);
		return true;
	}

	public void kill() {
		mKillMe.set(true);
	}

}
