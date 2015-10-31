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
package org.opendatakit.sensors.usb.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import org.opendatakit.sensors.*;
import org.opendatakit.sensors.drivers.ManifestMetadata;
import org.opendatakit.sensors.exception.IdNotFoundException;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.manager.DiscoverableDeviceState;
import org.opendatakit.sensors.ui.ISensorSelectorItem;
import org.opendatakit.sensors.ui.SensorsAdapter;
import org.opendatakit.sensors.usb.USBManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class SensorUsbSelectionActivity extends Activity {

   // logging
   private static final String LOGTAG = SensorUsbSelectionActivity.class.getSimpleName();
   private static final boolean LOGENABLED = true;

   private static final String M_SENSOR_ITEMS = "mSensorItems";

   // UI objects
   private List<ISensorSelectorItem> mSensorItems = new ArrayList<ISensorSelectorItem>();
   private ListView mSensorList;
   private SensorsAdapter mSensorsAdapter;

   private String mProgressId = "";
   private ProgressDialog mRegisterProgress = null;

   private View metadialogView;

   private USBManager usbManager;
   private int mMetaDialogPosition = 0;

   private String appName;

   /**
    * Called when activity is first created
    */
   @Override public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      Intent intent = getIntent();
      String tmpAppName = intent.getStringExtra(ServiceConstants.APP_NAME_KEY);
      if (tmpAppName == null) {
         // TODO: change to get the default from preferences instead of hardcode
         appName = ServiceConstants.DEFAULT_APP_NAME;
      } else {
         appName = tmpAppName;
      }

      mSensorsAdapter = new SensorsAdapter(getApplicationContext(), mSensorItems);

      setContentView(R.layout.usb_sensor_selection);

      mSensorList = (ListView) findViewById(R.id.lvSensors);

      mSensorList.setAdapter(mSensorsAdapter);
      mSensorList.setClickable(true);
      mSensorList.setOnItemClickListener(mItemClickListener);

      // buttons
      Button mHelpButton = (Button) findViewById(R.id.help_button);
      mHelpButton.setOnClickListener(mHelpButtonListener);

      getApplicationContext().registerReceiver(mSensorStateChangeReceiver,
          new IntentFilter(ServiceConstants.USB_STATE_CHANGE));

      usbManager = SensorsSingleton.getUSBManager();
      updateSensorList();
   }

   /**
    * Cleanup activity
    */
   @Override public void onDestroy() {
      if (LOGENABLED)
         Log.w(LOGTAG, "onDestroy");

      getApplicationContext().unregisterReceiver(mSensorStateChangeReceiver);
      super.onDestroy();
   }

   @Override protected void onSaveInstanceState(Bundle outState) {
      if (LOGENABLED)
         Log.w(LOGTAG, "onSaveInstanceState");

      // Save SensorItems To Bundle -- SensorItems Is Parelable
      outState
          .putParcelableArrayList(M_SENSOR_ITEMS, (ArrayList<? extends Parcelable>) mSensorItems);
      super.onSaveInstanceState(outState);
   }

   /**
    * Restore UI State
    *
    * @param savedInstanceState bundle to restore state from
    */
   @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
      if (LOGENABLED)
         Log.w(LOGTAG, "onRestoreInstanceState");

      // Restore UI State From Bundle -- Contains Array List Of SensorItems
      mSensorItems = savedInstanceState.getParcelableArrayList(M_SENSOR_ITEMS);
      mSensorsAdapter = new SensorsAdapter(this, mSensorItems);
      mSensorList.setAdapter(mSensorsAdapter);
      mSensorsAdapter.notifyDataSetChanged();
      super.onRestoreInstanceState(savedInstanceState);
   }

   /**
    * Help button listener
    */
   private Button.OnClickListener mHelpButtonListener = new Button.OnClickListener() {
      public void onClick(View v) {
         // start help activity
         Intent hi = new Intent(getApplicationContext(), SensorUsbSelectionHelpActivity.class);
         startActivityForResult(hi, 0);
      }
   };

   /**
    * Handles A Metadata Dialog Update
    */
   private final android.content.DialogInterface.OnClickListener mMetaDiagListener = new DialogInterface.OnClickListener() {

      public void onClick(DialogInterface dialog, int whichButton) {

         Log.d(LOGTAG, "A driver selected from the spinner");
         // Lookup Views And Selector Item
         Spinner spinner = getMetaDialogSpinner(metadialogView);

         ISensorSelectorItem si = (ISensorSelectorItem) mSensorsAdapter
             .getItem(mMetaDialogPosition);

         DriverType driver = (DriverType) spinner.getSelectedItem();

         switch (whichButton) {

         // On Positive Set The Text W/ The ListItem
         case DialogInterface.BUTTON_POSITIVE:
            showRegistrationProgress(si.getSensorId(), driver.getSensorType());
            usbManager.sensorRegister(si.getSensorId(), driver, appName);

            // Just Leave On Cancel
         default:
            dialog.cancel();
            break;
         }
      }
   };

   /**
    * Handles sensor list short clicks
    */
   private OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
      public void onItemClick(AdapterView<?> parentView, View view, int position, long id) {

         // Sensor Ready: Display The Data View Activity For Sensor
         if (mSensorsAdapter.sensorIsReady(position)) {
            // Set result and finish this Activity
            // Application decides how to proceed based on SENSOR_STATE
            Log.d(LOGTAG, "Sensor is ready and connected");
            String sensorId = mSensorsAdapter.getId(position);
            Intent intent = new Intent();
            intent.putExtra(Constants.SENSOR_ID, sensorId);
            setResult(RESULT_OK, intent);
            finish();
         }

         // Sensor Unregistered: Register Sensor
         else {
            Log.d(LOGTAG, "Sensor is unregistered");
            showMetaDataDialog(position);
         }
      }
   };

   /**
    * Show sensor registration progress dialog
    *
    * @param id   sensor id
    * @param name sensor name
    */
   private void showRegistrationProgress(String id, String name) {
      mRegisterProgress = ProgressDialog
          .show(this, "", "Registering Sensor: " + name + ", Please wait...", true);
      mProgressId = id;
   }

   /**
    * Save UI State
    */

   private ISensorSelectorItem createSensorSelectorItem(String id, DiscoverableDeviceState sState,
       String name) {
      return new SensorUsbSelectorItem(id, sState, name);
   }

   private View getMetaDialog() {
      LayoutInflater factory = LayoutInflater.from(this);
      return factory.inflate(R.layout.metadata_dialog, null);
   }

   private Spinner getMetaDialogSpinner(View metaDialog) {
      return (Spinner) metaDialog.findViewById(R.id.sensor_type_spinner);
   }

   private List<DriverType> getDrivers() {
      return SensorDriverDiscovery.getAllDriversForChannel(this, CommunicationChannelType.USB,
          ManifestMetadata.FRAMEWORK_VERSION_2);
   }

   /**
    * Updates The Sensor List -- Calls Service To Determine List And Updates
    */
   private void updateSensorList() {
      Log.d(LOGTAG, "entered uodateSensorList");
      if (usbManager != null) {
         Log.d(LOGTAG, "calling usbManager.getDiscSensors()");
         // request sensor list from service
         List<DiscoverableDevice> sensorList = usbManager.getDiscoverableSensor();
         if (sensorList != null) {
            // load sensor list into list adapter
            for (DiscoverableDevice device : sensorList) {
               String id = device.getDeviceId();
               DiscoverableDeviceState state = device.getDeviceState();
               Log.d(LOGTAG, "Id: " + id + "  state: " + state);
               String name = device.getDeviceName();

               try {
                  mSensorsAdapter.sensorStateChange(id, state, name);
               } catch (IdNotFoundException e) {
                  ISensorSelectorItem sensor = createSensorSelectorItem(id, state, name);
                  mSensorsAdapter.addSensor(sensor);
               }
               mSensorsAdapter.notifyDataSetChanged();

               if (id.compareTo(mProgressId) == 0) {
                  if (mRegisterProgress != null) {
                     mRegisterProgress.dismiss();
                  }
               }
            }
         }
      }

   }

   /**
    * Handles State Change Intents From Sensors, Passes MSG To UI Thread
    */
   private final BroadcastReceiver mSensorStateChangeReceiver = new BroadcastReceiver() {

      public void onReceive(Context context, Intent intent) {

         Log.d(LOGTAG, "Got state change event");
         updateSensorList();
      }
   };

   /**
    * Displays The MetaDataEditor Dialog
    *
    * @param position Position Of The Item That Was Clicked
    */
   private void showMetaDataDialog(int position) {

      // Setup Dialog
      final AlertDialog.Builder alert = new AlertDialog.Builder(this);

      if (metadialogView == null)
         metadialogView = getMetaDialog();

      // Set Text To Existing Values
      alert.setView(metadialogView);

      // Save Off Position For Click Handling
      mMetaDialogPosition = position;

      List<DriverType> drivers = getDrivers();

      for (DriverType driver : drivers) {
         Log.d(LOGTAG, "driver type: " + driver.getSensorType() + " address: " + driver
             .getSensorDriverAddress());
      }

      ArrayAdapter<DriverType> adapter = new ArrayAdapter<DriverType>(this,
          android.R.layout.simple_spinner_item, drivers);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

      //Log.d(LOGTAG,adapter.getCount()+"");

      Spinner spinner = (Spinner) getMetaDialogSpinner(metadialogView);
      if (spinner == null) {
         Log.w(LOGTAG, "Spinner is null");
      }
      spinner.setAdapter(adapter);
      Log.d(LOGTAG, spinner.getCount() + "");

      // Positive Button & Negative Button & Handler
      alert.setPositiveButton("Select", mMetaDiagListener);
      alert.setNegativeButton("Cancel", mMetaDiagListener);
      alert.show();
   }

}
