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


/**
 * android:name attribute tags of metadata elements present in 
 * sensor driver app's manifest file
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class ManifestMetadata {
	public static final String ODK_FRAMEWORK_VERSION = "ODK_sensors_version"; 
	public static final String DRIVER_COMMUNICATION_CHANNEL = "ODK_sensors_commChannel";
	public static final String DRIVER_TYPE = "ODK_sensors_driverType";
	public static final String DRIVER_IMPL_CLASSNAME = "ODK_sensors_driverImplClassname";
	public static final String DRIVER_ADDRESS = "ODK_sensors_address";
	public static final String DRIVER_PACKAGE = "ODK_sensors_package";
	public static final String DRIVER_READ_UI = "ODK_sensors_read_ui";
	public static final String DRIVER_CONFIG_UI = "ODK_sensors_config_ui";
	
	public static final String FRAMEWORK_VERSION_2 = "V2";
	public static final String FRAMEWORK_VERSION_3 = "V3";
	
	public static final String RETURN_ADDRESS = "ODK_sensors_return_address";
	public static final String TABLE_DEFINITION = "ODK_sensors_table_definition";
}
