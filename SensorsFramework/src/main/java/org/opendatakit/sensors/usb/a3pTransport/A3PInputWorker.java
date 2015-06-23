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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendatakit.sensors.usb.USBCommon;

import android.util.Log;

/**
 * A thread that reads from stream any A3PMessage received and places it
 * into the incomingQ.  Implements the state machine for incoming A3P messages, including
 * CRC error checking and (soon) handshaking. (Note: be sure to store connection state in the
 * A3PSession itself for good design).
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class A3PInputWorker extends Thread {
	
	private static final String LOG_TAG = "A3PInputWorker";
	private static final boolean DEBUG = false;
	private static final int MAX_CONSECUTIVE_IO_EXCEPTIONS = 5;
	private static final int REPORTING_INTERVAL = 50;
	private static final int MAX_PAYLOAD_SIZE = 2048;
	
	private static final int MAX_MESSAGE_SIZE = 1024;//512; // Max message size to expect from the Arduino
	
	private A3PSession mySession;
	
	private ConcurrentLinkedQueue<A3PMessage> incomingQ;
	private InputStream inputStream;
	private boolean runWorker;			// Instruct the run() method whether to exit its while loop
	private int numConsecutiveIOExceptions;
	private A3PInputState currentState;
	private long numMessagesReceived;
	
	private int currentArrayPointer = 0;	// Index into each 512-byte message
	private byte[] currentMessage = new byte[MAX_MESSAGE_SIZE];
	
	// These are class-wide so receiving a packet can log the payload position.
	// If necessary, move these back into the run() method.
	private int payloadLength = 0;
	private int payloadReceived = 0;
	
	public A3PInputWorker(A3PSession parentSession, ConcurrentLinkedQueue<A3PMessage> incomingQ,
			InputStream inputStream){
		super("A3PInputWorker Thread");
		this.mySession = parentSession;
		this.incomingQ = incomingQ;
		this.inputStream = inputStream;
		this.runWorker = true;
		numConsecutiveIOExceptions = 0;
		numMessagesReceived = 0;
		currentState = A3PInputState.PRE1;				
	}
	
	/**
	 * Indirectly stop the worker thread and close IO stream
	 */
	public void stopWorker(){
		runWorker = false;
		this.interrupt();		
	}
	
	/**
	 * Called to stop the thread based on too many IO errors
	 */
	private void fail(){
		Log.e(LOG_TAG, "******\t Input worker dying from too many I/O errors (" +
				numConsecutiveIOExceptions + " errors)\t******");
		stopWorker();
	}
	
	/**
	 * The number of messages received so far.
	 * @return
	 */
	public long messagesReceived(){
		return numMessagesReceived;
	}
	
	@Override
	/**
	 * The main run loop for this thread.  Loops by retrieving the next byte, checking it,
	 * and updating the state machine.
	 */
	public void run() { 
		if(DEBUG){
			Log.d(LOG_TAG, "Entered inputWorker run loop");
		}
		// Preallocate enough byte arrays and variables to temporarily hold
		// message information
		byte nextByte,sensorID=0;
		byte[] messageType = new byte[2];
		byte[] messageNumber = new byte[2];
		byte[] payloadLengthBytes = new byte[2];
		byte[] payload = null;
		
		while(runWorker){
			
			// Try to get one byte
			try{
				
				if (DEBUG) Log.d(LOG_TAG, "in...");
				nextByte = nextByte();
				
				if (DEBUG) Log.d(LOG_TAG, "out!");
				
				// Reset consecutive error count!
				numConsecutiveIOExceptions = 0;
			} catch (IOException e){
				
				// Log the exception and reset the receiving state
				numConsecutiveIOExceptions++;
				Log.e(LOG_TAG, "Error receiving byte!  Error during state: " + currentState.toString() + " errors so far: " + numConsecutiveIOExceptions);
				
				if(numConsecutiveIOExceptions >= MAX_CONSECUTIVE_IO_EXCEPTIONS){
					fail();
				}
				
				currentState = A3PInputState.PRE1;
				
				e.printStackTrace();
				
//				try{
//					synchronized(this){
//						this.wait(200);
//					}
//				} catch (InterruptedException e2){
//					// Do nothing if interrupted.
//				}
				
				continue;  // We must use continue or nextByte may be read uninitialized
			}
			
			// Process the byte to continue receiving the message
			switch(currentState){
				case PRE1:
					
					// Expecting byte one of the preamble
					if(nextByte == A3PCommon.PREAMBLE_HI){
						currentState = A3PInputState.PRE2;
					} else {
						if(DEBUG) Log.e(LOG_TAG, "bad byte!");
					}
					
					break;
				case PRE2:
					
					// Expecting byte two of the preamble
					// If we get the first byte again, remain in this state
					// If we get anything else, return to the first preamble state
					if(nextByte == A3PCommon.PREAMBLE_LOW){
						currentState = A3PInputState.TYP1;
					} else if (nextByte == A3PCommon.PREAMBLE_HI) {
						if(DEBUG) Log.e(LOG_TAG, "Repeated preamble byte... ignoring");
						currentState = A3PInputState.PRE2;
					} else {
						if(DEBUG) Log.e(LOG_TAG, "bad byte!");
						// If bad code, start over!
						currentState = A3PInputState.PRE1;
					}
					
					break;
				case TYP1:
					
					// Expecting byte one of the message type
					messageType[0] = nextByte;
					
					currentState = A3PInputState.TYP2;
					break;
				case TYP2:
					
					// Expecting byte two of the message type
					messageType[1] = nextByte;
					
					currentState = A3PInputState.NUM1;
					
					//Reset state to PRE1 if we get a bad message type
					int tempTypeCode = USBCommon.getIntFromTwoBytesLittleEndian(messageType);
					A3PMsgType tempMessageType = A3PMsgType.fromCode(tempTypeCode);
					if(tempMessageType == A3PMsgType.ERROR_BAD_MESSAGE_TYPE){
						Log.e(LOG_TAG,"Bad message type received.  Code: " + tempTypeCode + " dropping packet...");
						currentState = A3PInputState.PRE1;
					} else {
						if(DEBUG) Log.d(LOG_TAG,"Receiving type: " + tempMessageType);
					}
					
					break;
				case NUM1:
					
					messageNumber[0] = nextByte;
					currentState = A3PInputState.NUM2;
					break;
				case NUM2:
					
					messageNumber[1] = nextByte;
					currentState = A3PInputState.SID;
					break;
				case SID:
					
					sensorID = nextByte;
					currentState = A3PInputState.LEN1;
					break;
				case LEN1:
					
					// Expecting byte one of the payload length
					payloadLengthBytes[0] = nextByte;
					currentState = A3PInputState.LEN2;
					break;
				case LEN2:
					
					// Expecting byte two of the payload length
					payloadLengthBytes[1] = nextByte;
					
					// Arduino sends data little-endian
					payloadLength = USBCommon.getIntFromTwoBytesLittleEndian(payloadLengthBytes);
					
					if(payloadLength == 0){
						// Skip the payload stage
						payload = new byte[0];
						currentState = A3PInputState.CRC;
					} else {
						
						if(Math.abs(payloadLength) < MAX_PAYLOAD_SIZE){
							// Get ready to get the payload
							payload = new byte[payloadLength];
							currentState = A3PInputState.PAYX;
						} else {
							Log.e(LOG_TAG, "Bad payload size (" + payloadLength + ")! Dropping packet!");
							currentState = A3PInputState.PRE1;
						}
					}
					
					break;
				case PAYX:
					
					// Expecting payload bytes
					payload[payloadReceived] = nextByte;
					// Mark this byte received
					payloadReceived++;
					
					if(payloadLength == payloadReceived){
						// Next byte will be CRC
						currentState = A3PInputState.CRC;
					}
					break;
					
				case CRC:
					// Expecting one byte of CRC.
					// If the CRC passes, save the received message
					A3PMessage toAdd = validateAndAssemble(messageType, messageNumber, sensorID, payloadLengthBytes, payload, nextByte);
					
					boolean sendACK 	= true;
					boolean keepPacket	= true;
					boolean countPacket = true;
					
					if(toAdd != null){
						
						if(DEBUG){
							Log.d(LOG_TAG, "Got a packet of type: " + toAdd.getMessageType().name());
						}
						
						switch(toAdd.getMessageType()){
							case SETUP_MESSAGE_ACKNOWLEDGE:
								sendACK = false;
								keepPacket = false;
								countPacket = false;
								break;
							case SETUP_HANDSHAKE_SENSE_TYPE :
								keepPacket = false;
								countPacket = false;
								sendACK = false;
								mySession.confirmHandshake();								
								break;
							default:
								break;
						}
						
						if(sendACK){
							if(DEBUG){
								Log.d(LOG_TAG, "ACKing the packet...");
							}
							// Call on the A3PSession to ack the packet we just received
							mySession.ack(toAdd.getMessageNumber());
						}
						
						if(keepPacket){
							incomingQ.add(toAdd);
						}
						
						// Mark a local tally for messages received
						if(countPacket){
							numMessagesReceived++;
							if(DEBUG){
								Log.d(LOG_TAG, "TOTAL MSGS RECEIVED: " + numMessagesReceived);
							} else if (numMessagesReceived % REPORTING_INTERVAL == 1){
								Log.d(LOG_TAG, "TOTAL MSGS RECEIVED: " + numMessagesReceived);
							}
						}
					} else {
							Log.e(LOG_TAG,"Lost a packet!");
					}
					
					// Reset to get the next message, even if this message failed.
					currentState = A3PInputState.PRE1;
					payload = null;
					payloadReceived = 0;
					
					break;
			}
		}
		// Out of the while loop!
		// We should close the connection
		Log.d(LOG_TAG,"calling A3PSession.endConnection");
		mySession.endConnection();
	}
	
	//	private int currentArrayPointer = 0;	// Index into each 512-byte message
	//private byte[] currentMessage;
	
	// Attempt to get the next byte from the queue
	private byte nextByte() throws IOException {
		
		int bytesRead = 0;
		
		if(currentMessage == null || currentArrayPointer >= MAX_MESSAGE_SIZE){	//Need to get a new byte buffer (message)
			//currentMessage = new byte[EXACT_PAYLOAD_SIZE];
			
			// Clear the buffer!
			for(int i = 0; i < MAX_MESSAGE_SIZE; i++){
				currentMessage[i] = 0;
			}
			
			bytesRead = inputStream.read(currentMessage,0,MAX_MESSAGE_SIZE);
			if(DEBUG){
				Log.d(LOG_TAG,"Got a message of " + bytesRead + " bytes!");
			}
			currentArrayPointer = 0;
		}
	
		int nextByteInt = (int) currentMessage[currentArrayPointer];
	
		currentArrayPointer++;
		
		if(bytesRead == -1){
			// End of stream or some other error.  Break now!
			throw new IOException("Reached end of buffer!");
		}
		
		if(DEBUG){
			if(currentState == A3PInputState.PAYX){
				Log.d(LOG_TAG,"Got byte: " + Integer.toHexString(nextByteInt) + " currently getting payload bytes.  Received: " + (payloadReceived + 1) 
						+ " of " + payloadLength + " bytes.");
			} else {
				Log.d(LOG_TAG,"Got byte: " + Integer.toHexString(nextByteInt) + " currently in state- " + currentState);
			}
		}
	
		// Get the byte out of this int
		return USBCommon.getTwoBytesFromInt(nextByteInt)[1];
		
	}
	
	// An enumerated type to track what part of a packet we are expecting
	private enum A3PInputState{
		PRE1("Preamble byte 1"), PRE2("Preamble byte 2"), TYP1("Message Type byte 1"), TYP2("Message Type byte 2"),
		NUM1("Message Number byte 1"), NUM2("Message Number byte 2"), SID("SensorID byte"),LEN1("Payload Length byte 1"),
		LEN2("Payload Length byte 2"), PAYX("Payload byte"), CRC("CRC byte");
		
		private String nextExpected;
		
		private A3PInputState(String nextExpected){
			this.nextExpected = nextExpected;
		}
		
		public String toString(){
			return "Expecting " + nextExpected;
		}
	}

	// Assemble the received bytes into an A3PMessage object
	// Return null if the CRC doesn't checkout
	private A3PMessage validateAndAssemble(byte[] messageTypeBytes, byte[] messageNumberBytes, byte sensorID,
			byte[] payloadLengthBytes, byte[] payload, byte crc){
		
		byte crcValue = A3PCommon.calculateCRC(messageTypeBytes, messageNumberBytes, sensorID, payloadLengthBytes, payload);
		
		if(DEBUG){
			Log.d(LOG_TAG, "Pre-received message# " + USBCommon.getIntFromTwoBytesLittleEndian(messageNumberBytes) +
					" \tcrc-calc: 0x" + USBCommon.byte2Hex(crcValue) + " \tcrc-rcvd: 0x" + USBCommon.byte2Hex(crc));
		}
				
		
		if(crc != crcValue){
			Log.e(LOG_TAG, "CRC Failed! crc calc: " + USBCommon.byteToIntUnsigned(crcValue) +
					" crc received: " + USBCommon.byteToIntUnsigned(crc));
			return null;
		}
		
		if(DEBUG){
			Log.d(LOG_TAG, "CRC Passed! \t CRC: 0x" + USBCommon.byte2Hex(crc));
		}
		
		// Compose and return the message!
		
		// For now, only using the lower byte for message type
		A3PMsgType messageType = A3PMsgType.fromCode(messageTypeBytes[0]);
		
		// Arduino sends data little-endian
		int messageNumber = USBCommon.getIntFromTwoBytesLittleEndian(messageNumberBytes);
		
		// Record this last-found message number
		
		A3PMessage toReturn = new A3PMessage(messageType, messageNumber, sensorID, payload);
		
		return toReturn;
	}

	public int getNumIOExceptions() {
		return numConsecutiveIOExceptions;
	}
}