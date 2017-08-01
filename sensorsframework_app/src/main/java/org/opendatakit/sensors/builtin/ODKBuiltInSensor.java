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
package org.opendatakit.sensors.builtin;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import org.opendatakit.sensors.*;
import org.opendatakit.sensors.builtin.drivers.AbstractBuiltinDriver;
import org.opendatakit.sensors.manager.DatabaseManager;
import org.opendatakit.sensors.manager.SensorNotFoundException;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class ODKBuiltInSensor implements ODKSensor, SensorEventListener {

   // logging
   private static final String LOGTAG = "BuiltInSensor";

   private static final String DELIMINATOR = "\n";

   // sensor description
   private final BuiltInSensorType sensorType;
   private final SensorManager mBuiltInSensorManager;
   private final String sensorId;
   private final Driver sensorDriver;

   // state
   private Queue<SensorDataPacket> buffer;
   private boolean dbTransfer;
   private String appNameForDatabase;
   private byte[] remainingBytes;

   private int rate;

   private boolean connected;

   public ODKBuiltInSensor(BuiltInSensorType type, SensorManager builtInSensorManager,
       String sensorID, String appName, boolean transferToDb) throws Exception {
      this.appNameForDatabase = appName;
      this.dbTransfer = transferToDb;
      this.sensorType = type;
      this.mBuiltInSensorManager = builtInSensorManager;
      this.sensorId = sensorID;
      Class<? extends AbstractBuiltinDriver> sensorClass = sensorType.getDriverClass();
      Constructor<? extends AbstractBuiltinDriver> constructor;
      constructor = sensorClass.getConstructor();
      this.sensorDriver = constructor.newInstance();

      this.buffer = new ConcurrentLinkedQueue<SensorDataPacket>();
      this.rate = SensorManager.SENSOR_DELAY_NORMAL;
      this.connected = false;
   }

   public SensorStateMachine getSensorState() {
      SensorStateMachine ssm = new SensorStateMachine();
      if(connected) {
         ssm.status = SensorStatus.CONNECTED;
      }
      return ssm;
   }

   @Override public void connect() throws SensorNotFoundException {
      Sensor sensor = mBuiltInSensorManager.getDefaultSensor(sensorType.getType());
      if (sensor == null) {
         throw new SensorNotFoundException("Unable to locate sensor " + sensorType.name());
      }
     connected = true;
   }

   @Override public void disconnect() throws SensorNotFoundException {
      this.stopSensor();
      connected = false;
   }

   @Override public void shutdown() throws SensorNotFoundException {
      this.disconnect();
   }

   @Override public void configure(String setting, Bundle params) throws ParameterMissingException {
      if (setting.equals("rate")) {
         int tmpRate = params.getInt("rate");

         // TODO: remove check after application moves to higher API level
         // As of Android 3.0 (API Level 11) you can also specify the delay
         // as an absolute value (in microseconds).
         if (tmpRate == SensorManager.SENSOR_DELAY_NORMAL
             || tmpRate == SensorManager.SENSOR_DELAY_UI
             || tmpRate == SensorManager.SENSOR_DELAY_GAME
             || tmpRate == SensorManager.SENSOR_DELAY_FASTEST) {
            rate = tmpRate;
         }
      }
   }

   @Override public boolean startSensor() {
      Sensor sensor = mBuiltInSensorManager.getDefaultSensor(sensorType.getType());
      if (sensor != null) {
         mBuiltInSensorManager.registerListener(this, sensor, rate);
         return true;
      } else {
         return false;
      }

   }

   @Override public boolean stopSensor() {
      mBuiltInSensorManager.unregisterListener(this);
      return true;
   }

   @Override public List<Bundle> getSensorData(long maxNumReadings) {
      ArrayList<SensorDataPacket> rawData = new ArrayList<SensorDataPacket>();
      synchronized (buffer) {
         rawData.addAll(buffer);
         buffer.clear();
      }
      SensorDataParseResponse response = sensorDriver
          .getSensorData(maxNumReadings, rawData, remainingBytes);
      remainingBytes = response.getRemainingData();
      return response.getSensorData();
   }

   @Override public CommunicationChannelType getCommunicationChannelType() {
      return CommunicationChannelType.BUILTIN;
   }

   @Override public String getSensorID() {
      return sensorId;
   }

   @Override public void dataBufferReset() {
      Log.d(LOGTAG, "dataBufferReset: clearing buffer for sensor ");
      if (buffer != null) {
         buffer = new ConcurrentLinkedQueue<SensorDataPacket>();
      }
   }

   @Override public void addSensorDataPacket(SensorDataPacket packet) {
      buffer.add(packet);
   }

   @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
      // DO NOTHING YET

   }

   @Override public void onSensorChanged(SensorEvent event) {
      if (event.sensor.getType() == sensorType.getType()) {
         // encode the float array into a byte array
         String sdpValuesStr = "";
         for (float value : event.values) {
            sdpValuesStr += Float.toString(value) + DELIMINATOR;
         }

         // populate sensor data packet
         SensorDataPacket sdp = new SensorDataPacket(sdpValuesStr.getBytes(), event.timestamp);
         addSensorDataPacket(sdp);
      }

   }

   @Override public String getReadingUiIntentStr() {
      return null;
   }

   @Override public String getConfigUiIntentStr() {
      return null;
   }

   @Override public String getAppNameForDatabase() {
      return appNameForDatabase;
   }

   @Override public boolean hasReadingUi() {
      return false;
   }

   @Override public boolean hasConfigUi() {
      return false;
   }

   @Override public boolean hasAppNameForDatabase() {
      return (appNameForDatabase != null);
   }

   @Override public void setAppNameForDatabase(String appName) {
      DatabaseManager dbManager = SensorsSingleton.getDatabaseManager();
      dbManager.internalSensorUpdateAppName(sensorId, appName);
      appNameForDatabase = appName;
   }

   @Override public boolean transferDataToDb() {
      return dbTransfer;
   }

   @Override public void setDbTransfer(boolean transferToDb) {
      DatabaseManager dbManager = SensorsSingleton.getDatabaseManager();
      dbManager.internalSensorUpdateDbTransfer(sensorId, transferToDb);
      dbTransfer = transferToDb;
   }

   @Override public void sendDataToSensor(Bundle data) {
      // TODO Auto-generated method stub

   }
}
