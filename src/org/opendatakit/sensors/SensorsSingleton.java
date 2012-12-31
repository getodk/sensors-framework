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
package org.opendatakit.sensors;

import org.opendatakit.sensors.bluetooth.BluetoothManager;
import org.opendatakit.sensors.exception.CustomUncaughtExceptionHandler;
import org.opendatakit.sensors.manager.DatabaseManager;
import org.opendatakit.sensors.manager.ODKSensorManager;
import org.opendatakit.sensors.tests.DummyManager;
import org.opendatakit.sensors.usb.USBManager;

import android.content.Context;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorsSingleton {

	private static final String LOGTAG = "SensorSingleton";

	protected static BluetoothManager bluetoothManager = null;

	protected static USBManager usbManager = null;

	protected static DummyManager dummyManager = null;

	protected static DatabaseManager dbManager = null;

	protected static ODKSensorManager sensorManager = null;

	protected static boolean constructed = false;

	public synchronized static void construct(Context cxt) {
		if (constructed) {
			return;
		}

		// START CONSTRUCTION
		Log.d(LOGTAG, "Starting Singleton Construction");

		// create and initialize database
		dbManager = new DatabaseManager(cxt);

		// create communication managers
		bluetoothManager = new BluetoothManager(cxt);
		usbManager = new USBManager(cxt);
		dummyManager = new DummyManager(cxt, dbManager);
		Thread.setDefaultUncaughtExceptionHandler(new CustomUncaughtExceptionHandler());

		// create sensor manager
		sensorManager = new ODKSensorManager(cxt, dbManager, bluetoothManager,
				usbManager, dummyManager);

		// provide reference to the sensor manager
		bluetoothManager.setSensorManager(sensorManager);
		usbManager.setSensorManager(sensorManager);
		dummyManager.setSensorManager(sensorManager);

		// try to connect to registered sensors
		sensorManager.initializeRegisteredSensors();

		// start communication managers
		bluetoothManager.initializeSensors();
		usbManager.initializeSensors();
		dummyManager.initializeSensors();

		// UPDATE STATE AFTER CONSTRUCTION COMPLETES
		constructed = true;
		;
		Log.d(LOGTAG, "Ending Singleton Construction");
	}

	public static void destroy() {
		dbManager = null;
		bluetoothManager = null;
		usbManager = null;
		dummyManager = null;
		constructed = false;
		System.gc();
	}

	public static BluetoothManager getBluetoothManager() {
		return bluetoothManager;
	}

	public static USBManager getUSBManager() {
		return usbManager;
	}

	public static DatabaseManager getDatabaseManager() {
		return dbManager;
	}

	public static ODKSensorManager getSensorManager() {
		return sensorManager;
	}

	public static DummyManager getDummyManager() {
		return dummyManager;
	}

}
