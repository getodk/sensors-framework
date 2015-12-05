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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorServiceProxy implements ServiceConnection {
	
	private ODKSensorService sensorSvcProxy;
	private String TAG = "MiddlewareProxy"; 
	protected Context componentContext;
	protected final AtomicBoolean isBoundToService = new AtomicBoolean(false);
	
	public SensorServiceProxy(Context context) {
		this(context, "org.opendatakit.sensors", "org.opendatakit.sensors.service.SensorService");
	}
	
	public SensorServiceProxy(Context context, String frameworkPackage, String frameworkService) {
		componentContext = context;
		Intent bind_intent = new Intent();
		bind_intent.setClassName(frameworkPackage, frameworkService);
        componentContext.bindService(bind_intent, this, Context.BIND_AUTO_CREATE);
	}
	
	public void shutdown() {
		Log.d(TAG, "Application shutdown - unbinding from SensorService");
		if(isBoundToService.get()) {
			try {
				componentContext.unbindService(this);
				isBoundToService.set(false);
				Log.d(TAG,"unbound to sensor service");
			}
			catch(Exception ex) {
				Log.d(TAG,"onDestroy threw exception");
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.d(TAG,"Bound to sensor service");
		sensorSvcProxy = ODKSensorService.Stub.asInterface(service);
		isBoundToService.set(true);
	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		Log.d(TAG,"unbound to sensor service");
		isBoundToService.set(false);
	}
	
	public void sensorConnect(String id, String appForDatabase) throws RemoteException{
		try {
			sensorSvcProxy.sensorConnect(id, appForDatabase);
		}
		catch(RemoteException rex) {
			rex.printStackTrace();
			throw rex;
		}
	}
	
	public List<Bundle> getSensorData(String id, long numSamples) throws RemoteException{
		List<Bundle> data = null;
		try {
			data = sensorSvcProxy.getSensorData(id, numSamples);
		}
		catch(RemoteException rex) {
			rex.printStackTrace();
			throw rex;
		}
		return data;
	}
	
	public void sendDataToSensor(String id, Bundle dataToSend) throws RemoteException {
		try {
			sensorSvcProxy.sendDataToSensor(id, dataToSend);
		}
		catch(RemoteException rex) {
			rex.printStackTrace();
			throw rex;
		}
	}
	
	// Configure
	public void configure(String id, String setting, Bundle params) throws RemoteException{
			try {
				sensorSvcProxy.configure(id, setting, params);
			} catch (RemoteException e) {
				e.printStackTrace();
				throw e;
			}
	}
	
	// Start
	public boolean startSensor(String id) throws RemoteException{
		try {
			return sensorSvcProxy.startSensor(id);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	// Stop
	public boolean stopSensor(String id) throws RemoteException{
		try {
			return sensorSvcProxy.stopSensor(id);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public boolean isConnected(String id) throws RemoteException {
		try {
			return sensorSvcProxy.isConnected(id);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
			
		}
	}
	
	public boolean isBusy(String id) throws RemoteException{
		try {
			return sensorSvcProxy.isBusy(id);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public boolean hasSensor(String id) throws RemoteException{
		try {
			return sensorSvcProxy.hasSensor(id);
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void removeAllSensors() throws RemoteException{
		try {
			sensorSvcProxy.removeAllSensors();
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public boolean isBoundToService() {
		return isBoundToService.get();
	}

}
