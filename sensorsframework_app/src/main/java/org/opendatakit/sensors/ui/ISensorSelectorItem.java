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
package org.opendatakit.sensors.ui;

import org.opendatakit.sensors.manager.DiscoverableDeviceState;
import org.opendatakit.sensors.ui.BaseSensorSelectorItem.DisplayIcon;

import android.content.Context;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public interface ISensorSelectorItem extends Parcelable {
	
	/**
	 * Updates State Information For This Item
	 * @param ss Sensor State
	 */
	public void updateState(DiscoverableDeviceState ss);

	/**
	 * Get sensor id
	 * @return sensor id
	 */
	public String getSensorId();

	/**
	 * Get sensor state
	 * @return sensor state
	 */
	public DiscoverableDeviceState getSensorState();

	/**
	 * Get sensor name
	 * @return sensor name
	 */
	public String getName();

	/**
	 * Returns The View That Represents The SelectoItems State
	 * @param context Application Context
	 * @param convertView convert view
	 * @param parent parent view
	 * @return The View w/ Information Populated
	 */
	public View getView(Context context, View convertView, ViewGroup parent); 


	/**
	 * Returns The Sensors State In User Terms
	 * @return sensor display state
	 */
	public DisplayIcon getUiStep();
}
