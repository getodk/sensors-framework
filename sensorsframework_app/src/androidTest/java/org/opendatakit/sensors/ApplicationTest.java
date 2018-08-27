package org.opendatakit.sensors;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.sensors.service.ODKSensorService;

import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;


@RunWith(AndroidJUnit4.class)
public class ApplicationTest {

   private final static String LOGTAG = ApplicationTest.class.getCanonicalName();

   @Rule
   public final ServiceTestRule mServiceRule = new ServiceTestRule();

   private ODKSensorService bindToService() {
      Context context = InstrumentationRegistry.getContext();
      Intent bind_intent = new Intent();
      bind_intent.setClassName(SensorsConsts.frameworkPackage, SensorsConsts.frameworkService);

      int count = 0;
      ODKSensorService sensorService;
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
         sensorService = ODKSensorService.Stub.asInterface(service);
      } catch (IllegalArgumentException e) {
         sensorService = null;
      }
      return sensorService;
   }

   @Test
   public void testBinding() {
      ODKSensorService serviceInterface = bindToService();
      assertNotNull( "bind did not succeed", serviceInterface);
   }
}