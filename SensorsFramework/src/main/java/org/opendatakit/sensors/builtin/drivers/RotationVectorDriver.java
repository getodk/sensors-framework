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
public class RotationVectorDriver extends AbstractBuiltinDriver {

	private static final String X_SIN = "x-sin";
	private static final String Y_SIN = "y-sin";
	private static final String Z_SIN = "z-sin";
	private static final String COS = "cos";

	public RotationVectorDriver() {
		sensorParams.add(new SensorParameter(X_SIN, SensorParameter.Type.FLOAT, SensorParameter.Purpose.DATA, "Rotation vector component along the x axis (x * sin(θ/2))."));
		sensorParams.add(new SensorParameter(Y_SIN, SensorParameter.Type.FLOAT, SensorParameter.Purpose.DATA, "Rotation vector component along the y axis (y * sin(θ/2))."));
		sensorParams.add(new SensorParameter(Z_SIN, SensorParameter.Type.FLOAT, SensorParameter.Purpose.DATA, "Rotation vector component along the z axis (z * sin(θ/2))."));
		sensorParams.add(new SensorParameter(COS, SensorParameter.Type.FLOAT, SensorParameter.Purpose.DATA, "Scalar component of the rotation vector ((cos(θ/2))."));
	}
	
	@Override
	public SensorDataParseResponse getSensorData(long maxNumReadings,
			List<SensorDataPacket> rawSensorData, byte[] remainingData) {

		List<Bundle> sensorData = new ArrayList<Bundle>();
		for (SensorDataPacket pkt : rawSensorData) {
			String tmp = new String(pkt.getPayload());
			if (tmp != null) {
				String[] values = tmp.split("\n");
				if (values.length >= 3) {
					Bundle data = new Bundle();
					data.putFloat(X_SIN, Float.valueOf(values[0]));
					data.putFloat(Y_SIN, Float.valueOf(values[1]));
					data.putFloat(Z_SIN, Float.valueOf(values[2]));
					if (values.length >= 4) {
						data.putFloat(COS, Float.valueOf(values[3]));
					}
					sensorData.add(data);
				}
			}

		}
		return new SensorDataParseResponse(sensorData, remainingData);
	}
}