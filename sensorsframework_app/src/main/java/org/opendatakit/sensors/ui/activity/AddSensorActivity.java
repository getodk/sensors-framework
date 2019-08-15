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
package org.opendatakit.sensors.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import org.opendatakit.sensors.Constants;
import org.opendatakit.sensors.R;
import org.opendatakit.sensors.SensorsSingleton;
import org.opendatakit.sensors.ServiceConstants;
import org.opendatakit.utilities.RuntimePermissionUtils;

/**
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class AddSensorActivity extends Activity {

   private static final String LOGTAG = AddSensorActivity.class.getSimpleName();

   protected static final String[] REQUIRED_PERMISSIONS = new String[] {
       Manifest.permission.BLUETOOTH_ADMIN,
       Manifest.permission.BLUETOOTH,
       Manifest.permission.WRITE_EXTERNAL_STORAGE,
       Manifest.permission.ACCESS_COARSE_LOCATION
   };

   protected static final int PERMISSION_REQ_CODE = 5;

   private static final int RESULT_OK_BT = 1;
   private static final int RESULT_OK_USB = 2;

   private String appName;

   @Override public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      if (!RuntimePermissionUtils.checkSelfAllPermission(this, REQUIRED_PERMISSIONS)) {
         ActivityCompat.requestPermissions(
             this,
             REQUIRED_PERMISSIONS,
             PERMISSION_REQ_CODE
         );
      }

      Intent intent = getIntent();
      String tmpAppName = intent.getStringExtra(ServiceConstants.APP_NAME_KEY);
      if (tmpAppName == null) {
         appName = SensorsSingleton.defaultAppName();
      } else {
         appName = tmpAppName;
      }
      setContentView(R.layout.add_sensor);
   }

   public void addUSBAction(View view) {
      // need to add a USB sensor
      Intent usbSensorSelectionIntent = new Intent(this,
          org.opendatakit.sensors.usb.ui.SensorUsbSelectionActivity.class);
      usbSensorSelectionIntent.putExtra(ServiceConstants.APP_NAME_KEY, appName);
      this.startActivityForResult(usbSensorSelectionIntent, RESULT_OK_USB);
   }

   public void addBluetoothAction(View view) {
      // need to add a BT sensor
      Intent btSensorSelectionIntent = new Intent(this,
          org.opendatakit.sensors.bluetooth.ui.SensorBtSelectionActivity.class);
      btSensorSelectionIntent.putExtra(ServiceConstants.APP_NAME_KEY, appName);
      this.startActivityForResult(btSensorSelectionIntent, RESULT_OK_BT);
   }

   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      // Log.d(LOGTAG,"onActivityResult: resultCode: " + resultCode +
      // " requestCode: " + requestCode);
      switch (requestCode) {
      case RESULT_OK_USB:
         if (data != null) {
            Log.d(LOGTAG,
                "Finished discover USB sensor: " + data.getStringExtra(Constants.SENSOR_ID));
            setResult(resultCode, data);
         }
         finish();
         break;
      case RESULT_OK_BT:
         // Finish this Activity by passing data from sensor select activity
         // back to application
         if (data != null) {
            Log.d(LOGTAG,
                "Finished discover bluetooth sensor: " + data.getStringExtra(Constants.SENSOR_ID));
            setResult(resultCode, data);
         }
         finish();
         break;
      }
   }

   @Override
   public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);

      if (requestCode != PERMISSION_REQ_CODE) {
         return;
      }

      boolean granted = true;
      if (grantResults.length > 0) {
         for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
               granted = false;
            }
         }

         if (granted)
            return;

         if (RuntimePermissionUtils.shouldShowAnyPermissionRationale(this, permissions)) {
            RuntimePermissionUtils.createPermissionRationaleDialog(this, requestCode, permissions).setMessage(R.string.write_external_storage_rationale)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   @Override public void onClick(DialogInterface dialog, int which) {
                      dialog.cancel();
                      setResult(Activity.RESULT_CANCELED);
                      finish();
                   }
                }).show();
         } else {
            Toast.makeText(this, R.string.write_external_perm_denied, Toast.LENGTH_LONG).show();
            setResult(Activity.RESULT_CANCELED);
            finish();
         }
      }
   }
}
