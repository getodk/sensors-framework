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

/**
 * Defines message types for A3P
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public enum A3PMsgType {

	// Setup message codes
	SETUP_HANDSHAKE_SENSE_TYPE 			(0x11),
	SETUP_HANDSHAKE_COMMAND_TYPE 		(0x12),
	SETUP_SLEEPSTART_SENSE_TYPE 		(0x13),
	SETUP_WAKEUPHANDSHAKE_SENSE_TYPE	(0x14),
	SETUP_WAKEUPHANDSHAKE_COMMAND_TYPE	(0x15),
	SETUP_PARAMREQ_COMMAND_TYPE 		(0x16),
	SETUP_PARAMRESPONSE_SENSE_TYPE 		(0x17),
	SETUP_PARAMSET_COMMAND_TYPE 		(0x18),
	SETUP_SENS_LIST_REQ_COMMAND_TYPE	(0x19),
	SETUP_SENS_LIST_REPLY_SENSE_TYPE	(0x1A),
	SETUP_MESSAGE_ACKNOWLEDGE			(0x1F),

	// Alert message codes
	ALERT_DATARANGEWARNING_SENSE_TYPE	(0x21),
	
	// Short data message codes
	SHORT_NATIVEDIGITALREAD_SENSE_TYPE	(0x31),
	SHORT_NATIVEANALOGREAD_SENSE_TYPE	(0x32),
	SHORT_SINGLEDATABYTE_SENSE_TYPE		(0x33),
	SHORT_DOUBLEDATABYTE_SENSE_TYPE		(0x34),
	
	// Long data message codes
	LONG_GENERICDATA_SENSE_TYPE 		(0x41),
	LONG_GENERICDATA_COMMAND_TYPE 		(0x42),
	LONG_INITCODE_SENSE_TYPE 			(0x43),
	LONG_INITCODE_COMMAND_TYPE 			(0x44),
	LONG_DATALOGHEADER_SENSE_TYPE		(0x45),
	LONG_DATALOGPAYLOAD_SENSE_TYPE		(0x46),
	
	// Bad message type
	ERROR_BAD_MESSAGE_TYPE				(0xFF);
	
	// Store the value here
	private final int typeCode; 
	
	// Private constructor
	private A3PMsgType(int typeCode){
		this.typeCode = typeCode;
	}

	// Get the code as a byte
	public byte typeAsByte(){
		return (byte) typeCode;
	}
	
	// Get the code as an int
	public int code(){
		return typeCode;
	}
	
	// Get the message class from its type
	public A3PMsgClass msgClass(){
		return A3PMsgClass.fromCode(typeCode & 0xF0);
	}

	// Construct this type from a byte code
	public static A3PMsgType fromCode(byte message_code){
		return fromCode((int)message_code);
	}
	
	// Construct this type from an int code
	public static A3PMsgType fromCode(int message_code){
		for(A3PMsgType m : A3PMsgType.values()){
			if(m.typeCode == message_code){
				return m;
			}
		}
		return ERROR_BAD_MESSAGE_TYPE;
	}
}