package org.opendatakit.sensors.dummy;

import org.opendatakit.sensors.Driver;
import org.opendatakit.sensors.ParameterMissingException;
import org.opendatakit.sensors.SensorDataPacket;
import org.opendatakit.sensors.SensorDataParseResponse;
import org.opendatakit.sensors.SensorParameter;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DummySensorInternalDriver implements Driver {

   public static final String DUMMY_VALUE = "dummy-value";

   private static final String TAG = DummySensorInternalDriver.class.getSimpleName();

   private static final int MIN_BUFFER_SIZE = 2; // starter byte, plus a minimium of 1 byte payload

   private List<SensorParameter> sensorParams = new ArrayList<SensorParameter>();

   public DummySensorInternalDriver() {
      sensorParams.add(new SensorParameter(DUMMY_VALUE, SensorParameter.Type.INTEGER,
          SensorParameter.Purpose.DATA, "Dummy Value"));
   }

   @Override public SensorDataParseResponse getSensorData(long maxNumReadings,
       List<SensorDataPacket> rawSensorData, byte[] remainingData) {
      List<Bundle> allData = new ArrayList<Bundle>();
      List<Byte> dataBuffer = new ArrayList<Byte>();

      // Copy over the remaining bytes
      if (remainingData != null) {
         for (Byte b : remainingData) {
            dataBuffer.add(b);
         }
      }

      // Add the new raw data
      for (SensorDataPacket pkt : rawSensorData) {
         byte[] payload = pkt.getPayload();
         for (int i = 0; i < payload.length; i++) {
            dataBuffer.add(payload[i]);
         }
      }

      while (dataBuffer.size() >= MIN_BUFFER_SIZE) {
         byte b = dataBuffer.remove(0);
         if(b == DummySensorDataGenerator.START_DELIMINATOR) {
            // create bundles
            Bundle data = new Bundle();
            while (b != DummySensorDataGenerator.END_DELMINATOR) {
               data.putInt(DUMMY_VALUE, b);
               b = dataBuffer.remove(0);
            }
            allData.add(data);
         }
      }

      Log.i(TAG, "Finished parsing data");

      return new SensorDataParseResponse(allData, null);
   }

   @Override public byte[] configureCmd(String setting, Bundle config)
       throws ParameterMissingException {
      return null;
   }

   @Override public byte[] getSensorDataCmd() {
      return null;
   }

   @Override public byte[] startCmd() {
      return null;
   }

   @Override public byte[] stopCmd() {
      return null;
   }

   @Override public byte[] sendDataToSensor(Bundle dataToFormat) {
      return null;
   }

   @Override public List<SensorParameter> getDriverParameters() {
      return sensorParams;
   }
}
