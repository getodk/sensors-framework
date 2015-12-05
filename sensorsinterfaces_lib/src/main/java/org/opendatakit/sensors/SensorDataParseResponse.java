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

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public final class SensorDataParseResponse implements Parcelable {

	private final byte[] remainingData;
	
	private final List<Bundle> sensorData;
	
	public SensorDataParseResponse(List<Bundle> sensorData, byte[] remainingData) {
		this.sensorData = sensorData;
		this.remainingData = remainingData;
	}
	
	public SensorDataParseResponse(Parcel p) {
		sensorData = new ArrayList<Bundle>();
		p.readList(sensorData, null);
		remainingData = p.createByteArray();
//		Log.d("SDP", "read payload of length: " + payload.length);
	}

	
	
	public byte[] getRemainingData() {
		return remainingData;
	}

	public List<Bundle> getSensorData() {
		return sensorData;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(sensorData);
		dest.writeByteArray(remainingData);
	}

	public static final Parcelable.Creator<SensorDataParseResponse> CREATOR = new Parcelable.Creator<SensorDataParseResponse>() {
		public SensorDataParseResponse createFromParcel(Parcel in) {
			return new SensorDataParseResponse(in);
		}

		public SensorDataParseResponse[] newArray(int size) {
			return new SensorDataParseResponse[size];
		}
	};

}
