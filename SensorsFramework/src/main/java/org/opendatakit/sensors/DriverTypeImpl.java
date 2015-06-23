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
public class DriverTypeImpl implements DriverType {

	private final String driverType;
	private final String packageName;
	private final String driverAddress;
	private final CommunicationChannelType communicationChannelType;
	private final String readingUiIntentStr;
	private final String configUiIntentStr;
	
	// Optional parameter for writing to a database
	private final String tableDefinition;
	
	// Created a new constructor 
	public DriverTypeImpl(String driverType, String packageName,
			String driverAddress, CommunicationChannelType communicationType, String readingUiIntentStr, String configUiIntentStr, String tableDefinition) {

		this.driverType = driverType;
		this.packageName = packageName;
		this.driverAddress = driverAddress;
		this.communicationChannelType = communicationType;
		this.readingUiIntentStr = readingUiIntentStr;
		this.configUiIntentStr = configUiIntentStr;
		this.tableDefinition = tableDefinition;
		
	}

	@Override
	public String getSensorType() {
		return driverType;
	}

	@Override
	public String getSensorPackageName() {
		return packageName;
	}

	@Override
	public String getSensorDriverAddress() {
		return driverAddress;
	}

	@Override
	public String toString() {
		return driverType;
	}

	@Override
	public CommunicationChannelType getCommunicationChannelType() {
		return communicationChannelType;
	}

	@Override
	public String getReadingUiIntentStr() {
		return readingUiIntentStr;
	}

	@Override
	public String getConfigUiIntentStr() {
		return configUiIntentStr;
	}
	
	@Override
	public String getTableDefinitionStr() {
		return tableDefinition;
	}

	
}