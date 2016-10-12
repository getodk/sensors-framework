package org.opendatakit.sensors.dummy;

import android.os.Bundle;

import org.opendatakit.sensors.*;
import org.opendatakit.sensors.manager.DatabaseManager;
import org.opendatakit.sensors.manager.SensorNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by wrb on 9/26/2016.
 */
public class ODKDummyInternalSensor implements ODKSensor {

   private static final String LOGTAG = ODKDummyInternalSensor.class.getSimpleName();

   public static final String INTERNAL_ID = "DUMMY_INTERNAL";

   private String sensorId;
   private String appNameForDatabase;
   private boolean dbTransfer;
   private Driver sensorDriver;
   private boolean connected;
   private DummySensorDataGenerator dataGenerator;

   private Queue<SensorDataPacket> buffer;
   private byte[] remainingBytes;

   public ODKDummyInternalSensor(String sensorID, String appName, boolean transferToDb, Driver driver) {
      this.sensorId = sensorID;
      this.appNameForDatabase = appName;
      this.dbTransfer = transferToDb;
      this.sensorDriver = driver;

      this.connected = false;
      this.dataGenerator = new DummySensorDataGenerator(this);
      this.buffer = new ConcurrentLinkedQueue<SensorDataPacket>();
   }

   boolean isConnected () {
      return connected;
   }

   @Override public void connect() throws SensorNotFoundException {
      this.dataGenerator.start();
      connected = true;
   }

   @Override public CommunicationChannelType getCommunicationChannelType() {
      return CommunicationChannelType.DUMMY;
   }

   @Override public void disconnect() throws SensorNotFoundException {
      connected = false;
   }

   @Override public void configure(String setting, Bundle params) throws ParameterMissingException {
      dataGenerator.config(setting, params);
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

   @Override public void sendDataToSensor(Bundle dataToEncode) {

   }

   @Override public boolean startSensor() {
      dataGenerator.activate();
      return true;
   }

   @Override public boolean stopSensor() {
      dataGenerator.deactivate();
      return true;
   }

   @Override public String getSensorID() {
      return sensorId;
   }

   @Override public void addSensorDataPacket(SensorDataPacket packet) {
      buffer.add(packet);
   }

   @Override public void dataBufferReset() {
      buffer.clear();
   }

   @Override public void shutdown() throws SensorNotFoundException {
      dataGenerator.deactivate();
      dataGenerator.kill();
   }

   @Override public String getReadingUiIntentStr() {
      return null;
   }

   @Override public String getConfigUiIntentStr() {
      return null;
   }

   @Override public boolean hasReadingUi() {
      return false;
   }

   @Override public boolean hasConfigUi() {
      return false;
   }

   @Override public String getAppNameForDatabase() {
      return appNameForDatabase;
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
}
