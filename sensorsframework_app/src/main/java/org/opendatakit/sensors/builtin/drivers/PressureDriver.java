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
public class PressureDriver extends AbstractBuiltinDriver {

	private static final String PRESSURE = "pressure";

	public PressureDriver() {
		sensorParams.add(new SensorParameter(PRESSURE, SensorParameter.Type.FLOAT, SensorParameter.Purpose.DATA, "Ambient air pressure."));
	}
	
	@Override
	public SensorDataParseResponse getSensorData(long maxNumReadings,
			List<SensorDataPacket> rawSensorData, byte[] remainingData) {

		List<Bundle> sensorData = new ArrayList<Bundle>();
		for (SensorDataPacket pkt : rawSensorData) {
			String tmp = new String(pkt.getPayload());
			if (tmp != null) {
				String[] values = tmp.split("\n");
				if (values.length >= 1) {
					Bundle data = new Bundle();
					data.putFloat(PRESSURE, Float.valueOf(values[0]));
					sensorData.add(data);
				}
			}
		}
		return new SensorDataParseResponse(sensorData, remainingData);
	}
}