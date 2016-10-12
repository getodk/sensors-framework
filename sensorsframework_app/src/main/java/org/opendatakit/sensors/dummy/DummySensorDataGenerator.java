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

import android.os.Bundle;
import android.util.Log;
import org.opendatakit.sensors.SensorDataPacket;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class DummySensorDataGenerator extends Thread  {

   // logging
   private static final String LOGTAG = DummySensorDataGenerator.class.getSimpleName();

   // Protocol W/ Simulated Phone Sensors
   public static final String GET_TYPE = "GET_TYPE";
   public static final String STOP_DATA = "STOP_DATA";
   public static final String START_DATA = "START_DATA";

   public static final String SEND_DELAY_PARAM = "SendDelay";
   public static final String PACKET_SIZE_PARAM = "PacketSize";
   public static final String START_CMD = "Start";
   public static final String STOP_CMD = "Stop";
   public static final char START_DELIMINATOR = 'S';
   public static final char END_DELMINATOR = 'E';

   private final AtomicBoolean mIsActivated = new AtomicBoolean(false);
   private AtomicBoolean mKillMe = new AtomicBoolean(false);

   // data
   private ODKDummyInternalSensor mSensor;

   private int mSendDelay = 100;
   private int mPacketSize = 1;

   /**
    * Data Generator Constructor
    *
    * @param sensor     dummy sensor to generate data for
    */
   public DummySensorDataGenerator(ODKDummyInternalSensor sensor) {
      mSensor = sensor;
   }

   /**
    * Start Data Read
    */
   public void activate() {
      Log.d(LOGTAG, "Activated data generation");
      mIsActivated.set(true);
   }

   /**
    * Stop Data Read
    */
   public void deactivate() {
      Log.d(LOGTAG, "Deactivate data generation");
      mIsActivated.set(false);
   }


   public void config(String setting, Bundle params) {
      Log.d(LOGTAG, "Dummy sensor setting: " + setting);
      if (setting.equals(SEND_DELAY_PARAM)) {
         mSendDelay = params.getInt(setting);
         Log.d(LOGTAG, "Set " + SEND_DELAY_PARAM + ": " + mSendDelay);
      } else if (setting.equals(PACKET_SIZE_PARAM)) {
         mPacketSize = params.getInt(setting);
         Log.d(LOGTAG, "Set " + PACKET_SIZE_PARAM + ": " + mPacketSize);
      } else if (setting.equals(START_CMD)) {
         activate();
      } else if (setting.equals(STOP_CMD)) {
         deactivate();
      }
   }

   /**
    * Thread Main Loop, Handles Data Connection
    */
   public void run() {
      Log.d(LOGTAG, "Sensor Data Generation Thread Created");

      while (true) {

         try {
            Log.d(LOGTAG, "Sleep " + mSendDelay);
            Thread.sleep(mSendDelay);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }

         Log.d(LOGTAG, "End Sleep");
         if (mKillMe.get() == true) {
            mIsActivated.set(false);
            break;
         }

         // SENSOR ACTIVATED
         if (mIsActivated.get() == true) {
            boolean success = generateSensorData();
            if (!success) {
               mIsActivated.set(false);
            }
         }
      }
   }

   /**
    * Genarate Sensor Data
    */
   private boolean generateSensorData() {
      Log.d(LOGTAG, "GENERATE NEW PACKET");
      byte[] data = new byte[mPacketSize + 2];
      int i = 0;
      data[i++] = START_DELIMINATOR;
      while(i < mPacketSize +1) {
         data[i++] = 1;
      }
      data[i++] = END_DELMINATOR;

      SensorDataPacket sdp = new SensorDataPacket(data, 0);
      mSensor.addSensorDataPacket(sdp);
      return true;
   }

   public void kill() {
      mKillMe.set(true);
   }

}
