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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.opendatakit.sensors.DriverType;
import org.opendatakit.sensors.ODKSensor;
import org.opendatakit.sensors.SensorDataPacket;
import org.opendatakit.sensors.ServiceConstants;
import org.opendatakit.sensors.manager.DetailedSensorState;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.manager.ODKSensorManager;
import org.opendatakit.sensors.manager.SensorNotFoundException;
import org.opendatakit.sensors.usb.a3pTransport.A3PSession;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class ArduinoSubChannel implements USBCommSubChannel, Runnable {
	
	// File-wide settings
		private static final boolean DEBUG_VERBOSE = false;
		
		private static final String TAG = "ArduinoChannel";
		private static final byte ENUMERATE_SENSORS = 0x4;
		private static final int MAX_RETRIES_FOR_DISCOVERY = 3;
		private static final int MAX_RETRIES_FOR_CONN_READY = 3;
		
		// Private working variables
	    private static UsbManager androidUsbManager;		// The Android-provided UsbManager
	    private static UsbAccessory adkAccessory;			// The ADK-compatible accessory we're connected to
	    private ParcelFileDescriptor myParcelFD;	// The file descriptor to our accessory
	    private volatile List<String> deviceIDs;	// List of ids of sensors discovered on the ADK device
		
		private Thread workerThread;				// The worker to process incoming USBMessages
		private boolean workerRun;					// Flag to indicate whether to keep running the worker thread
		
		private A3PSession a3pSession;				// Reference to the A3PSession managing the current connection
		private boolean usbInited = false;
		public String A3P_DEVICE_ID = "0";
		private ODKSensorManager mSensorManager;
		private Context mContext;
				
		private List<DiscoverableDevice> mDiscoverableDeviceList;
		
	public ArduinoSubChannel (Context svcContext,ODKSensorManager sensorManager) {
		
		if(DEBUG_VERBOSE) Log.d(TAG,"constructor entered!");		
		
		//XXX we should have only 1 data structure to store discovered sensors.
		//that data stucture should include sensorID & sensor description sent from Arduino
		deviceIDs = new ArrayList<String>();
		mDiscoverableDeviceList = new ArrayList<DiscoverableDevice>();	
		
		mSensorManager = sensorManager;
		mContext = svcContext;

        initUSB();       
        
		if(DEBUG_VERBOSE) Log.d(TAG,"constructor exited!");
	}
	
	public static boolean scanForDevice(Context svcContext) {					
		UsbAccessory[] accessories = ((UsbManager) svcContext.getSystemService(Context.USB_SERVICE)).getAccessoryList(); 
    	if(accessories != null) {
    		adkAccessory = accessories[0];
    		return true;
    	}
    	
		return false;
	}
	
	public static void authorize(Context svcContext, PendingIntent usbAuthPendingIntent) {
		androidUsbManager = ((UsbManager) svcContext.getSystemService(Context.USB_SERVICE));
		
		if (!androidUsbManager.hasPermission(adkAccessory)) {				
			androidUsbManager.requestPermission(adkAccessory, usbAuthPendingIntent);
		}
	}
	
    public void shutdown(){
    	if(DEBUG_VERBOSE) Log.d(TAG,"shutdown entered!");    	    	

    	// Kill the A3PSession and wait for it to stop
    	if(a3pSession != null) {    	
    		Log.d(TAG, "Ending A3PSession and waiting for it to stop");
    		a3pSession.endConnection();
    		while(!a3pSession.isClosed()){
    			try{
    				synchronized(this){
    					this.wait(100);
    				}
    			} catch (InterruptedException e){
    				// Do nothing if interrupted.
    			}
    		}
    	}

    	if(workerThread != null){
    		Log.d(TAG, "Ending worker thread and waiting for it to stop");
    		// Kill the worker thread and wait for it to stop
    		workerRun = false;
    		workerThread.interrupt();
    		while(workerThread.isAlive()){
    			try{
    				synchronized(this){
    					this.wait(100);
    				}
				} catch (InterruptedException e){
					// Do nothing if interrupted.
				}
	    	}
    	}

		closeMyAccessory();		
		
		if(DEBUG_VERBOSE) Log.d(TAG,"shutdown exited!");
    }
	
    // Initialize the USB
    private void initUSB() {
    	if(initA3P()) {
    		initThread();
    		int retries = 0;
    		while(++retries < MAX_RETRIES_FOR_CONN_READY) {
    			if(a3pSession.isConnected()) {
    				Log.d(TAG,"USB ready for communication");
    				usbInited = true;
    				break;
    			}
    			try {
    				Thread.sleep(500);
    			}
    			catch(InterruptedException iex) {
    				
    			}
    		}
    	}
    }
    
    public boolean channelInited() {
    	return usbInited;
    }
	
	/*
	 * Methods indicated by AbstractChannelManagerBase
	 */
	public void initializeSensors() {
		getDiscoverableSensor();
		if(mDiscoverableDeviceList == null) {
			Log.e(TAG," getDiscoverableSensor returned NULL");
		}
		else {
		Log.d(TAG, "initSensors discovered list size: " + mDiscoverableDeviceList.size());
		}
	}	

	public List<DiscoverableDevice> getDiscoverableSensor() 
	{
		List<DiscoverableDevice> deviceList = null;
		
		// Reinit the USB (if needed), then ask for a list of devices
		if(!usbInited)
			initUSB();
		
		// If we have an a3pSession, try to get a device list from the ADK board
		if(a3pSession != null && !a3pSession.isClosed()) {
			deviceIDs.clear();			
			
			Log.d(TAG, "Sending enumerate sensors req");

			int retries = 0;
			while (deviceIDs.size() == 0 && (retries < MAX_RETRIES_FOR_DISCOVERY)) {
				sendDeviceListRequest();
				retries++;
				if(DEBUG_VERBOSE)
					Log.d(TAG, "blocking for notify. retry cnt: " + retries);
				synchronized(deviceIDs) {
					try {
						deviceIDs.wait(2000);
						Log.d(TAG, "woke up with " + deviceIDs.size());
					}
					catch(InterruptedException iex) {
						iex.printStackTrace();
					}
				}
			}

			if(deviceIDs.size() == 0)
			{
				Log.e(TAG, "Finding deviceIDs failed!");
				Log.d(TAG, "<--- Exiting getDiscoverableSensor()");
				return null;
			} 

			Log.d(TAG, "Finding deviceIDs succeeded!");				

			deviceList= new ArrayList<DiscoverableDevice>();
			Iterator<String> deviceListIterator = deviceIDs.iterator();
			while (deviceListIterator.hasNext())
			{
				String sensorID = deviceListIterator.next();
				USBDiscoverableDevice newDiscoverableDevice = new USBDiscoverableDevice(sensorID,this.mSensorManager);			
				newDiscoverableDevice.connectionLost = false;
				deviceList.add(newDiscoverableDevice);
				
				Log.d(TAG,"updating sensor state in sensormanager");
				mSensorManager.updateSensorState(sensorID, DetailedSensorState.CONNECTED);
				Log.d(TAG, "sensorID " + sensorID + " set to connected in DB");
				
			}

			this.mDiscoverableDeviceList.clear();
			this.mDiscoverableDeviceList.addAll(deviceList);

			Log.d(TAG, deviceList.size() + " sensors found");		
		}
		else {
			Log.d(TAG,"a3pSession is null or closed. cannot discover sensors ");		
		}

		Log.d(TAG,"<--- Exiting getDiscoverableSensor()");
		return deviceList;
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
		Log.d(TAG, "In sensor register.");
		if (sensorType == null) {
			Log.d(TAG, "Did not find sensor type for sensor " + id_to_add);
			return false;
		}
		
		if(mDiscoverableDeviceList == null || mDiscoverableDeviceList.size() == 0 ) {
			//re init once again 
			initializeSensors();			
		}
		
		if(mDiscoverableDeviceList == null || mDiscoverableDeviceList.size() == 0 ) {
			//still no sensors? 
			Log.e(TAG, "No device ids, check usb connection!  FAILED to register '" +
			sensorType + "' sensor with passed id '" + id_to_add + "'.");
			return false;
		}
		
		boolean foundSensor = false;
		
		// TODO: Add a map from string id to string description
		
		Iterator <DiscoverableDevice>discoverableDeviceIterator = mDiscoverableDeviceList.iterator();
		while (discoverableDeviceIterator.hasNext())
		{
			USBDiscoverableDevice newDevice = (USBDiscoverableDevice) discoverableDeviceIterator.next();
			if (newDevice.getDeviceId().equals(id_to_add) && newDevice.connectionLost == false)
			{
				foundSensor = true;
			}
		}	
		
		if(foundSensor){			
			if(mSensorManager.getSensor(id_to_add) != null) 
				return true;
			// Need to add the sensor
			if(mSensorManager.addSensor(id_to_add, sensorType)){
				Log.d(TAG, "Added usb sensor to sensor manager.");
				
				Intent i = new Intent();
				i.setAction(ServiceConstants.USB_STATE_CHANGE);
				mContext.sendBroadcast(i);
				return true;
			} else {
				Log.e(TAG, "Failed to add usb sensor to sensor manager");
			}
		} 

		Log.d(TAG, "Can't find sensor: '" + id_to_add + "' of type '" +
				sensorType + "' registration failed.");
		return false;
	}

	private void sendDeviceListRequest(){
		byte[] payload = new byte[1];
		payload[0] = ENUMERATE_SENSORS;
		sensorWrite("0",payload);
	}		
	
    /*
     * Private helper methods
     */
    
	// open the accessory here	
	private boolean openMyAccessory(){
    	boolean accOpened = false;
    	if(DEBUG_VERBOSE) Log.d(TAG,"openMyAccessory entered!");    	  	
    	
    	// Next, ask the UsbManager (singleton?) for the ParcelFileDescriptor for the accessory
    	myParcelFD = androidUsbManager.openAccessory(adkAccessory);    	
    	
    	if(myParcelFD != null){	    	
    		accOpened = true;
			if(DEBUG_VERBOSE)
    		Log.d("myADK_OpenMyAccessory", "accessory opened. " + myParcelFD.toString());
    		
    	} else {
    		Log.e("myADK_OpenMyAccessory", "opening failed :(");	
    	}
    	
    	if(DEBUG_VERBOSE) Log.d(TAG,"openMyAccessory exited!");
    	return accOpened;
    }
    
    /**
     * Close my accessory's file streams here
     */
    public synchronized void closeMyAccessory(){
    	if(DEBUG_VERBOSE) Log.d(TAG,"closeMyAccessory entered!");
    	
		try {
			if (myParcelFD != null) {
				myParcelFD.close();
			}
		} catch (IOException e) {
			Log.e("myADK_closeMyAccessory", "error trying to close ParcelFileDescriptor");
		} finally {
			
			myParcelFD = null;
			adkAccessory = null;
			usbInited = false;
			
			// Give the data thread 100 ms to stop on its own after setting
			// the file descriptor and accessory to null
			try{
				synchronized(this){
					this.wait(100);
				}
			} catch (InterruptedException e) {
				// Interrupted while waiting, do nothing
				;
			}
			
			for(String sensor : deviceIDs) {
				if(mSensorManager.getSensor(sensor) != null) {
					mSensorManager.updateSensorState(sensor, DetailedSensorState.DISCONNECTED);
					Log.d(TAG,sensor + " updated to DISCONNECTED state in db");
				}
			}
			
			deviceIDs.clear();
		}		
		
		if(DEBUG_VERBOSE) Log.d(TAG,"closeMyAccessory exited!");
    }
    
    private boolean initA3P(){    
    	boolean a3pInited = false;
    	if(a3pSession != null && a3pSession.isConnected()){
    		// Session is still connected, do nothing
    		Log.d(TAG, "Session still connected.  Not reconnecting.");
    		return true;
    	}
    	
    	// If we already have an A3PSession, close it before making a new one
    	// Also, clear out previous device ID list
    	if(a3pSession != null) {    		
    		Log.d(TAG, "Closing previous A3PSession!");
    		a3pSession.endConnection();
    		
    		while(!a3pSession.isClosed()){
				try {
					synchronized(this){
						this.wait(100);
	    			}
				} catch (InterruptedException e){
					// Do nothing if interrupted.
				}
			}
    		deviceIDs.clear();
    	}
    	    	
    	if(myParcelFD == null) {
    		
    		if(openMyAccessory()) {

    			// Start or restart an A3PSession 		
    			if(a3pSession != null){
    				Log.d(TAG, "Copying queues from old A3P Session!");
    				a3pSession = A3PSession.freshCopy(a3pSession, myParcelFD);
    			} else {
    				Log.d(TAG, "Starting first session!");
    				a3pSession = new A3PSession(this, myParcelFD);
    			}

    			// Attempt to initiate connection
    			a3pSession.startConnection();
    			a3pInited = true;
    		}
    		else {
    			Log.e(TAG, "Cannot create A3PSession. openMyAccessory failed!");
    			a3pInited = false;
    		}
    	}
    	return a3pInited;
    }
    
//    private boolean initAccessory() {
//    	boolean status = false;
//    	UsbAccessory[] accessories = androidUsbManager.getAccessoryList();
//    	adkAccessory = (accessories == null ? null : accessories[0]);
//    	
//    	if(adkAccessory != null) {
//    		Log.e(TAG, "Found " + accessories.length + " accessories.");    		
//    		
//    		if (!androidUsbManager.hasPermission(adkAccessory)) {    			
//    			androidUsbManager.requestPermission(adkAccessory, usbAuthPendingIntent);
//    		}  
//    		
//    		status = openMyAccessory();
//    		
//    	} else {
//    		Log.e(TAG, "Found no accessories.");
//    	}
//    	return status;
//    }

	// Idempotent
    private void initThread(){
    	if(workerThread != null && workerThread.isAlive()){
    		Log.e(TAG, "Thread already running!");
    	} else {
	    	
			// Setup and start worker thread
			workerRun = true;
			workerThread = new Thread(null, this, "USB Manager Worker Thread");
			workerThread.start();
    	}
    }

	public void run() {
		String LOG_TAG = ArduinoSubChannel.TAG + "worker";
		
		while(workerRun){
			
			if(a3pSession != null){
				USBPayload nextPayload = a3pSession.getNextPayload();
				
				if(nextPayload == null){
					try{
						synchronized(this){
							this.wait(100);
		    			}
					} catch (InterruptedException e) {
						// Do nothing for now.
					}
				} else {
					
					// Process the payload here!
					if(DEBUG_VERBOSE){
						Log.d(LOG_TAG, "Received payload for sensorID:  " + nextPayload.getSensorID() );
					}
					
					// If this is a sensor enumeration packet, update our configuration information
					// Note:  this currently allows USBManager to be reconfigured while running
					//        which seems to make sense, since we're hoping to try to reconnect to
					//        the Arduino while live (although we may need a new A3PSession)
					//
					// We may later replace this processing with a virtual 'sensor 0'
					//
					if(nextPayload.getSensorID() == 0){
						Log.d(LOG_TAG, "We got a system control packet!");
						String payloadString = nextPayload.payloadAsString();
						if(payloadString.length() >= 1 && payloadString.charAt(0) == '!'){
							Log.d(LOG_TAG, "Processing identification string: " + payloadString);
							
							// Split the ID string on the ; character, ignoring the first character
							String[] ids = payloadString.substring(1).split(";");

							for(int i = 0; i < ids.length; i++){ 
								int temp = ids[i].indexOf(",");
								ids[i] = ids[i].substring(0, temp);
								try{
									ids[i] = "" + Long.parseLong(ids[i]);	// Trim off leading zeros or whitespace
								} catch (NumberFormatException e){
									Log.e(LOG_TAG,"Error parsing sensor id: " + ids[i]);
								}
								Log.d(LOG_TAG, "Found sensor id: " + ids[i]);
							}
							
							synchronized(deviceIDs) {
								deviceIDs.clear();
								deviceIDs.addAll(Arrays.asList(ids));
								Log.d(LOG_TAG, "notifying with devices " + deviceIDs.size());
								deviceIDs.notifyAll();								
							}																					
						}
					}
					else 
					{
						
						SensorDataPacket sdp;
						if(nextPayload.isReadingSeries()) {
							sdp = new SensorDataPacket(nextPayload.getRawBytes(), nextPayload.getSeriesTimestamp(), nextPayload.getNumOfReadingsInSeries());
						} else {
							sdp = new SensorDataPacket(nextPayload.getRawBytes(), nextPayload.getAndroidTimeStamp());
						}
						mSensorManager.addSensorDataPacket(String.valueOf(nextPayload.getSensorID()),sdp);
					}
				}
			}
		}
	}
	
	public void sensorConnect(String id) throws SensorNotFoundException {	
					
		if(!deviceIDs.contains(id)) {			
			if (mSensorManager.getSensor(id) == null) {
				throw new SensorNotFoundException(
				"Sensor not found in sensor manager");
			}
			searchForSensors();	
		}
		
		//check deviceIDs again (if previous check resulted in discovery & we got new devices) 
		if(!deviceIDs.contains(id)) {
			throw new SensorNotFoundException("Sensor ID: " + id + " not found");
		}
		
		mSensorManager.updateSensorState(id, DetailedSensorState.CONNECTED);		
	}

	public void sensorDisconnect(String id) throws SensorNotFoundException {
		
//		if(deviceIDs == null || !deviceIDs.contains(id)) {
//			Sensor sensor = mSensorManager.getSensor(id);
//			if (sensor == null) {
//				throw new SensorNotFoundException(
//				"Sensor not found in sensor manager");
//			}
//			searchForSensors();	
//		}
//		
//		//check deviceIDs again (if previous check resulted in discovery & we got new devices) 
//		if(deviceIDs == null || !deviceIDs.contains(id)) {
//			throw new SensorNotFoundException("Sensor ID: " + id + " not found");
//		}
		
		if(mSensorManager.getSensor(id) != null)			
			mSensorManager.updateSensorState(id, DetailedSensorState.DISCONNECTED);		
	}

	public void sensorWrite(String id, byte[] message) {		
		
		boolean sessionInited = true;
		
		if(id.equals(A3P_DEVICE_ID) || (mSensorManager.getSensor(id) != null)) {
			// TODO: Discuss whether reviving automatically is good or bad.

			// Try to revive a dead a3pSession before proceeding
			if(a3pSession == null || a3pSession.isClosed()) {
				sessionInited = initA3P();
			}
			if(sessionInited) 
			{
				long ts = Calendar.getInstance().getTimeInMillis();
				USBPayload payload = new USBPayload(message, ts, Long.parseLong(id), false);
				try {
					a3pSession.enqueuePayloadToSend(payload);   					
				}
				catch (IllegalStateException e){
					Log.e(TAG, "Connection is currently closed!  Packet cannot be enqueued!");
					e.printStackTrace();
				}
			}
			else {
				Log.e(TAG, "initA3P failed!  Packet cannot be enqueued!");
			}
		}

		else {
			Log.d(TAG, "SensorID: " + id + " unknown. Packet cannot be enqueued!");
		}
	}

	public void searchForSensors() {		
		Log.e(TAG, "Searching for sensors!");
		initializeSensors();
	}

	public void startSensorDataAcquisition(String id, byte[] command) {
		ODKSensor sensor = mSensorManager.getSensor(id);
		if(sensor != null) {
			sensor.dataBufferReset();
			sensorWrite(id,command);
		}
		else {
			Log.d(TAG, "SensorID: " + id + " unknown. cannot start sensor");
		}				
	}

	public void stopSensorDataAcquisition(String id, byte[] command) {
		if(mSensorManager.getSensor(id) != null) {
			sensorWrite(id,command);		
		}
		else {
			Log.d(TAG, "SensorID: " + id + " unknown. cannot stop sensor");
		}		
	}

	public synchronized void removeAllSensors() {
		Log.d(TAG, " in removeAllSensors discovered list size: " + mDiscoverableDeviceList.size());
		if (mDiscoverableDeviceList != null)
		{
			Iterator <DiscoverableDevice>discoverableDeviceIterator = mDiscoverableDeviceList.iterator();
			while (discoverableDeviceIterator.hasNext())
				discoverableDeviceIterator.next().connectionLost();
		}
		mDiscoverableDeviceList = new ArrayList <DiscoverableDevice>();
	}
}
