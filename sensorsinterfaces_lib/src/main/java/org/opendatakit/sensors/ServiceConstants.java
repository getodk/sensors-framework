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
/*
 * Health Conscious Developers
 * University of Washington - CSEP590 Smartphone Mobile Computing
 * 
 * Author: Matt Wright (miwright@cs.washington.edu)
 * Author: Eric Schwabe (eschwabe@cs.washington.edu)
 * Author: Waylon Brunette (wrb@cs.washington.edu)
 * Date: 2/10/11
 *  
 * Sensor registration states
 */

package org.opendatakit.sensors;

/**
 * Contains various constants used to interface with the service, intents and broadcasts.
 *
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 */
public class ServiceConstants {

   public static final String UNKNOWN = "unknown";

   public static final String DEFAULT_APP_NAME = "TABLES";

   // Intent/Broadcast Receiver Bundle Keys
   public static final String BT_STATE_CHANGE = "bt_state_change";
   public static final String USB_STATE_CHANGE = "usb_state_change";
   public static final String ACTION_SCAN_FINISHED = "action_scan_finished";

   public static final String APP_NAME_KEY = "app_name_key";
}
