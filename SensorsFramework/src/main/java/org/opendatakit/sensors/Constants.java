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
public class Constants {
	// Extras for Intent
	public static String SENSOR_ID = "sensor_id";
	public static String SENSOR_STATE = "state";

	// Column Names
	public static final String KEY_ID = "_id";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_SENSOR_ID = "sensorid";
	public static final String KEY_SENSOR_TYPE = "sensortype";
	public static final String KEY_MSG_TYPE = "msgtype";
	public static final String KEY_DATA = "data";

	// Column indexes
	public static final int TIMESTAMP_COLUMN = 1;
	public static final int SENSORID_COLUMN = 2;
	public static final int SENSORTYPE_COLUMN = 3;
	public static final int MSGTYPE_COLUMN = 4;
	public static final int DATA_COLUMN = 5;
}
