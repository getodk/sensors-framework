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
package org.opendatakit.sensors.usb;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendatakit.sensors.CommunicationChannelType;
import org.opendatakit.sensors.DriverType;
import org.opendatakit.sensors.manager.AbstractChannelManagerBase;
import org.opendatakit.sensors.manager.DetailedSensorState;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.manager.SensorNotFoundException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;


//svn version 1145: switching to the Android 3.1+ (API level 12) way of interacting with the USB subsystem.   

public class USBManager extends AbstractChannelManagerBase {
	
	private static final String TAG = "USBManager";
	private boolean DEBUG_VERBOSE = false;
	private static final String ACTION_USB_PERMISSION = "org.opendatakit.sensors.USB_PERMISSION";
		
    private PendingIntent usbAuthPendingIntent;
	private USBCommSubChannel activeCommChannel;
	private USBScanner deviceScanner;
	private volatile boolean deviceAttached = false;
	private volatile Set<String> connectedSensors;
	
	/*
	 * Service lifecycle methods
	 */
	
	/**
	 * Public constructor 
	 * @param svcContext
	 */
	public USBManager (Context svcContext){
		super(svcContext,CommunicationChannelType.USB);
		
		if(DEBUG_VERBOSE) Log.d(TAG,"constructor entered!");						
        
        usbAuthPendingIntent = PendingIntent.getBroadcast(mContext,
				0, new Intent(ACTION_USB_PERMISSION), 0);
        
        connectedSensors = Collections.synchronizedSet(new HashSet<String>());
        
		if(DEBUG_VERBOSE) Log.d(TAG,"constructor exited!");
	}
	
    public void shutdown(){
    	if(DEBUG_VERBOSE) Log.d(TAG,"shutdown entered!");    	    	

    	if(deviceAttached)
    		activeCommChannel.shutdown();
    	
    	mContext.unregisterReceiver(mUsbReceiver);
    	
    	if(deviceScanner != null) {
    		deviceScanner.shutdownThread();
    		deviceScanner = null;
    	}
    	
    	disconnectAllSensors();
		
		if(DEBUG_VERBOSE) Log.d(TAG,"shutdown exited!");
    }
	
	/*
	 * Methods indicated by AbstractChannelManagerBase
	 */
	
	@Override
	public void initializeSensors() {
		deviceScanner = new USBScanner();
        deviceScanner.start();
        
        mContext.registerReceiver(mUsbReceiver, new IntentFilter(
				UsbManager.ACTION_USB_ACCESSORY_ATTACHED));
		mContext.registerReceiver(mUsbReceiver, new IntentFilter(
				UsbManager.ACTION_USB_ACCESSORY_DETACHED));
		mContext.registerReceiver(mUsbReceiver, new IntentFilter(
				UsbManager.ACTION_USB_DEVICE_ATTACHED));
		mContext.registerReceiver(mUsbReceiver, new IntentFilter(
				UsbManager.ACTION_USB_DEVICE_DETACHED));

	}	

	@Override
	public List<DiscoverableDevice> getDiscoverableSensor() 
	{
		if(deviceAttached) {
			return activeCommChannel.getDiscoverableSensor();
		}
		return null;
	}
	
	/*
	 * Sensor connection methods
	 */
	
	/**
	 * Sensor register
	 * 
	 * @return True if sensor is successfully registered or has already been registered
	 */
	public boolean sensorRegister(String id_to_add, DriverType sensorType) {
		if(deviceAttached) {
			return activeCommChannel.sensorRegister(id_to_add, sensorType);
		}
		return false;
	}
	
	public void sensorConnect(String id) throws SensorNotFoundException {		
		if(deviceAttached) {
			activeCommChannel.sensorConnect(id);
			connectedSensors.add(id);
		}
	}

	public void sensorDisconnect(String id) throws SensorNotFoundException {
		if(deviceAttached) {
			activeCommChannel.sensorDisconnect(id);
			connectedSensors.remove(id);
		}
	}

	public void sensorWrite(String id, byte[] message) {		
		if(deviceAttached)
			activeCommChannel.sensorWrite(id, message);
	}

	public void searchForSensors() {	
		if(deviceAttached)
			activeCommChannel.searchForSensors();
	}

	@Override
	public void startSensorDataAcquisition(String id, byte[] command) {
		if(deviceAttached)
			activeCommChannel.startSensorDataAcquisition(id, command);	
	}

	@Override
	public void stopSensorDataAcquisition(String id, byte[] command) {
		if(deviceAttached)
			activeCommChannel.stopSensorDataAcquisition(id, command);
	}

	public synchronized void removeAllSensors() {
		if(deviceAttached)
			activeCommChannel.removeAllSensors();
	}
	
	private void disconnectAllSensors() {
		synchronized(connectedSensors) {
			
			for(String sensorId : connectedSensors) {
				mSensorManager.updateSensorState(sensorId,DetailedSensorState.DISCONNECTED);
			}
			connectedSensors.clear();
		}
	}
	
	public final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			Toast.makeText(context, "received USB event",
					Toast.LENGTH_SHORT).show();

			Log.d(TAG, "Received USB event");
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				Log.d(TAG, "Received USB Accessory Detached");				
				deviceAttached = false;												
			}
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
				Log.d(TAG, "Received USB Accessory Attached");
			}
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				Log.d(TAG, "Received USB Device Attached");
			}
			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				Log.d(TAG, "Received USB Device Detached");
				deviceAttached = false;				
			}
			
			if(deviceScanner != null) {
				synchronized(deviceScanner) {
					if(deviceAttached == false) {
						//usb connection dropped. notify scanner thread to start scanning port for devices to be connected.
						if(activeCommChannel != null)
							activeCommChannel.shutdown();
						
						disconnectAllSensors();
						deviceScanner.notifyAll();
					}
				}
			}
		}
	};
	
	private class USBScanner extends Thread {

		private final AtomicBoolean isRunning = new AtomicBoolean(true);
		
		public USBScanner() {
			super("USBScanner");
		}
		
		public void shutdownThread() {
			isRunning.set(false);
			interrupt();
		}

		@Override
		public void run() {
			while (isRunning.get()) {
				if(ArduinoSubChannel.scanForDevice(mContext)) {

					Log.d(TAG,"Found Arduino ADK device");				
					ArduinoSubChannel.authorize(mContext, usbAuthPendingIntent);
					activeCommChannel = new ArduinoSubChannel(mContext,mSensorManager);
					if(((ArduinoSubChannel)activeCommChannel).channelInited()) {
						deviceAttached = true;
						Log.d(TAG,"ArduinoChannel: deviceAttached set to true");

					}
				}
				else if(FTDISubChannel.scanForDevice(mContext)) {

					Log.d(TAG,"Found FTDI device");					

					FTDISubChannel.authorize(mContext, usbAuthPendingIntent);
					activeCommChannel = new FTDISubChannel(mContext,mSensorManager);
					deviceAttached = true;
					Log.d(TAG,"FTDIChannel: deviceAttached set to true");
				}
				//else if some other usb channel.

				if(deviceAttached == true) {
					//established a usb connection. stop scanning.
					synchronized (this) {
						try {
							this.wait();
						}
						catch(InterruptedException iex) {
							iex.printStackTrace();
						}							
					}
				}

				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}			
		}
	}
}