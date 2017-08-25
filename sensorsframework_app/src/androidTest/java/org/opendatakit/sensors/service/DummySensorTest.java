package org.opendatakit.sensors.service;

import android.os.RemoteException;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.sensors.SensorsConsts;
import org.opendatakit.sensors.builtin.BuiltInSensorType;
import org.opendatakit.sensors.dummy.DummySensorDataGenerator;
import org.opendatakit.sensors.dummy.DummySensorInternalDriver;
import org.opendatakit.sensors.dummy.ODKDummyInternalSensor;

import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class DummySensorTest {

   public static final String DUMMY_INTERNAL_ID = ODKDummyInternalSensor.INTERNAL_ID;
   public static final String DEFAULT_APP_NAME = "default";
   public static final String EXCEPTION_MSG = "Got an Exception: ";

   @Rule public final ODKServiceTestRule mServiceRule = new ODKServiceTestRule();

   public ODKSensorService boundService;

   @Before public void bindToSensorService() {

      Intent bind_intent = new Intent();
      bind_intent.setClassName(SensorsConsts.frameworkPackage, SensorsConsts.frameworkService);

      int count = 0;
      IBinder srv = null;
      try {
         srv = mServiceRule.bindService(bind_intent);
      } catch (TimeoutException e) {
         e.printStackTrace();
         fail(EXCEPTION_MSG + e.getMessage());
      }

      try {
         boundService = ODKSensorService.Stub.asInterface(srv);
      } catch (IllegalArgumentException e) {
         boundService = null;
      }

   }

   @After public void cleanUpService() {
      try {
         boundService.removeAllSensors();
      } catch (RemoteException e) {
         e.printStackTrace();
         fail(EXCEPTION_MSG + e.getMessage());
      }

      boundService = null;
   }

   @Test public void testBinding() {
      ODKSensorService serviceInterface = boundService;
      assertNotNull(serviceInterface);
   }

   @Test public void testDummyBuiltInSensorExists() {
      ODKSensorService srv = boundService;
      assertNotNull(srv);

      try {
         assertTrue(srv.hasSensor(DUMMY_INTERNAL_ID));
      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }
   }

   @Test public void testDummyInternalSensorConnected() {
      ODKSensorService srv = boundService;
      assertNotNull(srv);

      try {
         assertTrue(srv.hasSensor(DUMMY_INTERNAL_ID));
         assertFalse(srv.isConnected(DUMMY_INTERNAL_ID));

         srv.sensorConnect(DUMMY_INTERNAL_ID);
         assertTrue(srv.isConnected(DUMMY_INTERNAL_ID));

      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }

   }

/*   @Test public void testLightInternalSensorConnected() {
      ODKSensorService srv = boundService;
      assertNotNull(srv);

      try {
         String id = BuiltInSensorType.LIGHT.name();
         assertTrue(srv.hasSensor(id));
         assertFalse(srv.isConnected(id));

         srv.sensorConnect(id);
         assertTrue(srv.isConnected(id));

      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }
   }*/

   @Test public void testDummyInternalSensorGetSingleDataPoint() {

      ODKSensorService srv = boundService;
      assertNotNull(srv);

      try {
         assertTrue(srv.hasSensor(DUMMY_INTERNAL_ID));
         assertFalse(srv.isConnected(DUMMY_INTERNAL_ID));

         srv.sensorConnect(DUMMY_INTERNAL_ID);
         assertTrue(srv.isConnected(DUMMY_INTERNAL_ID));

         int dataGenerationDelay = 300;
         int sleepTime = (int) (dataGenerationDelay * 1.1);

         Bundle params = new Bundle();
         params.putInt(DummySensorDataGenerator.SEND_DELAY_PARAM, dataGenerationDelay);
         srv.configure(DUMMY_INTERNAL_ID, DummySensorDataGenerator.SEND_DELAY_PARAM, params);

         srv.startSensor(DUMMY_INTERNAL_ID, false, DEFAULT_APP_NAME);

         try {
            Thread.sleep(sleepTime);
         } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }

         srv.stopSensor(DUMMY_INTERNAL_ID);
         List<Bundle> dataPoints = srv.getSensorData(DUMMY_INTERNAL_ID, 0);
         assertTrue(dataPoints.size() == 1);
         Bundle data = dataPoints.remove(0);
         assertEquals(data.getInt(DummySensorInternalDriver.DUMMY_VALUE), 1);

      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }
   }

   @Test public void testDummyInternalSensorGetSingleDataPointVaryingDelaysInGeneration() {

      ODKSensorService srv = boundService;
      assertNotNull(srv);

      try {
         assertTrue(srv.hasSensor(DUMMY_INTERNAL_ID));
         assertFalse(srv.isConnected(DUMMY_INTERNAL_ID));

         srv.sensorConnect(DUMMY_INTERNAL_ID);
         assertTrue(srv.isConnected(DUMMY_INTERNAL_ID));

         int orgDataGenerationDelay = 1000;
         int sleepTime = orgDataGenerationDelay + 5;

         for (int iteration = 1; iteration < 5; iteration++) {
            int dataGenerationDelay = orgDataGenerationDelay / iteration;

            Bundle params = new Bundle();
            params.putInt(DummySensorDataGenerator.SEND_DELAY_PARAM, dataGenerationDelay);
            srv.configure(DUMMY_INTERNAL_ID, DummySensorDataGenerator.SEND_DELAY_PARAM, params);

            // thread could be sleeping from previous loop
            try {
               Thread.sleep(orgDataGenerationDelay + 100);
            } catch (InterruptedException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }

            srv.startSensor(DUMMY_INTERNAL_ID, false, DEFAULT_APP_NAME);

            try {
               Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
            }

            srv.stopSensor(DUMMY_INTERNAL_ID);

            List<Bundle> dataPoints = srv.getSensorData(DUMMY_INTERNAL_ID, 0);

            System.err.println("Iteration: " + iteration + " Size: " + dataPoints.size());
            assertTrue(dataPoints.size() == iteration);

            for (int i = 0; i < iteration; i++) {
               Bundle data = dataPoints.remove(0);
               assertEquals(data.getInt(DummySensorInternalDriver.DUMMY_VALUE), 1);
            }
         }

      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }
   }
}
