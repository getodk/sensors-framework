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

import android.content.Context;
import android.util.Log;
import org.opendatakit.sensors.*;
import org.opendatakit.sensors.bluetooth.BluetoothManager;
import org.opendatakit.sensors.builtin.BuiltInSensorType;
import org.opendatakit.sensors.builtin.ODKBuiltInSensor;
import org.opendatakit.sensors.drivers.ManifestMetadata;
import org.opendatakit.sensors.dummy.DummyManager;
import org.opendatakit.sensors.usb.USBManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class ODKSensorManager {

   private static final String LOGTAG = ODKSensorManager.class.getSimpleName();
   private DatabaseManager databaseManager;

   private Thread workerThread;
   private Context svcContext;

   private Map<String, ODKSensor> sensors;
   private List<DriverType> driverTypes;
   private Map<CommunicationChannelType, ChannelManager> channelManagers;

   public ODKSensorManager(Context context, DatabaseManager dbManager, BluetoothManager btManager,
       USBManager usbManager, DummyManager dummyManager) {

      this.svcContext = context;
      this.databaseManager = dbManager;

      sensors = new ConcurrentHashMap<String, ODKSensor>();
      channelManagers = new HashMap<CommunicationChannelType, ChannelManager>();

      channelManagers.put(btManager.getCommChannelType(), btManager);
      channelManagers.put(usbManager.getCommChannelType(), usbManager);

      if (SensorsSingleton.DEBUG) {
         channelManagers.put(dummyManager.getCommChannelType(), dummyManager);
      }

      queryNupdateSensorDriverTypes();

      //XXX FIX THIS: This needs to move to the superclass. being done here because each f/w version has a different contenturi
      workerThread = new WorkerThread(svcContext, this);
      workerThread.start();

   }

   public void initializeRegisteredSensors() {

      // discover built in sensors
      android.hardware.SensorManager builtInSensorManager = (android.hardware.SensorManager) svcContext
          .getSystemService(Context.SENSOR_SERVICE);
      if (builtInSensorManager != null) {
         List<android.hardware.Sensor> deviceSensors = builtInSensorManager
             .getSensorList(android.hardware.Sensor.TYPE_ALL);
         for (android.hardware.Sensor hwSensor : deviceSensors) {
            BuiltInSensorType sensorType = BuiltInSensorType
                .convertToBuiltInSensor(hwSensor.getType());
            if (sensorType != null) {
               try {
                  String id = sensorType.name();
                  //Log.d(LOGTAG,"Found sensor "+ id);

                  String appName = SensorsSingleton.defaultAppName();
                  boolean dbTransfer = false;
                  if (databaseManager.internalSensorMetadataInDb(id)) {
                     InternalSensorMetadata dbData = databaseManager.getInternalSensorDataForId
                         (id);
                     appName = dbData.appName;
                     dbTransfer = dbData.dbTransfer;
                  }

                  ODKSensor sensor = new ODKBuiltInSensor(sensorType, builtInSensorManager, id,
                      appName, dbTransfer);
                  addInternalSensor(id, sensor);
               } catch (Exception e) {
                  e.printStackTrace();
               }
            }
         }
      }

      // load sensors from the database
      for (ChannelManager channelManager : channelManagers.values()) {
         CommunicationChannelType type = channelManager.getCommChannelType();
         Log.d(LOGTAG, "Load from DB:" + type.name());

         List<ExternalSensorData> savedSensorList = databaseManager.externalSensorList(type);

         for (ExternalSensorData externalSensorData : savedSensorList) {
            Log.d(LOGTAG,
                "Sensor in DB:" + externalSensorData.id + " Type:" + externalSensorData.type);
            DriverType driverType = getDriverType(externalSensorData.type);

            if (driverType != null) {
               Log.d(LOGTAG, "initing sensor from DB: id: " + externalSensorData.id +
                   " driverType: " + externalSensorData.type + " state "
                   + externalSensorData.state);

               if (connectToDriver(externalSensorData.id, externalSensorData.appName,
                   externalSensorData.dbTransfer, driverType)) {
                  Log.d(LOGTAG,
                      externalSensorData.id + " connected to driver " + externalSensorData.type);

                  if (externalSensorData.state == DetailedSensorState.CONNECTED) {
                     try {
                        channelManager.sensorConnect(externalSensorData.id);
                        //								updateSensorState(externalSensorData.id, DetailedSensorState.CONNECTED);
                        Log.d(LOGTAG, "connected to sensor " + externalSensorData.id + " over "
                            + channelManager.getCommChannelType());
                     } catch (SensorNotFoundException snfe) {
                        updateSensorState(externalSensorData.id, DetailedSensorState.DISCONNECTED);
                        Log.d(LOGTAG, "SensorNotFoundException. unable to connect to sensor "
                            + externalSensorData.id + " over " + channelManager
                            .getCommChannelType());
                     }
                  }
               }
            } else {
               Log.e(LOGTAG, "driver NOT FOUND for type : " + externalSensorData.type);
            }
         }
      }
   }

   private boolean connectToDriver(String id, String appName, boolean dbTransfer, DriverType
       driver) {
      // create the sensor
      ODKExternalSensor sensorFacade = null;
      try {
         DriverCommunicator sensorDriver = new GenericDriverProxy(driver.getSensorPackageName(),
             driver.getSensorDriverAddress(), this.svcContext);
         sensorFacade = new ODKExternalSensor(id, appName, dbTransfer, sensorDriver,
             channelManagers.get(driver.getCommunicationChannelType()),
             driver.getReadingUiIntentStr(), driver.getConfigUiIntentStr());
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
         } catch (SensorNotFoundException snfe) {
            snfe.printStackTrace();
         }
      }
   }

   public void queryNupdateSensorDriverTypes() {
      List<DriverType> allDrivers = new ArrayList<DriverType>();
      List<DriverType> btDrivers = SensorDriverDiscovery
          .getAllDriversForChannel(svcContext, CommunicationChannelType.BLUETOOTH,
              ManifestMetadata.FRAMEWORK_VERSION_2);
      allDrivers.addAll(btDrivers);

      List<DriverType> usbDrivers = SensorDriverDiscovery
          .getAllDriversForChannel(svcContext, CommunicationChannelType.USB,
              ManifestMetadata.FRAMEWORK_VERSION_2);
      allDrivers.addAll(usbDrivers);

      if (SensorsSingleton.DEBUG) {
         List<DriverType> dummyDrivers = SensorDriverDiscovery
             .getAllDriversForChannel(svcContext, CommunicationChannelType.DUMMY,
                 ManifestMetadata.FRAMEWORK_VERSION_2);
         allDrivers.addAll(dummyDrivers);
      }
      driverTypes = allDrivers;
   }

   public DriverType getDriverType(String type) {
      DriverType foundDriverType = null;
      for (DriverType driverType : driverTypes) {
         if (driverType.getSensorType().equals(type)) {
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
    *
    * @param id Sensor id.
    * @return SensorState as determined by communication manager.
    */

   public SensorStateMachine getSensorState(String id) {
      Log.d(LOGTAG, "getting sensor state");
      ODKSensor sensor = sensors.get(id);
      if (sensor == null) {
         Log.e(LOGTAG, "Can't find sensor type");
         return null;
      }

      if(sensor instanceof ODKBuiltInSensor) {
         ODKBuiltInSensor builtInSensor = (ODKBuiltInSensor) sensor;
         return builtInSensor.getSensorState();
      }

      ChannelManager cm = channelManagers.get(sensor.getCommunicationChannelType());
      if (cm == null) {
         Log.e(LOGTAG, "unknown channel type: " + sensor.getCommunicationChannelType());
         return null;
      }

      return cm.getSensorStatus(id);
   }

   public void addSensorDataPacket(String id, SensorDataPacket sdp) {
      ODKSensor sensor = sensors.get(id);
      if (sensor != null) {
         sensor.addSensorDataPacket(sdp);
      } else {
         Log.e(LOGTAG, "can't route data for sensor ID: " + id);
      }
   }

   public void shutdown() {
      shutdownAllSensors();
      ((WorkerThread) workerThread).stopthread();
   }

   public boolean addSensor(String id, DriverType driver, String appName, boolean
       transferDataToDb) {

      if (driver == null) {
         return false;
      }

      Log.d(LOGTAG, "sensor type: " + driver);
      connectToDriver(id, appName, transferDataToDb, driver);

      databaseManager.insertExternalSensor(id, driver.getSensorType(), driver.getSensorType(),
          DetailedSensorState.DISCONNECTED, driver.getCommunicationChannelType(), appName, transferDataToDb);

      return true;
   }

   public boolean addInternalSensor(String id, ODKSensor sensor) {
      if(id == null || sensor == null)
         return false;

      sensors.put(id, sensor);

      return true;
   }

   public List<ODKSensor> getSensorsToTransferToDb() {
      List<ODKSensor> sensorList = new ArrayList<ODKSensor>();

      for (ODKSensor sensor : sensors.values()) {
         if (sensor.transferDataToDb() && sensor.hasAppNameForDatabase()) {
            sensorList.add(sensor);
         }
      }
      return sensorList;
   }

   public void removeAllSensors() {
      shutdownAllSensors();
      sensors = new Hashtable<String, ODKSensor>();

      // TODO: after mobisys consider what is the right thing
      databaseManager.deleteAllExternalSensors();
      databaseManager.deleteAllInternalSensorsMetadata();
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
      databaseManager.externalSensorUpdateState(id, state);
   }

   public DetailedSensorState querySensorState(String id) {
      return databaseManager.externalSensorQuerySensorState(id);
   }

   public String getSensorReadingUiIntentStr(String id) {
      ODKSensor sensor = sensors.get(id);
      if (sensor != null) {
         return sensor.getReadingUiIntentStr();
      }
      return null;
   }

   public String getSensorConfigUiIntentStr(String id) {
      ODKSensor sensor = sensors.get(id);
      if (sensor != null) {
         return sensor.getConfigUiIntentStr();
      }
      return null;
   }

   public boolean hasSensorReadingUi(String id) {
      ODKSensor sensor = sensors.get(id);
      if (sensor != null) {
         return sensor.hasReadingUi();
      }
      return false;
   }

   public boolean hasSensorConfigUi(String id) {
      ODKSensor sensor = sensors.get(id);
      if (sensor != null) {
         return sensor.hasConfigUi();
      }
      return false;
   }

   public DriverType getSensorDriverType(String sensorId) {
      DriverType sensorDriverType = getDriverType(
          databaseManager.externalSensorQueryType(sensorId));
      return sensorDriverType;

   }
}
