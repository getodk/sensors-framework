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
package org.opendatakit.sensors.service;

import android.os.Bundle;
import android.os.Debug;
import android.os.RemoteException;
import android.util.Log;
import org.opendatakit.sensors.*;
import org.opendatakit.sensors.bluetooth.BluetoothManager;
import org.opendatakit.sensors.manager.ODKSensorManager;
import org.opendatakit.sensors.manager.SensorNotFoundException;
import org.opendatakit.sensors.dummy.DummyManager;
import org.opendatakit.sensors.usb.USBManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class SensorServiceInterface extends IODKSensorService.Stub {

   private static final String TAG = SensorServiceInterface.class.getSimpleName();

   private ODKSensorManager mSensorManager;
   private DummyManager mDummyManager;
   private USBManager mUsbManager;
   private BluetoothManager mBtManager;

   public SensorServiceInterface(ODKSensorManager manager, BluetoothManager btManager,
       USBManager usbManager, DummyManager dummyManager) {
      mSensorManager = manager;
      mBtManager = btManager;
      mUsbManager = usbManager;
      mDummyManager = dummyManager;
   }

   public void sensorConnect(String id) throws RemoteException {
      Log.d(TAG, "sensorConnect. id: " + id);
      ODKSensor sensor = mSensorManager.getSensor(id);

      try {
         if (sensor != null) {
            Log.d(TAG, "calling Facade.connect " + id);
            sensor.connect();
            Log.d(TAG, "returned from Facade.connect " + id);
         }
      } catch (SensorNotFoundException snfe) {
         throw new RemoteException();
      }
   }

   public List<Bundle> getSensorData(String id, long maxNumReadings) throws RemoteException {
      List<Bundle> dataFmSensor = new ArrayList<Bundle>();

      ODKSensor sensor = mSensorManager.getSensor(id);

      if (sensor != null) {
         dataFmSensor = sensor.getSensorData(maxNumReadings);
      }

      return dataFmSensor;
   }

   @Override public void sendDataToSensor(String id, Bundle dataToEncode) throws RemoteException {
      ODKSensor sensor = mSensorManager.getSensor(id);

      if (sensor != null) {
         sensor.sendDataToSensor(dataToEncode);
      }
   }

   public boolean startSensor(String id, boolean transferToDb, String appNameForDatabase) throws
       RemoteException {
      ODKSensor sensor = mSensorManager.getSensor(id);

      if (sensor != null) {
         sensor.setAppNameForDatabase(appNameForDatabase);
         sensor.setDbTransfer(transferToDb);
         boolean stat = sensor.startSensor();
         return stat;
      } else {
         return false;
      }
   }

   public boolean stopSensor(String id) throws RemoteException {
      ODKSensor sensor = mSensorManager.getSensor(id);

      if (sensor != null) {
         return sensor.stopSensor();
      } else {
         return false;
      }
   }

   public void configure(String id, String setting, Bundle params) throws RemoteException {
      ODKSensor sensor = mSensorManager.getSensor(id);

      try {
         if (sensor != null) {
            sensor.configure(setting, params);
         }
      } catch (ParameterMissingException pmx) {
         throw new RemoteException();
      }
   }

   public boolean isConnected(String id) throws RemoteException {

      if (mSensorManager == null)
         Log.e(TAG, "Sensor manager is null");

      SensorStateMachine state = mSensorManager.getSensorState(id);
      if (state == null) {
         Log.e(TAG, "State is null");
         state = new SensorStateMachine();
      }
      Log.i(TAG, "Sensor State is " + state.status);
      return state.status == SensorStatus.CONNECTED;
   }

   public boolean isBusy(String id) throws RemoteException {

      SensorStateMachine state = mSensorManager.getSensorState(id);
      if (state == null) {
         Log.e(TAG, "State is null");
         state = new SensorStateMachine();
      }
      return state.workStatus == SensorWorkStatus.BUSY;
   }

   public boolean addSensor(String id, String driverType, String commChannel, String appName)
       throws RemoteException {
      Log.d(TAG, "Inside Add sensor");

      try {
         // Add to communication channel manager
         CommunicationChannelType commChannelType = CommunicationChannelType
             .getCommChannelTypeByName(commChannel);
         if (commChannelType != null) {
            switch (commChannelType) {
            case BLUETOOTH:
               return mBtManager
                   .sensorRegister(id, mSensorManager.getDriverType(driverType), appName);
            case USB:
               return mUsbManager
                   .sensorRegister(id, mSensorManager.getDriverType(driverType), appName);
            case DUMMY:
               return mDummyManager
                   .sensorRegister(id, mSensorManager.getDriverType(driverType), appName);
            default:
               break;
            }
         }
      } catch (Throwable e) {
         e.printStackTrace();
         throw new RemoteException();
      }

      return false;
   }

   public boolean hasSensor(String id) throws RemoteException {
      return (mSensorManager.getSensor(id) != null);
   }

   public void removeAllSensors() throws RemoteException {
      mSensorManager.removeAllSensors();
      mBtManager.removeAllSensors();
      mUsbManager.removeAllSensors();
      mDummyManager.removeAllSensors();
   }

   @Override public String getSensorReadingUiIntentStr(String id) throws RemoteException {
      if (mSensorManager.getSensor(id) != null) {
         return mSensorManager.getSensorReadingUiIntentStr(id);
      }
      return null;
   }

   @Override public String getSensorConfigUiIntentStr(String id) throws RemoteException {
      if (mSensorManager.getSensor(id) != null) {
         return mSensorManager.getSensorConfigUiIntentStr(id);
      }
      return null;
   }

   @Override public boolean hasSensorReadingUi(String id) throws RemoteException {
      if (mSensorManager.getSensor(id) != null) {
         return mSensorManager.hasSensorReadingUi(id);
      }
      return false;
   }

   @Override public boolean hasSensorConfigUi(String id) throws RemoteException {
      if (mSensorManager.getSensor(id) != null) {
         return mSensorManager.hasSensorConfigUi(id);
      }
      return false;
   }
}
