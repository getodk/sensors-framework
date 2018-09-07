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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.sensors.drivers.IODKSensorDriver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class GenericDriverProxy implements ServiceConnection, DriverCommunicator {

	public static final String TAG = "GenericDriverProxy";
	private Context componentContext;
	private IODKSensorDriver sensorDriverProxy;
	private boolean isBoundToService;

	public GenericDriverProxy(String packageName, String className,
			Context context) {
		componentContext = context;

		Intent bind_intent = new Intent();
		// XXX make sure classname used in the intent is the fully qualified
		// class name
		Log.d(TAG,"binding to sensor driver: pkg: " + packageName + " className: "+ className);
		bind_intent.setClassName(packageName, className);
		componentContext.bindService(bind_intent, this,
				Context.BIND_AUTO_CREATE);
	}


	@Override
	public void shutdown() {
		if(isBoundToService) {
			try {
				componentContext.unbindService(this);
			}
			catch(Exception ex) {
				Log.d(TAG,"shutdown threw exception");
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.d(TAG, "Bound to SensorDriver");
		sensorDriverProxy = IODKSensorDriver.Stub.asInterface(service);
		isBoundToService = true;
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		Log.d(TAG, "unbound to sensor driver");
		isBoundToService = false;
	}

	// dummy impl of getSensorData in V2
	public List<Bundle> getSensorData(long maxNumReadings) {
		return new ArrayList<Bundle>();
	}

	@Override
	public SensorDataParseResponse getSensorData(long maxNumReadings,
			List<SensorDataPacket> rawSensorData, byte [] remainingData) {

		if (isBoundToService) {
			try {
//				Log.d(TAG, " calling getSensorDataV2");
				SensorDataParseResponse sdp = sensorDriverProxy.getSensorDataV2(maxNumReadings,
						rawSensorData, remainingData);
				return sdp;
			} catch (RemoteException rex) {
				rex.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public byte[] sendDataToSensor(Bundle dataToFormat) {
		byte [] encodedSensorData = null;
		if (isBoundToService) {
			try {
				encodedSensorData = sensorDriverProxy.encodeDataToSendToSensor(dataToFormat);
			} catch (RemoteException rex) {
				rex.printStackTrace();
			}
		}
		return encodedSensorData;
	}

	@Override
	public byte[] configureCmd(String setting, Bundle params) throws ParameterMissingException {
		byte[] cmd = null;
		if (isBoundToService) {
			try {
				cmd = sensorDriverProxy.configureCmd(setting, params);
			} catch (RemoteException rex) {
				rex.printStackTrace();
			}
		}
		return cmd;
	}

	@Override
	public byte[] getSensorDataCmd() {
		byte[] cmd = null;
		if (isBoundToService) {
			try {
				cmd = sensorDriverProxy.getSensorDataCmd();
			} catch (RemoteException rex) {
				rex.printStackTrace();
			}
		}
		return cmd;
	}

	@Override
	public byte[] startCmd() {
		byte[] cmd = null;
		if (isBoundToService) {
			try {
				cmd = sensorDriverProxy.startCmd();
			} catch (RemoteException rex) {
				rex.printStackTrace();
			}
		}
		return cmd;
	}

	@Override
	public byte[] stopCmd() {
		byte[] cmd = null;
		try {
			cmd = sensorDriverProxy.stopCmd();
		} catch (RemoteException rex) {
			rex.printStackTrace();
		}
		return cmd;
	}


	@Override
	public List<SensorParameter> getDriverParameters() {
		List<SensorParameter> list = null;
		try {
			list = sensorDriverProxy.getDriverParameters();
		} catch (RemoteException rex) {
			rex.printStackTrace();
		}
		return list;
	}	
}
