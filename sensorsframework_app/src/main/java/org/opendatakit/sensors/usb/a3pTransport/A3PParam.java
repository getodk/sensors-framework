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
 * An enum to define limited control codes to allow the A3P host and client to
 * configure each other at the A3P level.
 * 
 * TODO May be unused/unnecessary, since most/all
 * configuration occurs via commands sent as USBPayload objects.
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public enum A3PParam {
	// Setup message codes
	PARAMETER_SEND_INTERVAL 			(0x01),

	
	// Bad parameter type
	ERROR_BAD_PARAMETER					(0xFF);
	
	// Store the value here
	private final int typeCode; 
	
	// Private constructor
	private A3PParam(int typeCode){
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
	public static A3PParam fromCode(byte message_code){
		return fromCode((int)message_code);
	}
	
	// Construct this type from an int code
	public static A3PParam fromCode(int message_code){
		for(A3PParam p : A3PParam.values()){
			if(p.typeCode == message_code){
				return p;
			}
		}
		return ERROR_BAD_PARAMETER;
	}
}
