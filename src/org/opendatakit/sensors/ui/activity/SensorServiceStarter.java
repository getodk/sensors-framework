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
package org.opendatakit.sensors.ui.activity;

import java.util.Iterator;
import java.util.List;

import org.opendatakit.sensors.SensorsSingleton;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.usb.USBDiscoverableDevice;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * SensorServiceStarter starts the SensorService when an adk accessory is plugged in. 
 * It responds to android.hardware.usb.action.USB_ACCESSORY_ATTACHED events. 
 * @author rohitc
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorServiceStarter extends Activity {
	
	@SuppressWarnings("rawtypes")
	private static final Class targetClass = org.opendatakit.sensors.service.SensorService.class;
	
	private static final String TAG = "SensorServiceStarter";
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
	@Override
	public void onResume() {
		super.onResume();
		
		
		if(serviceRunning()) {
			Log.d(TAG,"SensorService already started, checking for ADK board!");
			
			List<DiscoverableDevice> devList = SensorsSingleton.getUSBManager().getDiscoverableSensor();
			
			if(devList == null){
				Log.e(TAG, "No sensor list received!");
			} else 
			{
				
				String totalList = "";
				Iterator<DiscoverableDevice> deviceIterator = devList.iterator();
				while(deviceIterator.hasNext())
				{
					USBDiscoverableDevice newDevice = (USBDiscoverableDevice) deviceIterator.next();
					if (newDevice.connectionLost == false)
					{
						totalList += newDevice.getDeviceId() + ";";
					}
				}
				
				Log.d(TAG, "Got list: " + totalList);
				
			}
			
		} else {
			Log.d(TAG,"SensorService not running, starting it now!");
			Intent start_intent = new Intent(this, targetClass);
			startService(start_intent);
		}
        
        finish();
	}
	
	// 'is my service running?' code from user geekQ on stackoverflow at:
	// http://stackoverflow.com/questions/600207/android-check-if-a-service-is-running
	private boolean serviceRunning() {
		
		Log.d(TAG, "Searching for class: " + targetClass.getName());
		
		ActivityManager theManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
		for(RunningServiceInfo s : theManager.getRunningServices(Integer.MAX_VALUE)){
			if(s.service.getClassName().equals(targetClass.getName())){
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void onStop(){
		super.onStop();
	}
}
