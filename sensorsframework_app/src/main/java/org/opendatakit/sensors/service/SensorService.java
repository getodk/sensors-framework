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

import org.opendatakit.sensors.SensorsSingleton;
import org.opendatakit.sensors.bluetooth.BluetoothManager;
import org.opendatakit.sensors.manager.DatabaseManager;
import org.opendatakit.sensors.manager.ODKSensorManager;
import org.opendatakit.sensors.dummy.DummyManager;
import org.opendatakit.sensors.usb.USBManager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


// TODO: add in the concept of shutting down the sensors a specific client is using
public class SensorService extends Service {
	
	private static final String LOGTAG = "SensorServiceV2";

	private SensorServiceInterface mServiceBinder;
	private ODKSensorManager mSensorManager;
	
	// database
	private DatabaseManager mDatabaseManager = null;

	// bluetooth manager
	private BluetoothManager mBtManager = null;
	//usb manager
	private USBManager usbManager = null;
	private DummyManager dummyManager;

	public void onCreate() {
		Log.e(LOGTAG,"SensorService onCreate entered");

		SensorsSingleton.construct(this);
		
		// create and initialize database
		mDatabaseManager = SensorsSingleton.getDatabaseManager();
		
		// create communication managers
		mBtManager = SensorsSingleton.getBluetoothManager();
		usbManager = SensorsSingleton.getUSBManager();
		dummyManager = SensorsSingleton.getDummyManager();
	
		// create sensor manager
		mSensorManager = SensorsSingleton.getSensorManager();
		
		mServiceBinder = new SensorServiceInterface(mSensorManager, mBtManager, usbManager, dummyManager);
		Log.e(LOGTAG,"SensorService onCreate exiting");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.e(LOGTAG,"SensorService onBind entered");
		return mServiceBinder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e(LOGTAG,"SensorService onStart");
		return START_STICKY;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.e(LOGTAG,"SensorService onUnbind entered");
		return false;
	}
	
	@Override 
	public void onLowMemory() {
		Log.e(LOGTAG,"SensorService onLowMemory");
	}
	
	@Override
	public void onDestroy() {
		Log.d(LOGTAG,"Starting onDestroy");
		if(mSensorManager != null) {
			mSensorManager.shutdown();
			mSensorManager = null;
		}
		
		// shutdown bluetooth
		if (mBtManager != null) {
			mBtManager.shutdown();
			mBtManager = null;
		}
		
		//shutdown usb
		if(usbManager != null) {
			usbManager.shutdown();
			usbManager = null;
		}	
			
		// close db
		if(mDatabaseManager != null) {
			mDatabaseManager.closeDb();
			mDatabaseManager = null;
		}
		
		SensorsSingleton.destroy();
		
		// destroy service
		super.onDestroy();	
		Log.d(LOGTAG,"Completed onDestroy");
//		System.runFinalizersOnExit(true);
//		System.exit(0);
//		android.os.Process.killProcess(android.os.Process.myPid());
	}


}
