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

import org.json.JSONObject;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.common.android.data.ElementDataType;
import org.opendatakit.common.android.data.ElementType;
import org.opendatakit.common.android.data.ColumnDefinition;
import org.opendatakit.common.android.database.DataModelDatabaseHelper;
import org.opendatakit.common.android.database.DataModelDatabaseHelperFactory;
import org.opendatakit.common.android.utilities.ODKDatabaseUtils;
import org.opendatakit.common.android.utilities.ODKJsonNames;
import org.opendatakit.sensors.DataSeries;
import org.opendatakit.sensors.DriverType;
import org.opendatakit.sensors.ODKSensor;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
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
	private Context serviceContext;
	private ODKSensorManager sensorManager;

	public WorkerThread(Context context, ODKSensorManager manager) {
		super("WorkerThread");
		isRunning = true;
		serviceContext = context;
		sensorManager = manager;
	}

	public void stopthread() {
		isRunning = false;
		this.interrupt();
	}

	@Override
	public void run() {
		Log.d(TAG, "worker thread started");		
		
		while(isRunning) {
			try {
				for(ODKSensor sensor : sensorManager.getSensorsUsingAppForDatabase()) {
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
				while(iter.hasNext()) {
					Bundle aBundle = iter.next();	

					DriverType driver = sensorManager.getSensorDriverType(aSensor.getSensorID());
					if (driver != null && driver.getTableDefinitionStr() != null) {
						parseSensorDataAndInsertIntoTable(aSensor, driver.getTableDefinitionStr(), aBundle);
					}
					
				}
			}
		}
	}
	
	
	private void parseSensorDataAndInsertIntoTable(ODKSensor aSensor, String strTableDef, Bundle dataBundle){
		JSONObject jsonTableDef = null;
		ContentValues tablesValues = new ContentValues();
		
		DataModelDatabaseHelper dbh = DataModelDatabaseHelperFactory.getDbHelper(serviceContext, aSensor.getAppNameForDatabase());
		SQLiteDatabase db = dbh.getWritableDatabase();
		
		try {
			jsonTableDef = new JSONObject(strTableDef);
			
			String tableId = jsonTableDef.getJSONObject(ODKJsonNames.jsonTableStr).getString(ODKJsonNames.jsonTableIdStr);
			
			if (tableId == null) {
			  return;
			}

		    boolean success;
		    
		    success = false;
		    try {
		      success = ODKDatabaseUtils.hasTableId(db, tableId);
		    } catch ( Exception e ) {
		      e.printStackTrace();
		      throw new SQLException("Exception testing for tableId " + tableId);
		    }
		    if (!success) {
	           sensorManager.parseDriverTableDefintionAndCreateTable(aSensor.getSensorID(), aSensor.getAppNameForDatabase(), db);
		    }

          success = false;
          try {
            success = ODKDatabaseUtils.hasTableId(db, tableId);
          } catch ( Exception e ) {
            e.printStackTrace();
            throw new SQLException("Exception testing for tableId " + tableId);
          }
          if (!success) {
            throw new SQLException("Unable to create tableId " + tableId);
          }

		    final String dbTableName = "\"" + tableId + "\"";
         
			List<Column> columns = ODKDatabaseUtils.getUserDefinedColumns(db, tableId);
			List<ColumnDefinition> orderedDefs = ColumnDefinition.buildColumnDefinitions(columns);
			
 			// Create the columns for the driver table
			for ( ColumnDefinition col : orderedDefs ) {
			  if ( !col.isUnitOfRetention() ) {
			    continue;
			  }

			  String colName = col.getElementKey();
			  ElementType type = ElementType.parseElementType(col.getElementType(), !col.getChildren().isEmpty());
			  
			  if ( colName.equals(DataSeries.SENSOR_ID) ) {

			    // special treatment
			    tablesValues.put(colName, aSensor.getSensorID());

			  } else if ( type.getDataType() == ElementDataType.bool ) {

			    Boolean boolColData = dataBundle.containsKey(colName) ? dataBundle.getBoolean(colName) : null;
             Integer colData = (boolColData == null) ? null : (boolColData ? 1 : 0); 
             tablesValues.put(colName, colData);
			    
           } else if ( type.getDataType() == ElementDataType.integer ) {
             
             Integer colData = dataBundle.containsKey(colName) ? dataBundle.getInt(colName) : null;
             tablesValues.put(colName, colData);
             
           } else if ( type.getDataType() == ElementDataType.number ) {
             
             Double colData = dataBundle.containsKey(colName) ? dataBundle.getDouble(colName) : null;
             tablesValues.put(colName, colData);
             
           } else {
             // everything else is a string value coming across the wire...
             String colData = dataBundle.containsKey(colName) ? dataBundle.getString(colName) : null;
             tablesValues.put(colName, colData);
			  }
			}
   			
 			if (tablesValues.size() > 0) {
 				Log.i(TAG,"Writing db values for sensor:" + aSensor.getSensorID());
 				ODKDatabaseUtils.writeDataIntoExistingDBTable(db, dbTableName, tablesValues);
 			}

        } catch (Exception e) {
        	e.printStackTrace();
        }
	    
	    db.close();
	}
}
