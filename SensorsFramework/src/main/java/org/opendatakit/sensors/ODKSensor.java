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

import org.opendatakit.sensors.manager.SensorNotFoundException;

import android.os.Bundle;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public interface ODKSensor {

	public abstract void connect(String appForDatabase)
			throws SensorNotFoundException;

	public abstract CommunicationChannelType getCommunicationChannelType();

	public abstract void disconnect() throws SensorNotFoundException;

	/**
	 * Sends a message to the sensor to configure it.
	 * @param setting TODO
	 * @param configData Data for configuration.
	 */
	public abstract void configure(String setting, Bundle params)
			throws ParameterMissingException;

	public abstract List<Bundle> getSensorData(long maxNumReadings);

	public abstract void sendDataToSensor(Bundle dataToEncode);

	public abstract boolean startSensor();

	public abstract boolean stopSensor();

	public abstract String getSensorID();

	/**
	 * Adds a new data sample packet for a sensor. 
	 * @param packet
	 *            sensor data packet
	 */
	public abstract void addSensorDataPacket(SensorDataPacket packet);

	/**
	 * Deletes any existing temporary buffers for a sensor. This should be
	 * called by a sensor prior to be activated and buffering data to clear any
	 * previous data.
	 * 
	 */
	public abstract void dataBufferReset();

	public abstract void shutdown() throws SensorNotFoundException;

	public abstract String getReadingUiIntentStr();

	public abstract String getConfigUiIntentStr();
	
	public abstract String getAppNameForDatabase();

	public abstract boolean hasReadingUi();

	public abstract boolean hasConfigUi();
	
	public abstract boolean hasAppNameForDatabase();

}