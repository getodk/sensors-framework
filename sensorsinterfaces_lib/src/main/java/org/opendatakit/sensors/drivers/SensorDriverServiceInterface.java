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
package org.opendatakit.sensors.drivers;

import java.lang.reflect.Constructor;
import java.util.List;

import org.opendatakit.sensors.Driver;
import org.opendatakit.sensors.ParameterMissingException;
import org.opendatakit.sensors.SensorDataPacket;
import org.opendatakit.sensors.SensorDataParseResponse;
import org.opendatakit.sensors.SensorParameter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorDriverServiceInterface extends ODKSensorDriver.Stub {

	private static final String TAG = "SensorDriverSvcIf";

	private Driver sensorDriver;
	
	public SensorDriverServiceInterface(Context context)	
	{
		if(context == null)			
			Log.d(TAG,"passed in null context!");			
		try {		
			ApplicationInfo ai = 
					context.getPackageManager().getApplicationInfo(context.getPackageName(), 
					PackageManager.GET_META_DATA);
			String classNameStr  = ai.metaData.getString(ManifestMetadata.DRIVER_IMPL_CLASSNAME);
			Log.d(TAG, "class name read: " + classNameStr);
			
			Constructor<? extends Driver> driverConstructor;	
			Class<? extends Driver> driverClass = Class.forName(classNameStr).asSubclass(Driver.class);	
			
			driverConstructor = driverClass.getConstructor();
			sensorDriver = driverConstructor.newInstance();			
		}
		catch(Exception ex) {
			ex.printStackTrace();			
		}
	}
	

	public byte [] configureCmd (String setting, Bundle configInfo)
	{
		byte[] result;
		try
		{
			Log.d(TAG,"configuring");
			result = sensorDriver.configureCmd(setting, configInfo);
			Log.d(TAG,"configured");
		}
		catch (ParameterMissingException pme)
		{
			pme.printStackTrace();
			result = null;
		}
		return result;	
	}

	public byte[] getSensorDataCmd ()
	{
		return sensorDriver.getSensorDataCmd();
	}

	public SensorDataParseResponse getSensorDataV2(long maxNumReadings,List<SensorDataPacket> rawData, byte [] remainingBytes)
	{
//		Log.d(TAG," sensor driver get dataV2");
		return sensorDriver.getSensorData(maxNumReadings,rawData, remainingBytes);
	}
	
	@Override
	public byte[] encodeDataToSendToSensor(Bundle dataToFormat) throws RemoteException {		
		return sensorDriver.sendDataToSensor(dataToFormat);
	}

	public byte[] startCmd()
	{
		return sensorDriver.startCmd();
	}

	public byte[] stopCmd()
	{
		return sensorDriver.stopCmd();
	}


	@Override
	public List<SensorParameter> getDriverParameters() throws RemoteException {
		return sensorDriver.getDriverParameters();
	}	

}
