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

import android.content.Context;
import android.util.Log;

import org.opendatakit.sensors.CommunicationChannelType;
import org.opendatakit.sensors.DriverType;
import org.opendatakit.sensors.ODKSensor;
import org.opendatakit.sensors.manager.AbstractChannelManagerBase;
import org.opendatakit.sensors.manager.DatabaseManager;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.manager.SensorNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class DummyManager extends AbstractChannelManagerBase {

	private String LOGTAG = "DummyManager";
	private HashMap<String, DummySensor> mSensorMap;
	private final boolean DEBUG = true;
	private DatabaseManager mDatabaseManager;
	
	public DummyManager(Context context, DatabaseManager database) {
		super(context, CommunicationChannelType.DUMMY);
		mSensorMap = new HashMap<String, DummySensor>();
		mDatabaseManager = database;
	}


	@Override
	public void initializeSensors() {
		// TODO Auto-generated method stub
		
	}
	

	@Override
	public List<DiscoverableDevice> getDiscoverableSensor() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public DummySensor getSensor(String id) {
		return mSensorMap.get(id);
	}

	public boolean sensorRegister(String id, DriverType sensorType, String appName) {
		if (DEBUG) Log.d(LOGTAG , "In sensor register.");
		if (id == null || sensorType == null) {
			Log.d(LOGTAG," registeration FAILED. sensor id or sensorType is null.");
			return false;
		}
		
		DummySensor ds = getSensor(id);
		if (ds == null) {
			if (DEBUG) Log.d(LOGTAG, "DummySensor is null");
			// NEED TO ADD A SENSOR

			mSensorManager.addSensor(id, sensorType, appName);
			if (DEBUG) Log.d(LOGTAG, "Added dummy sensor to sensor manager.");
		}

		if (ds != null) {
			return true;
		}

		return true;
	}

	public void sensorConnect(String id) throws SensorNotFoundException {
		if (DEBUG) Log.d(LOGTAG, "Sensor connect: " + id);
		DummySensor dummySensor = getSensor(id);

		// this seems like registration?
		
		if (dummySensor == null) {
			ODKSensor sensor = mSensorManager.getSensor(id);
			if (sensor == null) {
				throw new SensorNotFoundException(
				"Sensor not found in sensor manager");
			}
			if (DEBUG) Log.d(LOGTAG, "Dummy sensor created");

			dummySensor = new DummySensor(id, mDatabaseManager, sensor);
			mSensorMap.put(id, dummySensor);
		}

		if (dummySensor != null) {
			if (DEBUG) Log.d(LOGTAG, "Connecting to physical sensor");
			dummySensor.connect();
		}
	}

	public void sensorDisconnect(String id) throws SensorNotFoundException {
		// N/A
	}

	// Start the sensor data acquisition reading form the file
	// specified in command
	public void startSensorDataAcquisition(String id, byte[] command) {
		if (DEBUG) Log.d(LOGTAG, "Sensor record: " + id);

		// can safely ignore the bytes because they don't do anything
		
		// clear any previous buffer data
		ODKSensor sensor = mSensorManager.getSensor(id);
		sensor.dataBufferReset();

		// inform dummy sensor to start recording a sensor
		DummySensor ds = getSensor(id);

		if (ds != null)
			ds.activate();
	}

	public void stopSensorDataAcquisition(String id, byte[] command) {
		if (DEBUG) Log.d(LOGTAG, "Sensor record stop: " + id);

		// can safely ignore the bytes because they don't do anything

		
		DummySensor ds = getSensor(id);

		// inform dummy sensor to stop recording a sensor
		if (ds != null)
			ds.deactivate();
	}

	public void sensorWrite(String id, byte[] message) {
		DummySensor ds = getSensor(id);

		ds.write(new String(message));

	}

	public void searchForSensors() {
		// N/A
	}

	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public CommunicationChannelType getCommChannelType() {
		return CommunicationChannelType.DUMMY;
	}

	public synchronized void removeAllSensors() {
		// Cleanup All Of The Dummy Sensors
		if (mSensorMap != null) {
			for (String id : mSensorMap.keySet()) {
				mDatabaseManager.sensorDelete(id);
			}
			
			Collection<DummySensor> c = mSensorMap.values();
			Iterator<DummySensor> it = c.iterator();

			while (it.hasNext())
				it.next().kill();
		}
		mSensorMap.clear();
	}
}
