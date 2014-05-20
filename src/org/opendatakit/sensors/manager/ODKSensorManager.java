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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.sensors.CommunicationChannelType;
import org.opendatakit.sensors.DriverCommunicator;
import org.opendatakit.sensors.DriverType;
import org.opendatakit.sensors.GenericDriverProxy;
import org.opendatakit.sensors.ODKExternalSensor;
import org.opendatakit.sensors.ODKSensor;
import org.opendatakit.sensors.SensorDataPacket;
import org.opendatakit.sensors.SensorDriverDiscovery;
import org.opendatakit.sensors.SensorStateMachine;
import org.opendatakit.sensors.bluetooth.BluetoothManager;
import org.opendatakit.sensors.builtin.ODKBuiltInSensor;
import org.opendatakit.sensors.builtin.BuiltInSensorType;
import org.opendatakit.sensors.drivers.ManifestMetadata;
import org.opendatakit.sensors.tests.DummyManager;
import org.opendatakit.sensors.usb.USBManager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class ODKSensorManager {

	private static final String TAG = "ODKSensorManager";
	private DatabaseManager databaseManager;
	
	private Thread workerThread;
	private Context svcContext;
	
	private Map<String, ODKSensor> sensors;
	private List<DriverType> driverTypes;
	private Map<CommunicationChannelType,ChannelManager> channelManagers;
	
	public ODKSensorManager(Context context, DatabaseManager dbManager,BluetoothManager btManager,
			USBManager usbManager, DummyManager dummyManager) {
		
		svcContext = context;
		this.databaseManager = dbManager;

		sensors = new Hashtable<String, ODKSensor>();
		channelManagers = new HashMap<CommunicationChannelType,ChannelManager>();	
		
		channelManagers.put(btManager.getCommChannelType(), btManager);
		channelManagers.put(usbManager.getCommChannelType(), usbManager);
		channelManagers.put(dummyManager.getCommChannelType(),dummyManager);
		
		queryNupdateSensorDriverTypes();
		
		//XXX FIX THIS: This needs to move to the superclass. being done here because each f/w version has a different contenturi		
		workerThread = new WorkerThread(svcContext, this);
		workerThread.start();
		
	}			
	
	public void initializeRegisteredSensors() {
		
		// discover built in sensors
		android.hardware.SensorManager builtInSensorManager = (android.hardware.SensorManager) svcContext.getSystemService(Context.SENSOR_SERVICE);
		if(builtInSensorManager != null) {
			List<android.hardware.Sensor> deviceSensors = builtInSensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL);
			for(android.hardware.Sensor hwSensor : deviceSensors) {
				BuiltInSensorType sensorType = BuiltInSensorType.convertToBuiltInSensor(hwSensor.getType());
				if(sensorType != null) {
					try {
						String id = sensorType.name();
						//Log.d(TAG,"Found sensor "+ id);
						ODKSensor sensor = new ODKBuiltInSensor(sensorType, builtInSensorManager, id);			
						sensors.put(id, sensor);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// load sensors from the database		
		for(ChannelManager channelManager : channelManagers.values()) {
			CommunicationChannelType type = channelManager.getCommChannelType();
			Log.d(TAG, "Load from DB:" + type.name());

			List<SensorData> savedSensorList =  databaseManager.sensorList(type);

			for(SensorData sensorData : savedSensorList) {
				Log.d(TAG, "Sensor in DB:" + sensorData.id + " Type:" + sensorData.type);
				DriverType driverType = getDriverType(sensorData.type);

				if(driverType != null) {
					Log.d(TAG,"initing sensor from DB: id: " + sensorData.id + 
							" driverType: " + sensorData.type + " state " + sensorData.state);

					if(connectToDriver(sensorData.id,driverType)) {						
						Log.d(TAG,sensorData.id + " connected to driver " + sensorData.type);

						if(sensorData.state == DetailedSensorState.CONNECTED) {
							try {
								channelManager.sensorConnect(sensorData.id);
//								updateSensorState(sensorData.id, DetailedSensorState.CONNECTED);
								Log.d(TAG,"connected to sensor " + sensorData.id  + " over " + channelManager.getCommChannelType());
							}
							catch(SensorNotFoundException snfe) {
								updateSensorState(sensorData.id, DetailedSensorState.DISCONNECTED);
								Log.d(TAG,"SensorNotFoundException. unable to connect to sensor " + sensorData.id + " over " + channelManager.getCommChannelType());
							}
						}
					}
				}
				else {
					Log.e(TAG,"driver NOT FOUND for type : " + sensorData.type);
				}
			}					
		}
	}
	
	
	private boolean connectToDriver(String id,DriverType driver) {
		// create the sensor		
		ODKExternalSensor sensorFacade = null;
		try {
			DriverCommunicator sensorDriver = new GenericDriverProxy(driver.getSensorPackageName(), driver.getSensorDriverAddress(), this.svcContext);
			sensorFacade = new ODKExternalSensor(id,sensorDriver,channelManagers.get(driver.getCommunicationChannelType()), driver.getReadingUiIntentStr(), driver.getConfigUiIntentStr());			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		//put facade instead of driver
		sensors.put(id, sensorFacade);
		return true;		
	}
	
	private void shutdownAllSensors() {
		for (ODKSensor sensor : sensors.values()) {
			try {
				sensor.shutdown();
			}
			catch(SensorNotFoundException snfe) {
				snfe.printStackTrace();
			}
		}
	}
	

	public void queryNupdateSensorDriverTypes() {
		List<DriverType> allDrivers = new ArrayList<DriverType>();
		List<DriverType> btDrivers = SensorDriverDiscovery.getAllDriversForChannel(svcContext, CommunicationChannelType.BLUETOOTH, ManifestMetadata.FRAMEWORK_VERSION_2);
		List<DriverType> usbDrivers = SensorDriverDiscovery.getAllDriversForChannel(svcContext, CommunicationChannelType.USB, ManifestMetadata.FRAMEWORK_VERSION_2);
		List<DriverType> dummyDrivers = SensorDriverDiscovery.getAllDriversForChannel(svcContext, CommunicationChannelType.DUMMY, ManifestMetadata.FRAMEWORK_VERSION_2);
		allDrivers.addAll(btDrivers);
		allDrivers.addAll(usbDrivers);
		allDrivers.addAll(dummyDrivers);
		driverTypes = allDrivers;
	}
	
	public void parseDriverTableDefintionAndCreateTable(String sensorId, String appForDatabase, SQLiteDatabase db){
		String strTableDef = null;
		// Get the sensor information from the database
		SensorData sensorDataFromDb;
		if (databaseManager.sensorIsInDatabase(sensorId)) {
			sensorDataFromDb = databaseManager.getSensorDataForId(sensorId);
			DriverType driver = getDriverType(sensorDataFromDb.type);
			if (driver != null) {
				strTableDef = driver.getTableDefinitionStr();
			}
		}
		
		if (strTableDef == null) {
			return;
		}
		
		JSONObject jsonTableDef = null;
		
		try {
			
			jsonTableDef = new JSONObject(strTableDef);
			
			String tableName = jsonTableDef.getJSONObject("table").getString("name");
    	   
			LinkedHashMap<String, String> columns = new LinkedHashMap<String, String>();
   			
   			// Create the columns for the driver table
   			JSONArray colJsonArray = jsonTableDef.getJSONObject("table").getJSONArray("columns");
   			
   			for (int i = 0; i < colJsonArray.length(); i++) {
   				JSONObject colJson = colJsonArray.getJSONObject(i);
   				columns.put(colJson.getString("name"), colJson.getString("type"));
   			}
   			
   			// Create the table for driver
   			ODKDatabaseUtils.createOrOpenDBTableWithColumns(db, tableName, columns);
     
        } catch (Exception e) {
        	e.printStackTrace();
        }
	    
	    db.close();
	}

	public DriverType getDriverType(String type) {
		DriverType foundDriverType = null;
		for(DriverType driverType : driverTypes) {
			if(driverType.getSensorType().equals(type)) {
				foundDriverType = driverType;
				break;
			}
		}		
		return foundDriverType;
	}


	public ODKSensor getSensor(String id) {
		return sensors.get(id);
	}

	/**
	 * Get the sensor status
	 * @param id Sensor id.
	 * @return SensorState as determined by communication manager.
	 */

	public SensorStateMachine getSensorState(String id) {		
		Log.d(TAG,"getting sensor state");
		ODKSensor sensor = sensors.get(id);
		if (sensor == null) {
			Log.e(TAG, "Can't find sensor type");
			return null;
		}
		
		ChannelManager cm = channelManagers.get(sensor.getCommunicationChannelType());		
		if (cm == null) {
			Log.e(TAG, "unkown channel type: " + sensor.getCommunicationChannelType());
			return null;
		}
		
		return cm.getSensorStatus(id);		
	}

	public void addSensorDataPacket(String id, SensorDataPacket sdp) {
		ODKSensor sensor = sensors.get(id);
		if (sensor != null) {
			sensor.addSensorDataPacket(sdp);
		} else {
			Log.e(TAG, "can't route data for sensor ID: " + id);
		}
	}

	public void shutdown() {
		shutdownAllSensors(); 
		((WorkerThread) workerThread).stopthread();				
	}		

	public boolean addSensor(String id, DriverType driver) {

		if(driver == null) {
			return false;
		}

		Log.d(TAG,"sensor type: " + driver);		
		connectToDriver(id,driver);
	
		databaseManager.sensorInsert(id, driver.getSensorType(), driver.getSensorType(), 
				DetailedSensorState.DISCONNECTED, 
				driver.getCommunicationChannelType());
		
		return true;
	}
	
	public List<ODKSensor> getSensorsUsingAppForDatabase() {
		List<ODKSensor> sensorList = new ArrayList<ODKSensor>();

		for (ODKSensor sensor : sensors.values()) {
			if (sensor.hasAppNameForDatabase()) {
				sensorList.add(sensor);
			}
		}
		return sensorList;
	}


	public void removeAllSensors() {
		shutdownAllSensors();
		sensors = new Hashtable<String, ODKSensor>();
		
		// TODO: after mobisys consider what is the right thing
		databaseManager.deleteAllSensors();
	}
	

	

	public List<DriverType> getDriverTypes() {
		return driverTypes;
	}


	public List<ODKSensor> getRegisteredSensors(CommunicationChannelType channelType) {
		List<ODKSensor> sensorList = new ArrayList<ODKSensor>();

		for (ODKSensor sensor : sensors.values()) {
			if (sensor.getCommunicationChannelType() == channelType) {
				sensorList.add(sensor);
			}
		}
		return sensorList;
	}
	

	public void updateSensorState(String id, DetailedSensorState state) {
		databaseManager.sensorUpdateState(id, state);		
	}
	

	public DetailedSensorState querySensorState(String id) {
		return databaseManager.sensorQuerySensorState(id);
	}
	

	public String getSensorReadingUiIntentStr(String id) {
		ODKSensor sensor = sensors.get(id);
		if(sensor != null) {
			return sensor.getReadingUiIntentStr();
		}
		return null;
	}


	public String getSensorConfigUiIntentStr(String id) {
		ODKSensor sensor = sensors.get(id);
		if(sensor != null) {
			return sensor.getConfigUiIntentStr();
		}
		return null;
	}
	

	public boolean hasSensorReadingUi(String id) {
		ODKSensor sensor = sensors.get(id);
		if(sensor != null) {
			return sensor.hasReadingUi();
		}
		return false;
	}


	public boolean hasSensorConfigUi(String id) {
		ODKSensor sensor = sensors.get(id);
		if(sensor != null) {
			return sensor.hasConfigUi();
		}
		return false;
	}
	
	public DriverType getSensorDriverType(String sensorId) {
		DriverType sensorDriverType = getDriverType(databaseManager.sensorQueryType(sensorId));
		return sensorDriverType;

	}
}
