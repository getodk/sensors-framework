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


import org.opendatakit.sensors.DriverType;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.manager.SensorNotFoundException;

import java.util.List;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public interface USBCommSubChannel {
	
	public void initializeSensors();
	
	// Search for available sensors
	public void searchForSensors();

	public List<DiscoverableDevice> getDiscoverableSensor();

	// Register sensor
	public boolean sensorRegister(String id, DriverType sensorDriver, String appName);
	
	// Connect to sensor
	public void sensorConnect(String id) throws SensorNotFoundException;
	
	// Disconnect from sensor
	public void sensorDisconnect(String id) throws SensorNotFoundException;
	
	//start getting data from sensor
	public void startSensorDataAcquisition(String id, byte[] command);
	
	//stop getting data from sensor
	public void stopSensorDataAcquisition(String id, byte[] command);
	
	// Write data to sensor
	public void sensorWrite(String id, byte[] message);
	
	public void removeAllSensors();
	
	public void shutdown();
}