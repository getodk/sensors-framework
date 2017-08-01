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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.opendatakit.sensors.exception.IdNotFoundException;
import org.opendatakit.sensors.manager.DiscoverableDeviceState;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorsAdapter extends BaseAdapter {

	private final List<ISensorSelectorItem> mSelectorItems;
	private final Context mContext;

	/**
	 * Constructor. Save Off Context And List.
	 * @param context Application Context
	 * @param items List Of SensorSelectorItems
	 */
	public SensorsAdapter(Context context, List<ISensorSelectorItem> items) {
		mContext = context;
		mSelectorItems = items;
	}

	/**
	 * Get count of sensor items
	 * @return sensor count
	 */
	public int getCount() {
		if (mSelectorItems != null)
			return mSelectorItems.size();
		else
			return 0;
	}

	/**
	 * Get sensor item
	 * @param position sensor item position
	 * @return sensor item
	 */
	public Object getItem(int position) {
		if (mSelectorItems != null)
			return mSelectorItems.get(position);
		else
			return null;
	}

	/**
	 * Get the row id associated with the specified position in the list.
     *
	 * @param position sensor item position
	 * @return item id
	 */
	public long getItemId(int position) {
		return position;
	}

	/**
	 * Check if all items are enabled
	 * @return true if all enabled
	 */
	public boolean areAllItemsEnabled() {
		return true;
	}

	/**
	 * Get sensor name
	 * @param position sensor item position
	 * @return sensor name
	 */
	public String getName(int position) {
		if (mSelectorItems != null)
			return mSelectorItems.get(position).getName();
		else
			return "";
	}

	/**
	 * Returns the sensor id for the item at the specified position
	 * @param position sensor item position
	 */
	public String getId(int position) {

		if (mSelectorItems != null) {
			ISensorSelectorItem ss = mSelectorItems.get(position);
			return ss.getSensorId();
		} else
			return "";
	}

	/**
	 * Return the view at the specified position
	 * @param position sensor item position
	 * @param convertView convert view
	 * @param parent parent view
	 */
	public View getView(int position, View convertView, ViewGroup parent) {
		ISensorSelectorItem ssi;
		ssi = mSelectorItems.get(position);

		if (ssi != null)
			return ssi.getView(mContext, convertView, parent);
		else
			return null;
	}

	/**
	 * Return the state of the sensor at specified position
	 * @param position sensor item position
	 */
	public DiscoverableDeviceState getSensorState(int position) {
		ISensorSelectorItem ssi;
		ssi = mSelectorItems.get(position);

		if (ssi != null)
			return ssi.getSensorState();
		else
			return null;
	}
	
	public void addSensor(ISensorSelectorItem sensor) {
		mSelectorItems.add(sensor);
		// Resort The List
		Collections.sort(mSelectorItems, mListComparitor);
	}
	
	/**
	 * Handle state change broadcast from service
	 * @param id sensor id
	 * @param state sensor state
	 * @param name TODO
	 */
	public void sensorStateChange(String id, DiscoverableDeviceState state, String name) throws IdNotFoundException {
		Iterator<ISensorSelectorItem> it = mSelectorItems.iterator();
		ISensorSelectorItem is = null;
		ISensorSelectorItem sensor = null;

		// Search Through mSensors And See If This Sensor Is Already In The List
		while (it.hasNext()) {
			is = it.next();
			if (is.getSensorId().compareTo(id) == 0) {
				sensor = is;
			}
		}

		// If Sensor Does Not Exists, Create It. Otherwise Update Its State
		if (sensor == null) {
			throw new IdNotFoundException();
		} else {
			sensor.updateState(state);
		}

		// Resort The List
		Collections.sort(mSelectorItems, mListComparitor);
	}

	/**
	 * Return true if the sensor at the specified position is ready to record
	 * @param position
	 * @return true if ready, false otherwise
	 */
	public boolean sensorIsReady(int position) {
		return (mSelectorItems.get(position).getUiStep() == BaseSensorSelectorItem.DisplayIcon.READY_TO_RECORD);
	}

	/**
	 * Return true if the sensor is out of range and registered
	 * @param position
	 * @return true if out of range, false otherwise
	 */
	public boolean sensorIsOutOfRange(int position) {
		return (mSelectorItems.get(position).getUiStep() == BaseSensorSelectorItem.DisplayIcon.REGISTERED_OUT_OF_RANGE);
	}

	/**
	 * Return true if the sensor needs to be registered
	 * @param position
	 * @return true if requires registration
	 */
	public boolean sensorIsUnregistered(int position) {
		return (mSelectorItems.get(position).getUiStep() == BaseSensorSelectorItem.DisplayIcon.NEED_TO_REGISTER);
	}

	/**
	 * Return true if the sensor needs to be paired
	 * @param position
	 * @return true if pairing required
	 */
	public boolean sensorIsUnpaired(int position) {
		return (mSelectorItems.get(position).getUiStep() == BaseSensorSelectorItem.DisplayIcon.NEED_TO_PAIR);
	}

	/**
	 * Comparison object for sensor list items. Sorts by sensor state
	 */
	private static final Comparator<ISensorSelectorItem> mListComparitor = new Comparator<ISensorSelectorItem>() {
		
		/**
		 * Compare items
		 * @param item1 sensor item 1
		 * @param item2 sensor item 2
		 * @return 1 if item 1 greater than item 2, 0 if equal, -1 if less thans
		 */
		public int compare(ISensorSelectorItem item1, ISensorSelectorItem item2) {
			if (item1.getUiStep() == item2.getUiStep())
				return 0;
			if (item1.getUiStep().ordinal() > item2.getUiStep().ordinal())
				return 1;
			else
				return -1;

		}
	};

}
