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
package org.opendatakit.sensors.dummy;

import android.content.Context;
import android.util.Log;
import org.opendatakit.sensors.*;
import org.opendatakit.sensors.manager.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class DummyManager extends AbstractChannelManagerBase {

   private static final String LOGTAG = DummyManager.class.getSimpleName();
   private HashMap<String, ODKDummyInternalSensor> mSensorMap;
   private DatabaseManager mDatabaseManager;

   public DummyManager(Context context, DatabaseManager database) {
      super(context, CommunicationChannelType.DUMMY);
      mSensorMap = new HashMap<String, ODKDummyInternalSensor>();
      mDatabaseManager = database;
   }

   @Override public void initializeSensors() {

      // create dummy internal sensor
      String id = ODKDummyInternalSensor.INTERNAL_ID;
      String appName = SensorsSingleton.defaultAppName();
      if (mDatabaseManager.internalSensorMetadataInDb(id)) {
         appName = mDatabaseManager.internalSensorAppName(id);
      }
      ODKDummyInternalSensor sensor = new ODKDummyInternalSensor(id, appName, false, new
          DummySensorInternalDriver());
      mSensorMap.put(id, sensor);

      // add dummy internal sensor to sensor manager
      mSensorManager.addInternalSensor(id, sensor);

   }

   @Override public List<DiscoverableDevice> getDiscoverableSensor() {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public SensorStateMachine getSensorStatus(String id) {
      if(id.equals(ODKDummyInternalSensor.INTERNAL_ID)) {
         ODKDummyInternalSensor ds = getSensor(id);
         SensorStateMachine ssm = new SensorStateMachine();
         if(ds.isConnected()) {
            ssm.status = SensorStatus.CONNECTED;
         }
         return ssm;
      } else {
         // since it was not internal dummy sensor
         // use same external code the rest of the other drivers use
         return super.getSensorStatus(id);
      }
   }

   public boolean sensorRegister(String id, DriverType sensorType, String appName) {
      Log.d(LOGTAG, "In sensor register.");
      if (id == null || sensorType == null) {
         Log.d(LOGTAG, " registeration FAILED. sensor id or sensorType is null.");
         return false;
      }

      ODKDummyInternalSensor ds = getSensor(id);
      if (ds == null) {
         Log.d(LOGTAG, "DummySensorDataGenerator is null");
         // NEED TO ADD A SENSOR

         mSensorManager.addSensor(id, sensorType, appName, false);
         Log.d(LOGTAG, "Added dummy sensor to sensor manager.");
      }

      if (ds != null) {
         return true;
      }

      return true;
   }

   public void sensorConnect(String id) throws SensorNotFoundException {
      Log.d(LOGTAG, "Sensor connect: " + id);
      ODKDummyInternalSensor dummySensor = getSensor(id);



      if (dummySensor != null) {
         Log.d(LOGTAG, "Connecting to physical sensor");
         dummySensor.connect();
      }
   }

   public void sensorDisconnect(String id) throws SensorNotFoundException {
      // N/A
   }

   // Start the sensor data acquisition reading form the file
   public void startSensorDataAcquisition(String id, byte[] command) {
      Log.d(LOGTAG, "Sensor record: " + id);

      // can safely ignore the bytes because they don't do anything

      // clear any previous buffer data
      ODKSensor sensor = mSensorManager.getSensor(id);
      sensor.dataBufferReset();

      // inform dummy sensor to start recording a sensor
      ODKDummyInternalSensor ds = getSensor(id);

      if (ds != null)
         ds.startSensor();
   }

   public void stopSensorDataAcquisition(String id, byte[] command) {
      Log.d(LOGTAG, "Sensor record stop: " + id);

      // can safely ignore the bytes because they don't do anything

      ODKDummyInternalSensor ds = getSensor(id);

      // inform dummy sensor to stop recording a sensor
      if (ds != null)
         ds.stopSensor();
   }

   public void sensorWrite(String id, byte[] message) {

   }

   public void searchForSensors() {
      // N/A
   }

   public void shutdown() {
      removeAllSensors();
   }

   @Override public CommunicationChannelType getCommChannelType() {
      return CommunicationChannelType.DUMMY;
   }

   public synchronized void removeAllSensors() {
      // Cleanup All Of The Dummy Sensors
      if (mSensorMap != null) {
         for (String id : mSensorMap.keySet()) {
            mDatabaseManager.externalSensorDelete(id);
         }

         Collection<ODKDummyInternalSensor> c = mSensorMap.values();
         Iterator<ODKDummyInternalSensor> it = c.iterator();

         while (it.hasNext()) {
            try {
               it.next().shutdown();
            } catch(SensorNotFoundException e) {
               e.printStackTrace();
            }
         }
      }
      mSensorMap.clear();
   }

   ////////// HELPERS /////////////
   private ODKDummyInternalSensor getSensor(String id) {
      return mSensorMap.get(id);
   }
}
