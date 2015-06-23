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
package org.opendatakit.sensors.bluetooth.ui;

import org.opendatakit.sensors.R;
import org.opendatakit.sensors.manager.DiscoverableDeviceState;
import org.opendatakit.sensors.ui.BaseSensorSelectorItem;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

//TODO: clean BT out of this code and optimize for USB

public class SensorBtSelectorItem extends BaseSensorSelectorItem implements Parcelable {

	/**
	 * Constructor -- Builds A New Selector Item -- This Data Is Broadcast As An
	 * Intent From The Service
	 * @param id Sensor ID
	 * @param ss Sensor State

	 */
	public SensorBtSelectorItem(String id, DiscoverableDeviceState ss, String name) {
		super(id, ss, name);
	}


	/**
	 * Returns The View That Represents The SelectoItems State
	 * @param context Application Context
	 * @param convertView convert view
	 * @param parent parent view
	 * @return The View w/ Information Populated
	 */
	public View getView(Context context, View convertView, ViewGroup parent) {

		// Inflate The View If Needed
		if (convertView == null) {
			convertView = (View) LayoutInflater.from(context).inflate(R.layout.selector_item,
					parent, false);
		}

		// Lookup Widgets In View
		TextView nameText = (TextView) convertView.findViewById(R.id.name_text);
		ImageView icon = (ImageView) convertView.findViewById(R.id.state_icon);

		// Set Widget Text Based Off Object Information
		nameText.setText(getName());

		// Set Icon Based Off Of UI Step / Sensor & Registration States
		switch (getUiStep()) {
		case READY_TO_RECORD:
			icon.setImageResource(R.drawable.ic_menu_mark);
			break;
		case REGISTERED_OUT_OF_RANGE:
			icon.setImageResource(R.drawable.ic_menu_report_image);
			break;
		case NEED_TO_REGISTER:
			icon.setImageResource(R.drawable.ic_menu_add);
			break;
		case NOT_A_SENSOR:
			icon.setImageResource(R.drawable.ic_menu_report_image);
			break;
		case NEED_TO_PAIR:
			icon.setImageResource(R.drawable.ic_menu_manage);
			break;
		default:
			break;
		}
		return convertView;
	}


	
	// ---------------------------------------------------------------------------------------------
	// SENSOR SELECTOR ITEM PARCELS
	// ---------------------------------------------------------------------------------------------
	
	/**
	 * Create Interface. Supports saving application state.
	 */
	public static final Creator<SensorBtSelectorItem> CREATOR = new Parcelable.Creator<SensorBtSelectorItem>() {

		/**
		 * Create sensor selector item from parcel
		 * @param id parcel
		 */
		public SensorBtSelectorItem createFromParcel(Parcel in) {
			return new SensorBtSelectorItem(in);
		}

		/**
		 * Create sensor selector item array
		 * @param size array size
		 * @return sensor selector item array
		 */
		public SensorBtSelectorItem[] newArray(int size) {
			return new SensorBtSelectorItem[size];
		}
	};


	/**
	 * Constructor sensor selector items from parcel
	 * @param in parcels
	 */
	private SensorBtSelectorItem(Parcel in) {
		super(in);
	}
}
