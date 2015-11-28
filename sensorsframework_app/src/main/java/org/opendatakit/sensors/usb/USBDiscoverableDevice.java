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

import org.opendatakit.sensors.ODKSensor;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.manager.DiscoverableDeviceState;
import org.opendatakit.sensors.manager.ODKSensorManager;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class USBDiscoverableDevice implements DiscoverableDevice {

	private String deviceID;
	public boolean connectionLost;
	private ODKSensorManager sensorManager;
	
	public USBDiscoverableDevice(String devID, ODKSensorManager sensorManager) {
		this.deviceID = devID;
		this.sensorManager = sensorManager;
	}
	
	@Override
	public String getDeviceId() {
		return this.deviceID;
	}

	@Override
	public String getDeviceName() {
		// Does not exist right now for USB, might exist in future
		return deviceID;
	}

	@Override
	public DiscoverableDeviceState getDeviceState() {
		if (this.connectionLost)
			return DiscoverableDeviceState.OUT_OF_RANGE;
		ODKSensor sensor = sensorManager.getSensor(deviceID);
		if(sensor != null)
			return DiscoverableDeviceState.REGISTERED;
		
		return DiscoverableDeviceState.NOT_REGISTERED;
	}

	@Override
	public void connectionLost() {
		this.connectionLost = true;
	}

	@Override
	public void connectionRestored() {
		this.connectionLost = false;
	}
	

}
