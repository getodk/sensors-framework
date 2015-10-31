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
package org.opendatakit.sensors.bluetooth.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import org.opendatakit.sensors.*;
import org.opendatakit.sensors.bluetooth.BluetoothManager;
import org.opendatakit.sensors.drivers.ManifestMetadata;
import org.opendatakit.sensors.exception.IdNotFoundException;
import org.opendatakit.sensors.manager.DiscoverableDevice;
import org.opendatakit.sensors.manager.DiscoverableDeviceState;
import org.opendatakit.sensors.ui.ISensorSelectorItem;
import org.opendatakit.sensors.ui.SensorsAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class SensorBtSelectionActivity extends Activity {

   // logging
   private static final String LOGTAG = "SensorSelectionActivity";
   private static final boolean LOGENABLED = true;

   // message handler IDs
   private static final int STATE_CHANGE_MSG = 1;
   private static final int SCAN_FINISHED_MSG = 2;
   private static final int REQUEST_ENABLE_BT = 3;

   private static final String M_SENSOR_ITEMS = "mSensorItems";

   // UI objects
   private List<ISensorSelectorItem> mSensorItems = new ArrayList<ISensorSelectorItem>();
   private ListView mSensorList;
   private SensorsAdapter mSensorsAdapter;
   private ToggleButton mSearchButton = null;

   private String mProgressId = "";
   private ProgressDialog mRegisterProgress = null;

   private View metadialogView;

   private BluetoothManager btManager;

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

      setContentView(R.layout.sensor_selection);

      mSensorList = (ListView) findViewById(R.id.lvSensors);

      mSensorList.setAdapter(mSensorsAdapter);
      mSensorList.setClickable(true);
      mSensorList.setOnItemClickListener(mItemClickListener);

      // buttons
      Button mHelpButton = (Button) findViewById(R.id.help_button);
      mHelpButton.setOnClickListener(mHelpButtonListener);
      mSearchButton = (ToggleButton) findViewById(R.id.search_button);
      mSearchButton.setOnClickListener(mSearchListener);

      // TODO: FIX THIS AS WE NEED TO NOT WAIT ON THESE
      // handle sensor state change intents from service
      getApplicationContext().registerReceiver(mSensorStateChangeReceiver,
          new IntentFilter(ServiceConstants.BT_STATE_CHANGE));
      getApplicationContext()
          .registerReceiver(mScanFinished, new IntentFilter(ServiceConstants.ACTION_SCAN_FINISHED));

      updateSensorList();

      btManager = SensorsSingleton.getBluetoothManager();
   }

   /**
    * Save UI State
    *
    * @param outState bundle to save state into
    */
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
    * Handles Bluetooth Scan After Bluetooth Is Enabled
    */
   @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {

      // Handle Result Code From Enabling Bluetooth
      if (requestCode == REQUEST_ENABLE_BT) {
         if (resultCode == RESULT_OK) {
            toggleScan(true);
         }
      }
      super.onActivityResult(requestCode, resultCode, data);
   }

   /**
    * Cleanup activity
    */
   @Override public void onDestroy() {
      if (LOGENABLED)
         Log.w(LOGTAG, "onDestroy");

      // TODO: fix this based on what we do
      getApplicationContext().unregisterReceiver(mSensorStateChangeReceiver);
      getApplicationContext().unregisterReceiver(mScanFinished);
      super.onDestroy();
   }

   /**
    * Help button listener
    */
   private Button.OnClickListener mHelpButtonListener = new Button.OnClickListener() {
      public void onClick(View v) {
         // start help activity
         Intent hi = new Intent(getApplicationContext(), SensorBtSelectionHelpActivity.class);
         startActivityForResult(hi, 0);
      }
   };

   /**
    * Scan Button Listener
    */
   private Button.OnClickListener mSearchListener = new Button.OnClickListener() {
      public void onClick(View v) {
         // enable bluetooth scanning
         if (bluetoothIsEnabled(true) == true)
            toggleScan(true);
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
            btManager.sensorRegister(si.getSensorId(), driver, appName);

            // Just Leave On Cancel
         case DialogInterface.BUTTON_NEGATIVE:
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
         else if (mSensorsAdapter.sensorIsUnregistered(position)) {
            Log.d(LOGTAG, "Sensor is unregistered");
            showMetaDataDialog(position);
         }

         // Sensor Requires Pairing: Bring Up BT Pairing Dialog
         else if (mSensorsAdapter.sensorIsUnpaired(position)) {
            Log.d(LOGTAG, "Sensor is unpaired");
            showBluetoothPairDialog();
         }

         // Sensor is registered and paired
         else {
            // Set result and finish this Activity.
            // Application decides how to proceed based on SENSOR_STATE
            Log.d(LOGTAG, "Sensor is registered and paired, but not connected");
            String sensorId = mSensorsAdapter.getId(position);
            Intent intent = new Intent();
            intent.putExtra(Constants.SENSOR_ID, sensorId);
            setResult(RESULT_OK, intent);
            finish();
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
    * Updates The Sensor List -- Calls Service To Determine List And Updates
    */
   private void updateSensorList() {
      if (btManager != null) {
         // request sensor list from service
         List<DiscoverableDevice> sensorList = btManager.getDiscoverableSensor();

         // load sensor list into list adapter
         for (DiscoverableDevice device : sensorList) {
            String id = device.getDeviceId();
            DiscoverableDeviceState state = device.getDeviceState();
            String name = device.getDeviceName();
            if (name == null || name.equals("")) {
               name = id;
            }
            Log.d(LOGTAG, "Id: " + id + "  state: " + state + " name: " + name);

            try {
               mSensorsAdapter.sensorStateChange(id, state, name);
            } catch (IdNotFoundException e) {
               ISensorSelectorItem sensor = new SensorBtSelectorItem(id, state, name);
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

   /**
    * Handles State Change Intents From Sensors, Passes MSG To UI Thread
    */
   private final BroadcastReceiver mSensorStateChangeReceiver = new BroadcastReceiver() {

      public void onReceive(Context context, Intent intent) {

         Log.d(LOGTAG, "Got state change event");
         Message newMsg = mStateChangeHandler.obtainMessage(STATE_CHANGE_MSG);
         newMsg.setData(intent.getExtras());
         mStateChangeHandler.sendMessage(newMsg);
      }
   };

   /**
    * Handles State Change Intents From Sensors, Passes MSG To UI Thread
    */
   private final BroadcastReceiver mScanFinished = new BroadcastReceiver() {

      public void onReceive(Context context, Intent intent) {
         Message newMsg = mStateChangeHandler.obtainMessage(SCAN_FINISHED_MSG);
         newMsg.setData(intent.getExtras());
         mStateChangeHandler.sendMessage(newMsg);
         Log.d(LOGTAG, "scan finished");
      }
   };

   /**
    * Processes State Change Messages
    */
   private Handler mStateChangeHandler = new Handler() {
      @Override public void handleMessage(Message msg) {

         if (msg.what == STATE_CHANGE_MSG) {
            updateSensorList();
         } else if (msg.what == SCAN_FINISHED_MSG) {
            mSearchButton.setChecked(false);
            updateSensorList();
         } else {
            super.handleMessage(msg);
         }
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

      List<DriverType> drivers = SensorDriverDiscovery
          .getAllDriversForChannel(this, CommunicationChannelType.BLUETOOTH,
              ManifestMetadata.FRAMEWORK_VERSION_2);

      for (DriverType driver : drivers) {
         Log.d(LOGTAG, "driver type: " + driver.getSensorType() + " address: " + driver
             .getSensorDriverAddress());
      }

      ArrayAdapter<DriverType> adapter = new ArrayAdapter<DriverType>(this,
          android.R.layout.simple_spinner_item, drivers);
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

      // Log.d(LOGTAG,adapter.getCount()+"");

      Spinner spinner = (Spinner) getMetaDialogSpinner(metadialogView);
      if (spinner == null) {
         Log.w(LOGTAG, "Spinner is null");
      }
      spinner.setAdapter(adapter);
      Log.d(LOGTAG, spinner.getCount() + "");

      // Positive Button & Negative Button & handler
      alert.setPositiveButton("Select", mMetaDiagListener);
      alert.setNegativeButton("Cancel", mMetaDiagListener);
      alert.show();
   }

   /**
    * Prompts The User To See If They Want To Go To Bluetooth Settings
    */
   private void showBluetoothPairDialog() {

      AlertDialog.Builder alert = new AlertDialog.Builder(this);

      alert.setTitle("Pair Phone With Sensor?");
      alert.setMessage(
          "Pressing Yes will allow you to add new sensors ...  \n\nPress back when finished ...");

      // set alert 'yes' button
      alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
            // show bluetooth settings activity
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
         }
      });

      // set alert 'no' button
      alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
            // ignore
            return;
         }
      });

      alert.show();
   }

   /**
    * Confirmation Dialog To Enable Bluetooth
    *
    * @param prompt true to display dialog prompt to enable
    */
   private boolean bluetoothIsEnabled(boolean prompt) {
      boolean isEnabled = false;
      BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
      if (ba != null) {
         isEnabled = ba.isEnabled();
         if (isEnabled == false && prompt == true) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
         }
      }
      return isEnabled;
   }

   /**
    * Toggle bluetooth scanning
    *
    * @param on true to enable scanning, false to disable
    */
   private void toggleScan(boolean on) {
      if (btManager != null) {
         if (on) {
            if (bluetoothIsEnabled(false) == true) {
               btManager.searchForSensors();
               mSearchButton.setChecked(true);
            }
         } else {
            btManager.stopSearchingForSensors();
            mSearchButton.setChecked(false);
         }
      }

   }

   private View getMetaDialog() {
      LayoutInflater factory = LayoutInflater.from(this);
      return factory.inflate(R.layout.metadata_dialog, null);
   }

   private Spinner getMetaDialogSpinner(View metaDialog) {
      return (Spinner) metaDialog.findViewById(R.id.sensor_type_spinner);
   }
}
