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
/*
 * Adapted from:
 * Health Conscious Developers
 * University of Washington - CSEP590 Smartphone Mobile Computing
 * 
 * Author: Matt Wright (miwright@cs.washington.edu)
 * Date: 2/10/11
 *  
 * Displays the login activity
 */

package org.opendatakit.sensors.bluetooth;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendatakit.sensors.ODKSensor;
import org.opendatakit.sensors.SensorDataPacket;
import org.opendatakit.sensors.manager.DetailedSensorState;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class BluetoothSensor extends Thread {

	// logging
	private static final String LOGTAG = "BluetoothSensor";

	// Bluetooth And IPC Booleans
	private final AtomicBoolean mIsActivated = new AtomicBoolean(false);
	private final AtomicBoolean mConnect = new AtomicBoolean(false);
	private final AtomicBoolean mAttachedStatus = new AtomicBoolean(false);
	private final AtomicBoolean mKillMe = new AtomicBoolean(false);
	private static final UUID UNIQUE_ID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// data
	private String mId;
	private ODKSensor mSensor;
	private BluetoothDevice mBtDevice;

	private BluetoothSocket sock = null;
	private InputStream sockReader = null;
	private BufferedWriter sockWriter = null;
	private BluetoothManager mBtManager;

	/**
	 * Sensor Constructor
	 * 
	 * @param sensor
	 *            TODO
	 * @param device
	 *            android bluetooth device representation
	 */
	public BluetoothSensor(ODKSensor sensor, BluetoothDevice device,
			BluetoothManager btManager) {
		mId = sensor.getSensorID();
		mSensor = sensor;
		mBtDevice = device;
		mBtManager = btManager;
	}

	public boolean paired() {
		return mBtDevice.getBondState() == BluetoothDevice.BOND_BONDED;
	}

	/**
	 * Attempt Connection To Sensor
	 */
	public void connect() {

		if (paired()) {
			// reconnect
			mConnect.set(true);
			if (this.isAlive() == false) {
				this.start();
			}
		} else {
			Log.e(LOGTAG, "Will Not Connect Unpaired || Unregistered Sensor "
					+ mId);
		}
	}

	/**
	 * Start Data Read
	 */
	public void activiate() {
		if (mConnect.get()) {
			mIsActivated.set(true);
			Log.d(LOGTAG, "Activated start data collection");
		} else {
			Log.e(LOGTAG, "Will Not Activate Unconnected Sensor ");
		}
	}

	/**
	 * Stop Data Read
	 */
	public void deactivate() {
		mIsActivated.set(false);
	}

	/**
	 * Cleanup Method For BluetoothSensor
	 */
	public void kill() {
		mKillMe.set(true);
		
//		thread.interrupt doesn't interrupt the blocked socket read
//		this.interrupt();
		
		helperResetConnection();		
	}

	public void write(String data) {
		try {
			Log.d(LOGTAG, "writing to sensor");
			sockWriter.write(data);
			sockWriter.flush();
			Log.d(LOGTAG, "wrote to sensor");
		} catch (Exception e) {
			Log.e(LOGTAG, "Exception during write", e);
			e.printStackTrace();
		}
	}

	/**
	 * Thread Main Loop, Handles Data Connection
	 */
	public void run() {
		Log.d(LOGTAG, "Sensor Connection Thread Created: " + mId);

		while (true) {
			// PROCESS KILL -- Cleanup Sockets & Threads
			if (mKillMe.get() == true) {
				helperResetConnection();
				break;
			}

			// HANDLE (RE)CONNECTING
			if (!mAttachedStatus.get()) {
				if (mConnect.get() == true) {
					try {
						// Get Reference To Local BluetoothAdapter
						BluetoothAdapter btAdapter = BluetoothAdapter
								.getDefaultAdapter();
						if(btAdapter.isDiscovering()) {
							btAdapter.cancelDiscovery();
						}

						// create socket
						sock = mBtDevice
								.createRfcommSocketToServiceRecord(UNIQUE_ID);
						
						if (sock != null) {
							// Open Connection To Sensor
							sock.connect();
							
							// create reader/writer
							sockReader = sock.getInputStream();
							sockWriter = new BufferedWriter(
									new OutputStreamWriter(
											sock.getOutputStream()), 8192);
						} else {
							helperResetConnection();
						}
					} catch (IOException e) {
						helperResetConnection();
					}

					if (sock == null || sockReader == null
							|| sockWriter == null) {
						helperResetConnection();
						continue;
					} else {
						// Update Connected State -- Update Type
						mSensor.dataBufferReset();
						mAttachedStatus.set(true);
						Log.d(LOGTAG,
								"Connection Thread - Sensor Connected.  Bluetooth Reader/Writer Created "
										+ mId);
						mBtManager.updateSensorStateInDb(this.mId,
								DetailedSensorState.CONNECTED);
					}
				}
			} else {

				// SENSOR ACTIVATED
				if (mIsActivated.get() == true) {
					if (readSensorData(sockReader) == false) {

						helperResetConnection();
					} else {
						// go for the next read
						continue;
					}
				}
			}
			try {
				Log.v(LOGTAG, "State: Connect=" + mConnect.get()
						+ " Activated=" + mIsActivated.get() + " Attached="
						+ mAttachedStatus.get());
				Thread.sleep(250); 
			} catch (InterruptedException e) {
				Log.e(LOGTAG, "Unable To Sleep In SensorConnection Thread");
			}

		}
	}

	private synchronized void helperResetConnection() {
		mAttachedStatus.set(false);
		try {
			if (sockReader != null) {
				sockReader.close();
				sockReader = null;
			}
			if (sockWriter != null) {
				sockWriter.close();
				sockWriter = null;
			}
			if (sock != null) {
				sock.close();
				sock = null;
			}
			Log.v(LOGTAG, "Connection Thread - Closed Sockets To Sensor: "
					+ mId);
		} catch (IOException e) {
			Log.d(LOGTAG,
					"Connection Thread - Unable To Close Sockets To Sensor: "
							+ mId);
		}
		mBtManager.updateSensorStateInDb(mId, DetailedSensorState.DISCONNECTED);
	}

	/**
	 * Reads Sensor Data And Passes To DataProvider
	 * 
	 * @param sockReader2
	 *            buffered reader
	 * @param id
	 *            sensor id
	 */
	private boolean readSensorData(InputStream sockReader2) {

		byte[] buff = new byte[1024];
		int cnt;

		try {
			cnt = sockReader2.read(buff);
			if (cnt != -1) {
//				Log.d(LOGTAG,cnt + " bytes rcvd on socket");
//				for(int i = 0; i <cnt; i++) {
//					Log.d(LOGTAG,buff[i] + "");
//				}
				byte[] sdpbuff = new byte[cnt];				
				System.arraycopy(buff, 0, sdpbuff, 0, cnt);
				SensorDataPacket sdp = new SensorDataPacket(sdpbuff,
						System.currentTimeMillis());
				mSensor.addSensorDataPacket(sdp);
			}
		} catch (IOException e) {
//			e.printStackTrace();
			return false;
		}
		return true;
	}

}
