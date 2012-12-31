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
package org.opendatakit.sensors.builtin.drivers;

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.sensors.Driver;
import org.opendatakit.sensors.ParameterMissingException;
import org.opendatakit.sensors.SensorParameter;

import android.os.Bundle;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public abstract class AbstractBuiltinDriver implements Driver {

	protected List<SensorParameter> sensorParams = new ArrayList<SensorParameter>();
	
	@Override
	public byte[] configureCmd(String setting, Bundle config) throws ParameterMissingException {
		return null;
	}

	@Override
	public byte[] getSensorDataCmd() {
		return null;
	}

	@Override
	public byte[] startCmd() {
		return null;
	}

	@Override
	public byte[] stopCmd() {
		return null;
	}
	
	@Override
	public byte[] sendDataToSensor(Bundle dataToFormat) {
		return null;
	}	
	
	@Override
	public List<SensorParameter> getDriverParameters() {
		return sensorParams;
	}
	
}
