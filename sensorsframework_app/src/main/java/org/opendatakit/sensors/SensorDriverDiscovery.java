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

import java.util.ArrayList;
import java.util.List;

import org.opendatakit.sensors.drivers.ManifestMetadata;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorDriverDiscovery {

	private static final String LOGTAG = "SensorDriverDiscovery";
		
	public static List<DriverType> getAllDriversForChannel(Context context, CommunicationChannelType commChannelType1, String version) {
		
		List<DriverType> drivers = new ArrayList<DriverType>();
		PackageManager pkgManager = context.getPackageManager();
		List<ApplicationInfo> packages = pkgManager.getInstalledApplications(PackageManager.GET_META_DATA);

		for (ApplicationInfo packageInfo : packages) 
		{
			try
			{  
				if(packageInfo.metaData != null ) {  

					Bundle data = packageInfo.metaData; 
					
					String frameworkVersion = data.getString(ManifestMetadata.ODK_FRAMEWORK_VERSION);
					String driverCommChannel = data.getString(ManifestMetadata.DRIVER_COMMUNICATION_CHANNEL);
					String driverType = data.getString(ManifestMetadata.DRIVER_TYPE);
					String driverPackageName = packageInfo.packageName;
					String driverAddress = data.getString(ManifestMetadata.DRIVER_ADDRESS);
					String readUiIntentStr = data.getString(ManifestMetadata.DRIVER_READ_UI);
					String configUiIntentStr = data.getString(ManifestMetadata.DRIVER_CONFIG_UI);
					// This is an optional field used to write data into a database 
					String tableDefinition = data.getString(ManifestMetadata.TABLE_DEFINITION);
					
					if(frameworkVersion == null) {
						continue;
					}
					
					CommunicationChannelType commChannel = CommunicationChannelType.valueOf(driverCommChannel);
					
					// verify driver version & comm channel type is correct
					if(version.equals(frameworkVersion) && commChannelType1.equals(commChannel) && driverCommChannel != null && driverType != null && driverAddress != null) {
						Log.d(LOGTAG ,"Adding Driver for Package: "+ driverPackageName + "  Driver address: " + driverAddress);  
						DriverType driver = new DriverTypeImpl(driverType, driverPackageName, driverAddress, commChannel, readUiIntentStr, configUiIntentStr, tableDefinition);
						drivers.add(driver); 
					} else {
						Log.d(LOGTAG ,"NOT ADDING Driver for Package: "+ driverPackageName + "  Driver address: " + driverAddress);  
					}
				}
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}          
		}
		return drivers;
	}

}
