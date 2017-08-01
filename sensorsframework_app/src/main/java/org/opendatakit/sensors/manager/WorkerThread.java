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

import android.content.*;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendatakit.aggregate.odktables.rest.ElementDataType;
import org.opendatakit.aggregate.odktables.rest.ElementType;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.consts.IntentConsts;
import org.opendatakit.database.data.ColumnDefinition;
import org.opendatakit.database.data.ColumnList;
import org.opendatakit.database.data.OrderedColumns;
import org.opendatakit.database.service.*;
import org.opendatakit.exception.ServicesAvailabilityException;
import org.opendatakit.provider.DataTableColumns;
import org.opendatakit.sensors.DataSeries;
import org.opendatakit.sensors.DriverType;
import org.opendatakit.sensors.ODKSensor;
import org.opendatakit.utilities.LocalizationUtils;
import org.opendatakit.utilities.ODKJsonNames;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class WorkerThread extends Thread {
   private static final String TAG = "SensorsWorkerThread";

   private AtomicBoolean isRunning;
   private Context serviceContext;
   private ODKSensorManager sensorManager;
   private ServiceConnectionWrapper databaseServiceConnection = null;
   private UserDbInterface databaseService = null;

   public WorkerThread(Context context, ODKSensorManager manager) {
      super("WorkerThread");
      isRunning = new AtomicBoolean(true);
      serviceContext = context;
      sensorManager = manager;
   }

   /**
    * Wrapper class for service activation management.
    *
    * @author mitchellsundt@gmail.com
    */
   private final class ServiceConnectionWrapper implements ServiceConnection {

      @Override public void onServiceConnected(ComponentName name, IBinder service) {
         WorkerThread.this.doServiceConnected(name, service);
      }

      @Override public void onServiceDisconnected(ComponentName name) {
         WorkerThread.this.doServiceDisconnected(name);
      }
   }

   private void unbindDatabaseBinderWrapper() {
      try {
         ServiceConnectionWrapper tmp = databaseServiceConnection;
         databaseServiceConnection = null;
         if (tmp != null) {
            serviceContext.unbindService(tmp);
         }
      } catch (Exception e) {
         // ignore
         e.printStackTrace();
      }
   }

   private void shutdownServices() {
      Log.i(TAG, "shutdownServices - Releasing WebServer and DbShim service");
      databaseService = null;
      unbindDatabaseBinderWrapper();
   }

   private void bindToService() {
      if (isRunning.get()) {
         if (databaseService == null && databaseServiceConnection == null) {
            Log.i(TAG, "Attempting bind to Database service");
            databaseServiceConnection = new ServiceConnectionWrapper();
            Intent bind_intent = new Intent();
            bind_intent.setClassName(IntentConsts.Database.DATABASE_SERVICE_PACKAGE,
                IntentConsts.Database.DATABASE_SERVICE_CLASS);
            serviceContext.bindService(bind_intent, databaseServiceConnection,
                Context.BIND_AUTO_CREATE | ((Build.VERSION.SDK_INT >= 14) ?
                    Context.BIND_ADJUST_WITH_ACTIVITY :
                    0));
         }
      }
   }

   private void doServiceConnected(ComponentName className, IBinder service) {

      if (className.getClassName().equals(IntentConsts.Database.DATABASE_SERVICE_CLASS)) {
         Log.i(TAG, "Bound to Database service");

         try {
            InternalUserDbInterface internalUserDbInterface = new InternalUserDbInterfaceAidlWrapperImpl
                (AidlDbInterface.Stub.asInterface(service));
            databaseService = new UserDbInterfaceImpl(internalUserDbInterface);
         } catch (IllegalArgumentException e) {
            databaseService = null;
         }
      }
   }

   public UserDbInterface getDatabase() {
      return databaseService;
   }

   private void doServiceDisconnected(ComponentName className) {

      if (className.getClassName().equals(IntentConsts.Database.DATABASE_SERVICE_CLASS)) {
         if (!isRunning.get()) {
            Log.i(TAG, "Unbound from Database service (intentionally)");
         } else {
            Log.w(TAG, "Unbound from Database service (unexpected)");
         }
         databaseService = null;
         unbindDatabaseBinderWrapper();
      }

      // the bindToService() method decides whether to connect or not...
      bindToService();
   }

   public void stopthread() {
      isRunning.set(false);
      this.interrupt();
   }

   @Override public void run() {
      Log.d(TAG, "worker thread started");

      while (isRunning.get()) {
         bindToService();

         while ((isRunning.get()) && (getDatabase() != null)) {
            try {
               for (ODKSensor sensor : sensorManager.getSensorsToTransferToDb()) {
                  moveSensorDataToDB(sensor);
               }

               Thread.sleep(3000);
            } catch (InterruptedException iex) {
               Log.w(TAG, "Sensors worker thread interrupted");
            }
         }
      }

      shutdownServices();
   }

   private void moveSensorDataToDB(ODKSensor aSensor) {
      if (aSensor != null) {
         List<Bundle> bundles = aSensor.getSensorData(0);// XXX for now this gets
         // all data fm sensor
         if (bundles != null) {
            Iterator<Bundle> iter = bundles.iterator();
            while (iter.hasNext()) {
               Bundle aBundle = iter.next();

               DriverType driver = sensorManager.getSensorDriverType(aSensor.getSensorID());
               if (driver != null && driver.getTableDefinitionStr() != null) {
                  parseSensorDataAndInsertIntoTable(aSensor, driver.getTableDefinitionStr(),
                      aBundle);
               }

            }
         }
      }
   }

   private void parseSensorDataAndInsertIntoTable(ODKSensor aSensor, String strTableDef,
       Bundle dataBundle) {

      ContentValues tablesValues = new ContentValues();
      DbHandle db = null;
      try {
         db = getDatabase().openDatabase(aSensor.getAppNameForDatabase());

         if (strTableDef == null) {
            throw new IllegalArgumentException("The tableDefinition is null!");
         }

         JSONObject theTableDef = (new JSONObject(strTableDef))
             .getJSONObject(ODKJsonNames.jsonTableStr);
         String tableId = theTableDef.getString(ODKJsonNames.jsonTableIdStr);

         if (tableId == null) {
            throw new IllegalArgumentException("The tableDefinition does not specify the tableId!");
         }

         OrderedColumns orderedDefs;
         // if the table does not exist, create it.
         // NOTE: if the table does exist, we don't verify that the table schema matches.
         // if we want to do that, always take the not-exists branch...
         if (!getDatabase().hasTableId(aSensor.getAppNameForDatabase(), db, tableId)) {

            List<Column> columns = new ArrayList<Column>();
            // Create the columns for the driver table
            JSONArray colJsonArray = theTableDef.getJSONArray(ODKJsonNames.jsonColumnsStr);

            for (int i = 0; i < colJsonArray.length(); i++) {
               JSONObject colJson = colJsonArray.getJSONObject(i);
               String elementKey = colJson.getString(ODKJsonNames.jsonElementKeyStr);
               String elementName = colJson.getString(ODKJsonNames.jsonElementNameStr);
               String elementType = colJson.getString(ODKJsonNames.jsonElementTypeStr);
               String listChildElementKeys = colJson
                   .getString(ODKJsonNames.jsonListChildElementKeysStr);
               columns.add(new Column(elementKey, elementName, elementType, listChildElementKeys));
            }

            // Create the table for driver
            ColumnList cols = new ColumnList(columns);
            orderedDefs = getDatabase()
                .createOrOpenTableWithColumns(aSensor.getAppNameForDatabase(), db, tableId, cols);
         } else {
            orderedDefs = getDatabase()
                .getUserDefinedColumns(aSensor.getAppNameForDatabase(), db, tableId);
         }

         // store data values into the user-defined columns of the driver table
         for (ColumnDefinition col : orderedDefs.getColumnDefinitions()) {
            if (!col.isUnitOfRetention()) {
               continue;
            }

            String colName = col.getElementKey();
            ElementType type = col.getType();

            if (colName.equals(DataSeries.SENSOR_ID)) {

               // special treatment
               tablesValues.put(colName, aSensor.getSensorID());

            } else if (type.getDataType() == ElementDataType.bool) {

               Boolean boolColData = dataBundle.containsKey(colName) ?
                   dataBundle.getBoolean(colName) :
                   null;
               Integer colData = (boolColData == null) ? null : (boolColData ? 1 : 0);
               tablesValues.put(colName, colData);

            } else if (type.getDataType() == ElementDataType.integer) {

               Integer colData = dataBundle.containsKey(colName) ?
                   dataBundle.getInt(colName) :
                   null;
               tablesValues.put(colName, colData);

            } else if (type.getDataType() == ElementDataType.number) {

               Double colData = dataBundle.containsKey(colName) ?
                   dataBundle.getDouble(colName) :
                   null;
               tablesValues.put(colName, colData);

            } else {
               // everything else is a string value coming across the wire...
               String colData = dataBundle.containsKey(colName) ?
                   dataBundle.getString(colName) :
                   null;
               tablesValues.put(colName, colData);
            }
         }

         if (tablesValues.size() > 0) {
            Log.i(TAG, "Writing db values for sensor:" + aSensor.getSensorID());
            String rowId = tablesValues.containsKey(DataTableColumns.ID) ?
                tablesValues.getAsString(DataTableColumns.ID) :
                null;
            if (rowId == null) {
               rowId = LocalizationUtils.genUUID();
            }
            // don't require current user to have appropriate privileges to insert data
            getDatabase().privilegedInsertRowWithId(aSensor.getAppNameForDatabase(), db, tableId,
                orderedDefs, tablesValues, rowId, true);
         }
      } catch (ServicesAvailabilityException e) {
         e.printStackTrace();
      } catch (Exception e) {
         e.printStackTrace();
      } finally {
         if (db != null) {
            try {
               getDatabase().closeDatabase(aSensor.getAppNameForDatabase(), db);
            } catch (ServicesAvailabilityException e) {
               e.printStackTrace();
            }
         }
      }
   }
}
