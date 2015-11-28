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

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class DataSeries {
	public static final String SENSOR_ID = "sensorid";
	public static final String NUM_SAMPLES = "numSamples";
	public static final String SERIES_TIMESTAMP = "series-timestamp";
	public static final String DATA_AS_CSV = "csvData";
	public static final String SENSOR_TYPE = "sensortype";
	public static final String MSG_TYPE = "msgtype";
	public static final String SAMPLE = "sample";
	
	//msg types handled by sensors 
	public static byte CONFIGURE_SENSOR = 0x1;
	public static byte START_SENSOR = 0x2;
	public static byte STOP_SENSOR = 0x3;
}
