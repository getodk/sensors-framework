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

import android.content.Context;
import android.os.Parcel;
import android.view.View;
import android.view.ViewGroup;

// TODO: clean BT out of this code and optimize for USB

public abstract class BaseSensorSelectorItem implements ISensorSelectorItem {

	// Enumerates The States Of A Sensor From A User's Perspective
	public enum DisplayIcon {
		READY_TO_RECORD,
		REGISTERED_OUT_OF_RANGE,
		NEED_TO_REGISTER,
		NEED_TO_PAIR,
		NOT_A_SENSOR;
	}

	// sensor data
	private String mId;
	private DiscoverableDeviceState mSensorState;
	private String mName;
	
	/**
	 * Constructor -- Builds A New Selector Item -- This Data Is Broadcast As An
	 * Intent From The Service
	 * @param id Sensor ID
	 * @param ss Sensor State

	 */
	protected BaseSensorSelectorItem(String id, DiscoverableDeviceState ss, String name) {
		mId = id;
		mSensorState = ss;
		mName = name;
	}

	/**
	 * Constructor sensor selector items from parcel
	 * @param in parcels
	 */
	protected BaseSensorSelectorItem(Parcel in) {
		mId = in.readString();
		DiscoverableDeviceState state = DiscoverableDeviceState.valueOf(in.readString());
		mSensorState = state;
	}
	
	/**
	 * Updates State Information For This Item
	 * @param ss Sensor State
	 */
	public void updateState(DiscoverableDeviceState ss) {
		mSensorState = ss;
	}

	/**
	 * Get sensor id
	 * @return sensor id
	 */
	public String getSensorId() {
		return mId;
	}

	/**
	 * Get sensor state
	 * @return sensor state
	 */
	public DiscoverableDeviceState getSensorState() {
		return mSensorState;
	}

	/**
	 * Get sensor name
	 * @return sensor name
	 */
	public String getName() {
		return mName;
	}

	/**
	 * Returns The View That Represents The SelectoItems State
	 * @param context Application Context
	 * @param convertView convert view
	 * @param parent parent view
	 * @return The View w/ Information Populated
	 */
	public abstract View getView(Context context, View convertView, ViewGroup parent); 


	/**
	 * Returns The Sensors State In User Terms
	 * @return sensor display state
	 */
	public DisplayIcon getUiStep() {
		switch(mSensorState) {
		case REGISTERED:
			return DisplayIcon.READY_TO_RECORD;
		case PAIRED:
			return DisplayIcon.NEED_TO_REGISTER;
		case UNPAIRED:
			return DisplayIcon.NEED_TO_PAIR;		
		default:
			return DisplayIcon.NOT_A_SENSOR;
		}	
	}
	
	// ---------------------------------------------------------------------------------------------
	// SENSOR SELECTOR ITEM PARCELS
	// ---------------------------------------------------------------------------------------------
	

	/**
	 * Describe contents
	 * @return 0
	 */
	public int describeContents() {
		return 0;
	}

	/**
	 * Write sensor selector data to parcel
	 */
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mId);
		dest.writeString(mSensorState.name());
	}

}
