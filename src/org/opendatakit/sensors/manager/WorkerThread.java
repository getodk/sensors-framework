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
package org.opendatakit.sensors.manager;

import java.util.Iterator;
import java.util.List;

import org.opendatakit.sensors.Constants;
import org.opendatakit.sensors.DataSeries;
import org.opendatakit.sensors.ODKSensor;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class WorkerThread extends Thread {
	private static final String TAG = "WorkerThread";
	private boolean isRunning;
	private Uri uri;
	private ContentResolver resolver;
	private Context serviceContext;
	private ODKSensorManager sensorManager;

	public WorkerThread(Context context, ODKSensorManager manager, Uri contentProviderUri) {
		super("WorkerThread");
		isRunning = true;
		serviceContext = context;
		sensorManager = manager;
		resolver = serviceContext.getContentResolver();
		uri = contentProviderUri;
	}

	public void stopthread() {
		isRunning = false;
		this.interrupt();
	}

	@Override
	public void run() {
		Log.d(TAG, "worker thread started");		
		Log.e(TAG,"resolver.getType: " + resolver.getType(uri));
		
		while(isRunning) {

			

			try {
				
				for(ODKSensor sensor : sensorManager.getSensorsUsingContentProvider()) {
					moveSensorDataToCP(sensor);
				}
				
				Thread.sleep(300);
			}
			catch(InterruptedException iex) {
				iex.printStackTrace();
			}
		}
	}

	private void moveSensorDataToCP(ODKSensor aSensor) {		
		if (aSensor != null) {			
			List<Bundle> bundles = aSensor.getSensorData(0);//XXX for now this gets all data fm sensor			
			if(bundles == null) {
				Log.e(TAG,"WTF null list of bundles~");
			}
			else {
				Iterator<Bundle> iter = bundles.iterator();
				ContentValues values = new ContentValues();
				while(iter.hasNext()) {
					Bundle aBundle = iter.next();	
					String sensorID = aSensor.getSensorID();
					String msgType = aBundle.getString(DataSeries.MSG_TYPE);
					String sensorType = aBundle.getString(DataSeries.SENSOR_TYPE);
					long ts = aBundle.getLong(DataSeries.SERIES_TIMESTAMP);					
					int numSamples = aBundle.getInt(DataSeries.NUM_SAMPLES); 										
					String tempReport = aBundle.getString(DataSeries.DATA_AS_CSV);		

					Log.d(TAG, "sensorID: " + sensorID + " ts: " + ts + "numSamples : " + numSamples + 
							"msgType: " + msgType + " sensorType: " + sensorType);
					Log.d(TAG,"sensor data: " + tempReport);

					values.put(Constants.KEY_TIMESTAMP, ts);							
					values.put(Constants.KEY_SENSOR_ID, sensorID);
					values.put(Constants.KEY_MSG_TYPE, msgType);							
					values.put(Constants.KEY_SENSOR_TYPE, sensorType);
					values.put(Constants.KEY_DATA, tempReport);
					Uri newuri = resolver.insert(uri, values);
					Log.e(TAG,"new uri: "+ newuri.toString());
				}
			}
		}
	}
}
