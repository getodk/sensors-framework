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
 * Defines classes of messages for A3P
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public enum A3PMsgClass {

	// General message classes
	SETUP_MESSAGE_CLASS		(0x10),
	ALERT_MESSAGE_CLASS		(0x20),
	SHORT_MESSAGE_CLASS		(0x30),
	LONG_MESSAGE_CLASS		(0x40),
	ERROR_MESSAGE_CLASS		(0xF0);
	
	// Store the value here
	private final int typeCode; 
	
	// Private constructor
	private A3PMsgClass(int typeCode){
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
	
	// Construct this type from a byte code
	public static A3PMsgClass fromCode(byte message_code){
		return fromCode((int)message_code);
	}
	
	// Construct this class from a code
	public static A3PMsgClass fromCode(int message_code){
		for(A3PMsgClass m : A3PMsgClass.values()){
			if(m.typeCode == message_code){
				return m;
			}
		}
		return ERROR_MESSAGE_CLASS;
	}
}