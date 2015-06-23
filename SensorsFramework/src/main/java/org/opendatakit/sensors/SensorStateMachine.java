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
package org.opendatakit.sensors;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class SensorStateMachine implements Parcelable {

	//TODO: should these be public or not?
	public SensorStatus status;
	public SensorWorkStatus workStatus;

	public SensorStateMachine() {
		status = SensorStatus.DISCONNECTED;
		workStatus = SensorWorkStatus.NOT_BUSY;
	}

	public SensorStateMachine(Parcel in) {
		status = SensorStatus.valueOf(in.readString());
		workStatus = SensorWorkStatus.valueOf(in.readString());
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(status.name());
		dest.writeString(workStatus.name());
	}

	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<SensorStateMachine> CREATOR = new Parcelable.Creator<SensorStateMachine>() {
		public SensorStateMachine createFromParcel(Parcel in) {
			return new SensorStateMachine(in);
		}

		public SensorStateMachine[] newArray(int size) {
			return new SensorStateMachine[size];
		}
	};
}