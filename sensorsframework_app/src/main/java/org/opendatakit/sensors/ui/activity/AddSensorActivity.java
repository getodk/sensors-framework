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
package org.opendatakit.sensors.ui.activity;

import org.opendatakit.sensors.Constants;
import org.opendatakit.sensors.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class AddSensorActivity extends Activity {

	private static final int RESULT_OK_BT = 1;
	private static final int RESULT_OK_USB = 2;

	protected static final String LOGTAG = "AddSensorActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_sensor);
	}

	public void addUSBAction(View view) {
		// need to add a USB sensor
		Intent usbSensorSelectionIntent = new Intent(this,
				org.opendatakit.sensors.usb.ui.SensorUsbSelectionActivity.class);
		this.startActivityForResult(usbSensorSelectionIntent, RESULT_OK_USB);
	}

	public void addBluetoothAction(View view) {
		// need to add a BT sensor
		Intent btSensorSelectionIntent = new Intent(
				this,
				org.opendatakit.sensors.bluetooth.ui.SensorBtSelectionActivity.class);;
		this.startActivityForResult(btSensorSelectionIntent, RESULT_OK_BT);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Log.d(LOGTAG,"onActivityResult: resultCode: " + resultCode +
		// " requestCode: " + requestCode);
		switch (requestCode) {
		case RESULT_OK_USB:
			if (data != null) {
				Log.d(LOGTAG,
						"Finished discover USB sensor: "
								+ data.getStringExtra(Constants.SENSOR_ID));
				setResult(resultCode, data);
			}
			finish();
			break;
		case RESULT_OK_BT:
			// Finish this Activity by passing data from sensor select activity
			// back to application
			if (data != null) {
				Log.d(LOGTAG,
						"Finished discover bluetooth sensor: "
								+ data.getStringExtra(Constants.SENSOR_ID));
				setResult(resultCode, data);
			}
			finish();
			break;
		}
	}

}
