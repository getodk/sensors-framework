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

import java.util.List;

import android.os.Bundle;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public interface Driver {
	
	/*
	 * Get the sensor-specific start command. 
	 * return 0-length buffer if sensor does not have a start command
	 */
	byte[] startCmd();

	/*
	 * Get the sensor-specific stop command. 
	 * return 0-length buffer if sensor does not have a start command
	 */
	byte[] stopCmd();
	
	/*
	 * Get the sensor-specific command buffer to configure parameters. 
	 * return 0-length buffer if sensor does not have a configure command
	 * @param setting: The parameter to be configured
	 * @param config: Data for configuration.
	 */
	byte [] configureCmd(String setting, Bundle config) throws ParameterMissingException;
	
	/*
	 * Get the list of driver parameters 
	 */
	List<SensorParameter> getDriverParameters();
	
	/*
	 * Return raw sensor data parsed into key-value pairs in the Bundle contained in SensorDataParseResponse
	 * @param maxNumReadings: The max number of readings to return
	 * @param rawSensorData: buffer containing raw sensor data
	 * @param remainingData: buffer containing unprocessed raw sensor data from a previous call to this method
	 * @return SensorDataParseResponse: response from this method. SensorDataParseResponse has a Bundle containing parsed
	 * sensor data and a buffer containing any raw sensor data that was not processed during the method invocation
	 *  
	 */	
	SensorDataParseResponse getSensorData(long maxNumReadings, List<SensorDataPacket> rawSensorData, byte [] remainingData);
	
	/*
	 * Get a buffer containing data to send to a sensor. The buffer is formatted in a sensor-specific format
	 * @parameter dataToFormat: key-value pairs containing data to send to a sensor.  
	 */
	
	byte[] sendDataToSensor(Bundle dataToFormat);	
	
	/*
	 * Some sensors (e.g. WaterUse sensor) return data only when queried
	 * This method returns the command buffer needed to retrieve data from the sensor
	 * return 0-length buffer if a query command is not needed for the sensor
	 * @deprecated: sendDataToSensor provides a more general way to do this.
	 */
	byte [] getSensorDataCmd();		
	
}
