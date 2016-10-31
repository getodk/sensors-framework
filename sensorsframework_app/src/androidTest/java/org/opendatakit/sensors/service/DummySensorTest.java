package org.opendatakit.sensors.service;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.test.ServiceTestCase;
import org.opendatakit.sensors.SensorsConsts;
import org.opendatakit.sensors.builtin.BuiltInSensorType;
import org.opendatakit.sensors.dummy.DummySensorDataGenerator;
import org.opendatakit.sensors.dummy.DummySensorInternalDriver;
import org.opendatakit.sensors.dummy.ODKDummyInternalSensor;

import java.util.List;

public class DummySensorTest extends ServiceTestCase<SensorService> {

   public static final String DUMMY_INTERNAL_ID = ODKDummyInternalSensor.INTERNAL_ID;
   public static final String DEFAULT_APP_NAME = "default";
   public static final String EXCEPTION_MSG = "Got an Exception: ";


   public DummySensorTest() {
      super(SensorService.class);
   }

   public DummySensorTest(Class<SensorService> serviceClass) {
      super(serviceClass);
   }

   @Override protected void setUp() throws Exception {
      super.setUp();
      setupService();
   }

   @Override protected void tearDown() throws Exception {
      super.tearDown();
   }

   @Nullable private ODKSensorService bindToSensorService() {
      Intent bind_intent = new Intent();
      bind_intent.setClassName(SensorsConsts.frameworkPackage, SensorsConsts.frameworkService);
      IBinder service = this.bindService(bind_intent);

      ODKSensorService srv;
      try {
         srv = ODKSensorService.Stub.asInterface(service);
      } catch (IllegalArgumentException e) {
         srv = null;
      }
      return srv;
   }

   public void testBinding() {
      ODKSensorService serviceInterface = bindToSensorService();
      assertNotNull(serviceInterface);
   }

   public void testDummyBuiltInSensorExists() {
      ODKSensorService srv = bindToSensorService();
      assertNotNull(srv);

      try {
         assertTrue(srv.hasSensor(DUMMY_INTERNAL_ID));
      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }
   }

   public void testDummyInternalSensorConnected() {
      ODKSensorService srv = bindToSensorService();
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

   public void testLightInternalSensorConnected() {
      ODKSensorService srv = bindToSensorService();
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
   }

   public void testDummyInternalSensorGetSingleDataPoint() {

      ODKSensorService srv = bindToSensorService();
      assertNotNull(srv);

      try {
         assertTrue(srv.hasSensor(DUMMY_INTERNAL_ID));
         assertFalse(srv.isConnected(DUMMY_INTERNAL_ID));

         srv.sensorConnect(DUMMY_INTERNAL_ID);
         assertTrue(srv.isConnected(DUMMY_INTERNAL_ID));

         int dataGenerationDelay = 300;
         int sleepTime = (int)(dataGenerationDelay * 1.1);

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
         assertEquals(data.getInt(DummySensorInternalDriver.DUMMY_VALUE),1);

      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }
   }

   public void testDummyInternalSensorGetSingleDataPointVaryingDelaysInGeneration(){

      ODKSensorService srv = bindToSensorService();
      assertNotNull(srv);

      try {
         assertTrue(srv.hasSensor(DUMMY_INTERNAL_ID));
         assertFalse(srv.isConnected(DUMMY_INTERNAL_ID));

         srv.sensorConnect(DUMMY_INTERNAL_ID);
         assertTrue(srv.isConnected(DUMMY_INTERNAL_ID));

         int orgDataGenerationDelay = 1000;
         int sleepTime = orgDataGenerationDelay + 5;

         for(int iteration=1; iteration < 5; iteration++) {
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

            for(int i = 0; i<iteration; i++) {
               Bundle data = dataPoints.remove(0);
               assertEquals(data.getInt(DummySensorInternalDriver.DUMMY_VALUE), 1);
            }
         }

      } catch (Exception e) {
         fail(EXCEPTION_MSG + e.getMessage());
      }
   }
}
