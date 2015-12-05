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

import org.opendatakit.sensors.DataSeries;
import org.opendatakit.sensors.ParameterMissingException;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class USBParamUtil {

	public static byte[] createOneByteMsg(String cmd, byte value)
			throws ParameterMissingException {
		byte[] valueArray = new byte[1];
		valueArray[0] = (byte) (value & 0xff);
		return createMsg(cmd, valueArray);
	}

	public static byte[] createMsg(String cmd, byte[] value)
			throws ParameterMissingException {
		if (cmd.length() != 2) {
			throw new ParameterMissingException("Command MUST bs 2 characters");
		}

		int valueSize = 0;
		if (value != null) {
			if (value.length > 255) {
				throw new ParameterMissingException(
						"Param Value cannot be larger than 255 bytes");
			}
			valueSize = value.length;
		}

		byte[] payload = new byte[4 + valueSize];

		payload[0] = DataSeries.CONFIGURE_SENSOR;
		payload[1] = (byte) cmd.charAt(0);
		payload[2] = (byte) cmd.charAt(1);
		payload[3] = (byte) valueSize;
		for (int i = 0; i < valueSize; i++) {
			payload[4 + i] = value[i];
		}

		return payload;
	}

	public static byte[] createSamplingRateMsg(int rate)
			throws ParameterMissingException {
		byte [] payload = new byte[6];
		payload[0] = DataSeries.CONFIGURE_SENSOR;
		payload[1] = 'S';
		payload[2] = 'R';
		payload[3] = 2;
		payload[4] = (byte) ((rate >> 8) & 0xff);
		payload[5] = (byte) (rate & 0xff);
		
		return payload;
	}

	public static byte[] createReadRateMsg(int rate)
			throws ParameterMissingException {
		return createOneByteMsg("RR", (byte) (rate & 0xff));
	}

	public static byte[] createAlertThresholdMsg(int threshold) {

		byte[] payload = new byte[8];
		payload[0] = DataSeries.CONFIGURE_SENSOR;
		payload[1] = 'A';
		payload[2] = 'T';
		payload[3] = 4;
		payload[4] = (byte) ((threshold >> 24) & 0xff);
		payload[5] = (byte) ((threshold >> 16) & 0xff);
		payload[6] = (byte) ((threshold >> 8) & 0xff);
		payload[7] = (byte) (threshold & 0xff);

		return payload;
	}
}
