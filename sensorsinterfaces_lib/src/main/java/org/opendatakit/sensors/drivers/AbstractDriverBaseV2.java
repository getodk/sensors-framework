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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.sensors.Driver;
import org.opendatakit.sensors.ParameterMissingException;
import org.opendatakit.sensors.SensorDataPacket;
import org.opendatakit.sensors.SensorDataParseResponse;
import org.opendatakit.sensors.SensorParameter;

import android.os.Bundle;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public abstract class AbstractDriverBaseV2 implements Driver {
	
	protected List<SensorParameter> sensorParams = new ArrayList<SensorParameter>();
	
	/*
	 * (non-Javadoc)
	 * @see org.opendatakit.sensors.Driver#getSensorData(long, java.util.List, byte[])
	 */
	@Override
	public abstract SensorDataParseResponse getSensorData(long maxNumReadings, List<SensorDataPacket> rawSensorData, byte [] remainingData);
	
	/*
	 * (non-Javadoc)
	 * @see org.opendatakit.sensors.Driver#configureCmd(java.lang.String, android.os.Bundle)
	 */
	@Override
	public byte[] configureCmd(String setting, Bundle config) throws ParameterMissingException {
		return new byte[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.opendatakit.sensors.Driver#getSensorDataCmd()
	 */
	@Override
	public byte[] getSensorDataCmd() {
		return new byte[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.opendatakit.sensors.Driver#startCmd()
	 */
	@Override
	public byte[] startCmd() {
		return new byte[0];
	}

	/*
	 * (non-Javadoc)
	 * @see org.opendatakit.sensors.Driver#stopCmd()
	 */
	@Override
	public byte[] stopCmd() {
		return new byte[0];
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.opendatakit.sensors.Driver#sendDataToSensor(android.os.Bundle)
	 */
	@Override
	public byte[] sendDataToSensor(Bundle dataToFormat) {
		return new byte[0];
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.opendatakit.sensors.Driver#getDriverParameters()
	 */
	@Override
	public List<SensorParameter> getDriverParameters() {
		return sensorParams;
	}
	
}
