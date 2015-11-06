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
package org.opendatakit.sensors;

import android.os.Bundle;
import android.util.Log;
import org.opendatakit.sensors.manager.ChannelManager;
import org.opendatakit.sensors.manager.DatabaseManager;
import org.opendatakit.sensors.manager.SensorNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class ODKExternalSensor implements ODKSensor {
   // logging
   private static final String LOGTAG = "ODKExternalSensor";

   private String sensorId;
   private String appNameForDatabase;
   private ChannelManager commChannelManager;
   private DriverCommunicator sensorDriverCom;
   private Queue<SensorDataPacket> buffer;
   private String readingUiIntentStr;
   private String configUiIntentStr;
   private int clientCounter;

   private byte[] remainingBytes;

   public ODKExternalSensor(String sensorID, String appName, DriverCommunicator driverCom,
       ChannelManager channelMgr, String readingUiIntentStr, String configUiIntentStr) {
      this.sensorId = sensorID;
      this.appNameForDatabase = appName;
      this.sensorDriverCom = driverCom;
      this.commChannelManager = channelMgr;
      this.readingUiIntentStr = readingUiIntentStr;
      this.configUiIntentStr = configUiIntentStr;

      clientCounter = 0;
      this.buffer = new ConcurrentLinkedQueue<SensorDataPacket>();
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#connect(boolean)
    */
   @Override public void connect() throws SensorNotFoundException {
      try {
         commChannelManager.sensorConnect(sensorId);
      } catch (SensorNotFoundException snfe) {
         snfe.printStackTrace();
         throw snfe;
      }
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#getCommunicationChannelType()
    */
   @Override public CommunicationChannelType getCommunicationChannelType() {
      return commChannelManager.getCommChannelType();
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#disconnect()
    */
   @Override public void disconnect() throws SensorNotFoundException {
      try {
         commChannelManager.sensorDisconnect(sensorId);
      } catch (SensorNotFoundException snfe) {
         snfe.printStackTrace();
         throw snfe;
      }
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#configure(java.lang.String, android.os.Bundle)
    */
   @Override public void configure(String setting, Bundle params) throws ParameterMissingException {
      try {
         commChannelManager.sensorWrite(sensorId, sensorDriverCom.configureCmd(setting, params));
      } catch (ParameterMissingException pmx) {
         pmx.printStackTrace();
         throw pmx;
      }
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#getSensorData(long)
    */
   @Override public List<Bundle> getSensorData(long maxNumReadings) {
      ArrayList<SensorDataPacket> rawData = new ArrayList<SensorDataPacket>();

      synchronized (buffer) {
         rawData.addAll(buffer);
         buffer.clear();
      }

      SensorDataParseResponse response = sensorDriverCom
          .getSensorData(maxNumReadings, rawData, remainingBytes);
      remainingBytes = response.getRemainingData();
      return response.getSensorData();
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#sendDataToSensor(android.os.Bundle)
    */
   @Override public void sendDataToSensor(Bundle dataToEncode) {
      byte[] encodedSensorData = sensorDriverCom.sendDataToSensor(dataToEncode);
      if (encodedSensorData != null)
         commChannelManager.sensorWrite(sensorId, encodedSensorData);
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#startSensor()
    */
   @Override public boolean startSensor() {
      commChannelManager.startSensorDataAcquisition(sensorId, sensorDriverCom.startCmd());
      clientCounter++;
      return true;
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#stopSensor()
    */
   @Override public boolean stopSensor() {
      --clientCounter;
      //		Log.d(LOGTAG,"stopping sensor. conn counter: " + clientCounter);

      if (clientCounter == 0) {
         commChannelManager.stopSensorDataAcquisition(sensorId, sensorDriverCom.stopCmd());

         try {
            commChannelManager.sensorDisconnect(sensorId);
         } catch (SensorNotFoundException snfe) {
            snfe.printStackTrace();
         }
      }

      return true;
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#getSensorID()
    */
   @Override public String getSensorID() {
      return sensorId;
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#addSensorDataPacket(org.opendatakit.sensors.SensorDataPacket)
    */
   @Override public void addSensorDataPacket(SensorDataPacket packet) {
      synchronized (buffer) {
         buffer.add(packet);
      }
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#dataBufferReset()
    */
   @Override public void dataBufferReset() {
      Log.v(LOGTAG, "dataBufferReset: clearing buffer for sensor ");
      if (buffer != null) {
         buffer = new ConcurrentLinkedQueue<SensorDataPacket>();
      }
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#shutdown()
    */
   @Override public void shutdown() throws SensorNotFoundException {
      try {
         this.disconnect();
      } finally {
         sensorDriverCom.shutdown();
      }
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#getReadingUiIntentStr()
    */
   @Override public String getReadingUiIntentStr() {
      return readingUiIntentStr;
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#getConfigUiIntentStr()
    */
   @Override public String getConfigUiIntentStr() {
      return configUiIntentStr;
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#getAppNameForDatabase()
    */
   @Override public String getAppNameForDatabase() {
      return appNameForDatabase;
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#hasReadingUi()
    */
   @Override public boolean hasReadingUi() {
      return (readingUiIntentStr != null);
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#hasConfigUi()
    */
   @Override public boolean hasConfigUi() {
      return (configUiIntentStr != null);
   }

   /* (non-Javadoc)
    * @see org.opendatakit.sensors.ODKSensorInterface#hasAppNameForDatabase()
    */
   @Override public boolean hasAppNameForDatabase() {
      return (appNameForDatabase != null);
   }

   @Override public void setAppNameForDatabase(String appName) {
      DatabaseManager dbManager = SensorsSingleton.getDatabaseManager();
      dbManager.externalSensorUpdateAppName(sensorId, appName);
      appNameForDatabase = appName;
   }
}
