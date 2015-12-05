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
public final class SensorParameter implements Parcelable{
	public enum Type {
		BOOLEAN,
		BOOLEANARRAY,
		BYTE,
		BYTEARRAY,
		DOUBLE,
		DOUBLEARRAY,
		FLOAT,
		FLOATARRAY,
		INTEGER,
		INTEGERARRAY,
		INTEGERARRAYLIST,
		LONG,
		LONGARRAY,
		PARCELABLE,
		PARCELABLEARRAY,
		PARCELABLEARRAYLIST,
		SERIALIZABLE,
		STRING,
		STRINGARRAY,
		STRINGARRAYLIST,
		VOID;
	}
	
	
	public enum Purpose {
		DATA,
		CONFIG,
		ACTION;
	}
	
	private final String keyName;
	
	private final Type valueType;
	
	private final String valueDescription;
	
	public SensorParameter(String keyName, Type valueType, Purpose valuePurpose, String valueDescription) {
		this.keyName = keyName;
		this.valueType = valueType;
		this.valueDescription = valueDescription;
	}

	public SensorParameter(Parcel p) {
		keyName = p.readString();
		valueType = Type.valueOf(p.readString());
		valueDescription = p.readString();
	}
	
	public String getKeyName() {
		return keyName;
	}

	public Type getValueType() {
		return valueType;
	}

	public String getValueDescription() {
		return valueDescription;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(keyName);
		dest.writeString(valueType.name());
		dest.writeString(valueDescription);
	}
	
	public static final Parcelable.Creator<SensorParameter> CREATOR = new Parcelable.Creator<SensorParameter>() {
		public SensorParameter createFromParcel(Parcel in) {
			return new SensorParameter(in);
		}

		public SensorParameter[] newArray(int size) {
			return new SensorParameter[size];
		}
	};
}
