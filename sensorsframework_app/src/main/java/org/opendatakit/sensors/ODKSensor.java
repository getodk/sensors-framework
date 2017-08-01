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

import android.os.Bundle;
import org.opendatakit.sensors.manager.SensorNotFoundException;

import java.util.List;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public interface ODKSensor {

	public void connect()
			throws SensorNotFoundException;

	public CommunicationChannelType getCommunicationChannelType();

	public void disconnect() throws SensorNotFoundException;

	/**
	 * Sends a message to the sensor to configure it.
	 * @param setting TODO
	 */
	public void configure(String setting, Bundle params)
			throws ParameterMissingException;

	public List<Bundle> getSensorData(long maxNumReadings);

	public void sendDataToSensor(Bundle dataToEncode);

	public boolean startSensor();

	public boolean stopSensor();

	public String getSensorID();

	/**
	 * Adds a new data sample packet for a sensor. 
	 * @param packet
	 *            sensor data packet
	 */
	public void addSensorDataPacket(SensorDataPacket packet);

	/**
	 * Deletes any existing temporary buffers for a sensor. This should be
	 * called by a sensor prior to be activated and buffering data to clear any
	 * previous data.
	 * 
	 */
	public void dataBufferReset();

	public void shutdown() throws SensorNotFoundException;

	public String getReadingUiIntentStr();

	public String getConfigUiIntentStr();

	public boolean hasReadingUi();

	public boolean hasConfigUi();

	 public String getAppNameForDatabase();

	public boolean hasAppNameForDatabase();

	 public void setAppNameForDatabase(String appName);

	public boolean transferDataToDb();

	public void setDbTransfer(boolean transferToDb);
}