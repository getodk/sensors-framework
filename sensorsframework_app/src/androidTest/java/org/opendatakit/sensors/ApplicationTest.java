package org.opendatakit.sensors;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.test.rule.ServiceTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.opendatakit.sensors.service.IODKSensorService;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;


public class ApplicationTest {

   private final static String LOGTAG = ApplicationTest.class.getCanonicalName();

   @Rule
   public final ServiceTestRule mServiceRule = new ServiceTestRule();

   private IODKSensorService bindToService() {
      Intent bind_intent = new Intent();
      bind_intent.setClassName(SensorsConsts.frameworkPackage, SensorsConsts.frameworkService);

      int count = 0;
      IODKSensorService sensorService;
      try {
         IBinder service = null;
         while ( service == null ) {
            try {
               service = mServiceRule.bindService(bind_intent);
            } catch (TimeoutException e) {
               service = null;
            }
            if ( service == null ) {
               ++count;
               if ( count % 20 == 0 ) {
                  Log.i(LOGTAG, "bindToDbService failed for " + count);
               }
               try {
                  Thread.sleep(10);
               } catch (InterruptedException e) {
               }
            }
         }
         sensorService = IODKSensorService.Stub.asInterface(service);
      } catch (IllegalArgumentException e) {
         sensorService = null;
      }
      return sensorService;
   }

   @Test
   public void testBinding() {
      IODKSensorService serviceInterface = bindToService();
      assertNotNull( "bind did not succeed", serviceInterface);
   }
}