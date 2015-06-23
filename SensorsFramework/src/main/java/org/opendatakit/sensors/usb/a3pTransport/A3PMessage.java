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
package org.opendatakit.sensors.usb.a3pTransport;

import java.util.Calendar;

import org.opendatakit.sensors.usb.USBCommon;
import org.opendatakit.sensors.usb.USBPayload;

/**
 * Represents a single A3P message that has been received or
 * will be sent over the ADK framework
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class A3PMessage {
	private A3PMsgType type;
	private byte[] payload = new byte[0];
	private long timeStamp;
	private int messageNumber;
	private byte sensorID; 
	
	A3PMessage(A3PMsgType messageType, int messageNumber, byte sensorID, byte[] payload){
		this.type = messageType;
		this.messageNumber = messageNumber;
		this.payload = payload;
		this.timeStamp = Calendar.getInstance().getTimeInMillis();
		this.sensorID = sensorID;
	}
	
	public int getMessageNumber(){
		return messageNumber;
	}
	
	public int getPayloadLength(){
		return payload.length;
	}
	
	public A3PMsgType getMessageType(){
		return type;
	}
	
	public A3PMsgClass getMessageClass(){
		return type.msgClass();
	}
	
	public byte getSensorID() {
		return sensorID;
	}
	
	public String toString(){
		return "A3PMESSAGE_ Type: " + type.toString() + " Number: " + messageNumber + " SensorID: " + sensorID + " Payload length: " +
			payload.length + " Timestamp: " + timeStamp + "\nPayload: " + ((payload.length > 0) ? packetAsString() : " (no payload) ");
	}
	
	/**
	 * Returns the value of the payload bytes as a spaced hexadecimal string with a trailing
	 * space on the last byte pair. (Ex: 0x 1234 B78B )
	 * @return the value of the payload bytes as a spaced hexadecimal string.
	 */
	public String packetAsString(){
		String dataReceived = "0x ";
		
		for (int i = 0; i < payload.length; i++){
			
			int temp = USBCommon.byteToIntUnsigned(payload[i]);

			String tempString = Integer.toHexString(temp);

			while(tempString.length() < 2){
				tempString = "0" + tempString;
			}
			
			dataReceived += tempString + ((i %2 == 0) ? "" : " ");
		}
		
		return dataReceived;
	}
	
	/**
	 * Builds a byte array from the current fields of this A3PMessage to prepare it for sending.
	 * @return a ready-to-send byte array that represents this A3PMessage.
	 */
	public byte[] bytesToSend(){
		
		int fullSize = payload.length + A3PCommon.SIZE_OF_NO_PAYLOAD_MESSAGE;
		byte[] fullMessage = new byte [fullSize];
		
		byte[] messageTypeBytes = USBCommon.getTwoBytesFromInt(type.code());
		byte[] messageNumberBytes = USBCommon.getTwoBytesFromInt(messageNumber);
		byte[] payloadLengthBytes = USBCommon.getTwoBytesFromInt(payload.length);
		
		fullMessage[0] = A3PCommon.PREAMBLE_HI; 
		fullMessage[1] = A3PCommon.PREAMBLE_LOW;
		
		fullMessage[2] = messageTypeBytes[1];
		fullMessage[3] = messageTypeBytes[0];
		
		fullMessage[4] = messageNumberBytes[1];
		fullMessage[5] = messageNumberBytes[0];
		
		fullMessage[6] = sensorID;
		
		fullMessage[7] = payloadLengthBytes[1];
		fullMessage[8] = payloadLengthBytes[0];
		
		for (int i = 0; i < payload.length; i++){
			fullMessage[A3PCommon.SIZE_OF_NO_PAYLOAD_MESSAGE + i - 1] = payload[i];
		}
		
		// The crc code!
		fullMessage[fullSize - 1] = A3PCommon.calculateCRC(messageTypeBytes, messageNumberBytes, sensorID, payloadLengthBytes, payload);
		
		return fullMessage;
	}
	
	/**
	 * Returns a clone of the payload byte array.
	 * @return a clone of the payload byte array.
	 */
	public byte[] getPayload(){
		return payload.clone(); 
	}
	
	/**
	 * Returns a clone of the payload as a USBPayload object.
	 * @return a clone of the payload as a USBPayload object.
	 */
	public USBPayload getUSBPayload(){
		// TODO: JAYLEN IS THIS RIGHT??
		if(type.equals(A3PMsgType.LONG_DATALOGPAYLOAD_SENSE_TYPE))
			return new USBPayload(getPayload(), timeStamp, sensorID, true);
		else
			return new USBPayload(getPayload(), timeStamp, sensorID, false);
	}
}