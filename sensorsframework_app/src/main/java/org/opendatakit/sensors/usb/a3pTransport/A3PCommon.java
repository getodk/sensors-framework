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
 * Holds constants and common methods needed by the A3P transport layer
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class A3PCommon {
	protected static final byte PREAMBLE_HI = (byte)		0xAC; 		// binary 1010 1100 
	protected static final byte PREAMBLE_LOW = (byte)		0xBD;		// binary 1011 1101
	protected static final int SIZE_OF_NO_PAYLOAD_MESSAGE = 10;			// Preamble(2),MsgType(2),MsgNumber(2),SensorID(1),PayloadLength(2),CRC(1)  
	protected static final int PACKET_BUFFER_MAX_SIZE = 	16384;
	protected static final float A3P_VERSION = (float)		1.0;
	protected static final int A3P_HANDSHAKE_MSG_NUM = 		0;
	
	/**
	 * Computes the checksum for an A3P message by simply XORing all bytes together into one byte of CRC.
	 * Behavior is not specified if payload.length != the unsigned integer value of payloadLengthBytes
	 * @param messageTypeBytes a 2-byte array
	 * @param messageNumberBytes a 2-byte array
	 * @param sensorID a byte to indicate the sensor id number
	 * @param payloadLengthBytes a 2-byte array
	 * @param payload a variable-length byte array, length specified by payloadLengthBytes
	 * @return
	 */
	protected static byte calculateCRC(byte[] messageTypeBytes, byte[] messageNumberBytes,
			byte sensorID, byte[] payloadLengthBytes, byte[] payload){
		
		byte crcValue = 0x0;
		
//		Log.e("CRC_CALC", "\t\tCRC-0: 0x" + USBCommon.byte2Hex(crcValue));
				
		// i = 1, 0
		for(int i = 1; i > -1; i--) {
			
			crcValue = (byte) (crcValue ^ messageTypeBytes[i]);
//			Log.d("CRC_CALC","\t\t\ttoXOR_A1: 0x" + USBCommon.byte2Hex(messageTypeBytes[i]) +   "\tCRC-A1: 0x" + USBCommon.byte2Hex(crcValue));
						
			crcValue = (byte) (crcValue ^ messageNumberBytes[i]);
//			Log.d("CRC_CALC","\t\t\ttoXOR_A2: 0x" + USBCommon.byte2Hex(messageNumberBytes[i]) + "\tCRC-A2: 0x" + USBCommon.byte2Hex(crcValue));
			
			crcValue = (byte) (crcValue ^ payloadLengthBytes[i]);
//			Log.d("CRC_CALC","\t\t\ttoXOR_A3: 0x" + USBCommon.byte2Hex(payloadLengthBytes[i]) + "\tCRC-A3: 0x" + USBCommon.byte2Hex(crcValue));
			
//			Log.e("CRC_CALC", "\tCRC-A: 0x" + USBCommon.byte2Hex(crcValue));
		}
		
		crcValue = (byte) (crcValue ^ sensorID);
//		Log.d("CRC_CALC","\t\t\ttoXOR_B: 0x" + USBCommon.byte2Hex(sensorID) + "\tCRC-B: 0x" + USBCommon.byte2Hex(crcValue));
		
//		Log.e("CRC_CALC", "\tCRC-B: 0x" + USBCommon.byte2Hex(crcValue));
		
		if(payload != null){
			for(byte b : payload){
				crcValue = (byte) (crcValue ^ b);
//				Log.d("CRC_CALC","\t\t\ttoXOR_C: 0x" + USBCommon.byte2Hex(b) + "\tCRC-C: 0x" + USBCommon.byte2Hex(crcValue));
			}
			
//			Log.e("CRC_CALC", "\tCRC-C: 0x" + USBCommon.byte2Hex(crcValue));
		}
		
		return crcValue;
	}
	

	
//	/**
//	 * Computes the checksum for an A3P message by simply XORing all bytes together into one byte of CRC
//	 * @param messageTypeBytes
//	 * @param messageNumberBytes
//	 * @param sensorID
//	 * @param payloadLengthBytes
//	 * @param payload
//	 * @return
//	 */
//	protected static byte calculateCRC(byte[] messageTypeBytes, byte[] messageNumberBytes,
//			byte sensorID, byte[] payloadLengthBytes, byte[] payload){
//		byte crcValue = 0x0;
//		
//		crcValue = USBCommon.xorWithAll(crcValue, messageTypeBytes);
//		crcValue = USBCommon.xorWithAll(crcValue, messageNumberBytes);
//		crcValue = USBCommon.xorBytes(crcValue, sensorID);
//		crcValue = USBCommon.xorWithAll(crcValue, payloadLengthBytes);
//		
//		if(payload != null){
//			crcValue = USBCommon.xorWithAll(crcValue, payload);
//		}
//		
//		return crcValue;
//	}
}
