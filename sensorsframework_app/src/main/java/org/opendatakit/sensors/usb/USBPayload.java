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
package org.opendatakit.sensors.usb;

/**
 * A simple class to hold a byte array and a timestamp indicating when the
 * payload was received on the Android side. Also contains the sensor id as the
 * source/recipient of the payload.
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class USBPayload {
	private byte[] payload = new byte[0];
	private long timeStamp;
	private long sensorID;
	private int numOfReadingsInSeries;
	private long seriesTimestamp;

	private boolean readingSeries;
	
	public USBPayload(byte[] payload, long timeStamp, long sensorID,
			boolean seriesPayload) {
		this.timeStamp = timeStamp;
		this.sensorID = sensorID;
		this.readingSeries = seriesPayload;
		if (seriesPayload) {
			int size = payload.length - 5;
			this.payload = new byte[size];
			System.arraycopy(payload, 5, this.payload, 0, size);
			this.numOfReadingsInSeries = payload[4];
			this.seriesTimestamp = ((payload[3] & 0xff) << 24)
			| ((payload[2] & 0xff) << 16) | ((payload[1] & 0xff) << 8)
			| (payload[0] & 0xff);
		} else {
			this.payload = payload;
			this.numOfReadingsInSeries = 1;
			this.seriesTimestamp = -1;
		}
	}

	
	
	public boolean isReadingSeries() {
		return readingSeries;
	}



	public long getSeriesTimestamp() {
		return seriesTimestamp;
	}

	public long getAndroidTimeStamp() {
		return timeStamp;
	}

	public long getSensorID() {
		return sensorID;
	}

	public int getPayloadLength() {
		return payload.length;
	}

	public byte[] getRawBytes() {
		return payload.clone();
	}

	public String payloadAsString() {
		return new String(payload);
	}

	public int getNumOfReadingsInSeries() {
		return numOfReadingsInSeries;
	}
}
