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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.opendatakit.sensors.usb.USBPayload;

import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * A3PSession represents a conversation between an Android device and an ADK Arduino device
 * using AAAP (A3P).  It receives A3PMessages over the ADK protocol and passes USBPayloads
 * up to USBManagerService.  Designed to last as long as the input and output streams are valid.
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class A3PSession {

	private static final boolean SKIP_ACKS = false;			// Whether to skip the ACKing system
															// This *must* match the SKIP_QUEUE_3SEC and
															// SKIP_SENDING_ACKS_3SEC on the Arduino-side a3pSession.c
	private static final String LOG_TAG = "A3PSession";
	private static final boolean LOG_VERBOSE = true;
	
	private ConcurrentLinkedQueue<A3PMessage> incomingQ;	// Queue for incoming messages over A3P/ADK/USB channel
	private ConcurrentLinkedQueue<A3PMessage> commandQ;		// Queue for outgoing messages over A3P/ADK/USB channel
		
	private A3PInputWorker inputWorker;
	private A3POutputWorker outputWorker;
	private A3PConnectionState myState;
//	private ArduinoSubChannel arduinoChannel;
	
//	private static final int CONFIG_DEVICE_ID = 0;
	private static final int TURNOVER_EDGE_PERCENTAGE = 25;
	private static final long MAX_MESSAGE_NUMBER = 1023;
	
	//command sent to usb bridge when A3P session is being shutdow. 
	private static final int SHUTDOWN_A3P_CMD = 0x5;
	
	private int low_border = ((TURNOVER_EDGE_PERCENTAGE) * (int) MAX_MESSAGE_NUMBER) / 100;
	private int high_border = ((100 - TURNOVER_EDGE_PERCENTAGE) * (int) MAX_MESSAGE_NUMBER) / 100;
	
	private long handshakeTime = -1;
	
	private boolean hasBeenStarted = false;
	
	private int largestRcvdMsgNum  = 0;			// The last packet number we received *and processed*
	private volatile int outMsgNum = 0;	// The next packet number to send, (needs to be thread-safe!)
	
	//private int inMessagesLost = 0; // The number of messages we've lost, based on the message
									// numbers received and processed 
	
	private ConcurrentHashMap<Integer, Integer> receivedMap; // this could be a set now, but a map preserves information for later
	private ConcurrentHashMap<Integer, Integer> missingMap;		// this could potentially be changed to a set
	
//	public A3PSession(ArduinoSubChannel myChannel, ParcelFileDescriptor parcelFileDescriptor)
	public A3PSession(ParcelFileDescriptor parcelFileDescriptor) {
	
	
		this.incomingQ = new ConcurrentLinkedQueue<A3PMessage>();
		this.commandQ = new ConcurrentLinkedQueue<A3PMessage>();
		FileDescriptor fd = parcelFileDescriptor.getFileDescriptor();
		
//		this.arduinoChannel = myChannel;
		
		inputWorker = new A3PInputWorker(this, incomingQ, new FileInputStream(fd));
		outputWorker = new A3POutputWorker(this, commandQ, new FileOutputStream(fd));
		myState = A3PConnectionState.STARTED;
		
		receivedMap = new ConcurrentHashMap<Integer, Integer>();
		missingMap = new ConcurrentHashMap<Integer, Integer>();
	}
	
	/**
	 * Start the read and write threads to initialize the connection.
	 * Check .isConnected to be sure the connection has started.
	 * @throws IllegalStateException if the connection has been previously closed
	 */
	public void startConnection(){
		if(isClosed()){ 
			throw new IllegalStateException("Cannot start threads when connection " +
					"has been closed.  Please create a new A3PSession to reconnect.");
		}				
		
		// Start the worker threads
		inputWorker.start();
		outputWorker.start();
		
		while(!inputWorker.isAlive() || !outputWorker.isAlive()){
    		try{
    			synchronized(this){
					this.wait(100);
    			}
			} catch (InterruptedException e){
				// Do nothing if interrupted.
			}
		}
		
		hasBeenStarted = true;
	}
	
	/**
	 * Close the A3P connection.
	 * Check .isClosed to be sure the connection has closed.
	 * 
	 * Note: once this connection is closed, a new A3PSession must be created
	 *       to reconnect.  (mostly to ease inputstream/outputstream/etc tracking) 
	 */
	public synchronized void endConnection(){
		Log.d(LOG_TAG, "---> Entered endConnection");
		if(isClosed()){
			Log.d(LOG_TAG, "A3PSession already closed!");
			return;
		}		

		inputWorker.stopWorker();
		
		Log.d(LOG_TAG,"sending shutdown a3p command to adk board");
		sendShutdownA3PCmd();
		try{
			//lame. give outputworker time to send out the message
			Thread.sleep(1000);
		}
		catch(InterruptedException iex) {
			Log.d(LOG_TAG,"thread.sleep interrupted in shutdown()");
		}    	
		
		outputWorker.stopWorker();

		Log.v(LOG_TAG, "BEFORE CLOSING DESCRIPTOR");
		
//		arduinoChannel.closeMyAccessory();
		
		Log.v(LOG_TAG, "AFTER CLOSING DESCRIPTOR");	
		
		myState = A3PConnectionState.CLOSED;
		
		Log.d(LOG_TAG, "<--- Exited endConnection");
	}
	
	private void sendShutdownA3PCmd(){
		byte[] payload = new byte[1];
		payload[0] = SHUTDOWN_A3P_CMD;
		long ts = Calendar.getInstance().getTimeInMillis();
		USBPayload usbPayload = new USBPayload(payload, ts, 0, false);
		this.enqueuePayloadToSend(usbPayload);
	}
	
	/**
	 * Indicates whether the device is connected and has completed the A3P handshake.
	 * @return true the device is connected and has completed the A3P handshake, false otherwise
	 */
	public boolean isConnected(){
		updateState();
		return myState == A3PConnectionState.READY;
	}
	
	/**
	 * Indicates whether the device is closed and worker threads have been stopped.
	 * @return true the device is closed and worker threads have been stopped, false otherwise
	 */
	public boolean isClosed(){
		updateState();
		return myState == A3PConnectionState.CLOSED;
	}
	
	/**
	 * Grabs the next received payload from the next A3PMessage in the incoming queue.
	 * @return
	 */
	public USBPayload getNextPayload(){
		A3PMessage theMessage = incomingQ.poll();
		
		if(theMessage == null){
			return null;
		}
		
		if(SKIP_ACKS){
			// If we're skipping the ACKing system, simply return the next
			// available message!
			return theMessage.getUSBPayload();
		}
		
		Integer msgNbr = Integer.valueOf(theMessage.getMessageNumber());
		
		// Is this message number in the missing set?
		if(missingMap.containsKey(msgNbr)){
			// This message was missing, so it's not a duplicate.
			
			// Take the number out of the missing set, since we found it!
			missingMap.remove(msgNbr);
			
			Log.d(LOG_TAG, "Just got message: " + msgNbr);
			/*for(Integer n : missingMap.keySet()){
				Log.d(LOG_TAG, "Missing: " + n);
			}*/
			
			// Place this message in the received map and return the message!
			receivedMap.put(msgNbr, msgNbr);//theMessage);
			
			if(LOG_VERBOSE) Log.d("ACCEPT",msgNbr + "");
			return theMessage.getUSBPayload();
		}
		
		// In low-number region just after wraparound
		if( largestRcvdMsgNum < low_border ){
			Log.d(LOG_TAG,"In border region");
			
			// See code copy below for comments...
			if(msgNbr > largestRcvdMsgNum){
				if(msgNbr > high_border){
					// This is a re-send from the high_border section,
					// so *don't* count it as a moving-forward message.
					Log.d(LOG_TAG, "Saw large-end number " + msgNbr + " while in the low-end border region.");
				} else {
					if(msgNbr > largestRcvdMsgNum + 1){
						for(int i = largestRcvdMsgNum + 1; i < msgNbr; i++){
							missingMap.put(Integer.valueOf(i), Integer.valueOf(i));	}
						Log.d(LOG_TAG, "As of msg " + msgNbr + ", skipped " + (msgNbr - largestRcvdMsgNum - 1) +
							" messages! " + missingMap.size() + " lost so far, but " + receivedMap.size() + " received ok (no repeats).");
					}
					// Only move this number forward!  (Don't move it back!)
					largestRcvdMsgNum = msgNbr;
				
			    	// If we're about to leave the low_border, finish clearing
					// the maps of stuff in the higher border section 
			    	if(msgNbr >= low_border){
	
						//missingMap.clear();
						Log.d(LOG_TAG,"Removing old keys from the missing map");
						for(Integer i : missingMap.keySet()){
							if(i >= high_border){
								missingMap.remove(i);
							}
						}					
						
						//receivedMap.clear();
						Log.d(LOG_TAG,"Removing old keys from the received map");
						for(Integer i : receivedMap.keySet()){
							if(i >= high_border){
								receivedMap.remove(i);
							}
						}
			    	} // else do nothing...
				}
			}
			
		} else {
			// TODO: cleanup my logic here
			// Did we lose messages?
			if(msgNbr > largestRcvdMsgNum){
				
				if(msgNbr > largestRcvdMsgNum + 1){
					// We lost some messages!  Put all skipped messages into the missing map... 
					for(int i = largestRcvdMsgNum + 1; i < msgNbr; i++){
						missingMap.put(Integer.valueOf(i), Integer.valueOf(i));//new Boolean(true));
					}
					
					// Report the lost messages!
					Log.d(LOG_TAG, "As of msg " + msgNbr + ", skipped " + (msgNbr - largestRcvdMsgNum - 1) +
						" messages! " + missingMap.size() + " lost so far, but " + receivedMap.size() + " received ok (no repeats).");
					
					/*for(Integer n : missingMap.keySet()){
						Log.d(LOG_TAG, "Missing: " + n);
					}*/
				}
				
				// Only move this number forward!  (Don't move it back!)
				largestRcvdMsgNum = msgNbr;
				
			} else {
				// This message is equal to or earlier than the last received (non-resend) message
				// *don't* update the largestRcvdMsgNum UNLESS we're wrapping around to the beginning
				// of the numbers.

				
				// Probably, clear both maps and somehow save state for the last bits...
				if(msgNbr < low_border && largestRcvdMsgNum > high_border){
					// Wraparound now!
					
					// We're about to wraparound, so clear everything from the maps
					// except for recent stuff in the high_border section
					
					//missingMap.clear();
					Log.d(LOG_TAG,"Removing old keys from the missing map");
					for(Integer i : missingMap.keySet()){
						
						if(i < high_border){
							missingMap.remove(i);
						}
						
					}					
					
					//receivedMap.clear();
					Log.d(LOG_TAG,"Removing old keys from the received map");
					for(Integer i : receivedMap.keySet()){
						
						if(i < high_border){
							receivedMap.remove(i);
						}
						
					}
					
					largestRcvdMsgNum = msgNbr.intValue();
				}
			}
		}
			
		// Is this message a duplicate?
		if(receivedMap.containsKey(msgNbr)){
			// This message is a duplicate!  Discard it and recursively get the next message.
			// The base case for the recursion is to return null when no me)
			Log.d(LOG_TAG, "Duplicate packet number " + msgNbr + " found, discarding duplicate!  Recursion here...");
			return getNextPayload();
		} else {
			
			// This message is a normal, forward-order message
			// TODO: double check the logic here ^
			receivedMap.put(msgNbr, msgNbr);//theMessage);
			if(LOG_VERBOSE) Log.d("ACCEPT",msgNbr + "");
			return theMessage.getUSBPayload();
		}		
	}
	
	/**
	 *  Enqueue an arbitrary payload to be sent to the ADK device.
	 *  For now, uses the 'Parameter Setting Command' message type.
	 * @param toSend - the payload to send to the ADK device
	 * @throws IllegalStateException if the connection has been previously closed
	 */
	public void enqueuePayloadToSend(USBPayload toSend){
		if(isClosed()){
			throw new IllegalStateException("Cannot send payloads when connection " +
					"has been closed. Please create a new A3PSession to reconnect.");
		}
		A3PMessage messageToSend = new A3PMessage(A3PMsgType.SETUP_PARAMSET_COMMAND_TYPE, outMsgNum++,(byte) toSend.getSensorID(), 
				toSend.getRawBytes());
		sendA3PMessage(messageToSend);
	}
	
	/**
	 * Sends an ACK packet with msgNum corresponding to the packet being acked.
	 * This method is *only* for use by the input worker thread to ack
	 * incoming packets!
	 * 
	 * Currently uses handshake message type.
	 */
	public void ack(int msgNum){
		if(SKIP_ACKS){
			// If we're skipping the ACKing system, do nothing!
			return;
		}
		A3PMessage theACK = new A3PMessage(A3PMsgType.SETUP_MESSAGE_ACKNOWLEDGE, msgNum, (byte) 0, new byte[0]);
		sendA3PMessage(theACK);
	}
	
	/**
	 * This code is factored out so the two causes of packet sending (payload send and ack)
	 * can be thread-safe (especially for the outMsgNum counter).
	 * @param toSend
	 */
	private void sendA3PMessage(A3PMessage toSend){
		synchronized(this){
			commandQ.add(toSend);
		}
	}
	
	/**
	 * Checks and sets the current state of the A3PSession.
	 */
	private void updateState(){
		
		switch(myState){
			case STARTED:
				
				//TODO: how to tell when the connection is ready, except that we have received something?
				
				//if(inputWorker.messagesReceived() > 0){ 
					// Use messagesReceived instead of incomingQ size because incoming
					// queue may be being emptied by the client thread
					//Log.d(LOG_TAG,"***Now ready!");
				//	myState = A3PConnectionState.READY;
				//}
				
				// Double check to be sure worker threads are still alive if they were started earlier
				if(hasBeenStarted && ((!inputWorker.isAlive()) || (!outputWorker.isAlive()) )){
					//Log.d(LOG_TAG,"***Now closed!");
					myState = A3PConnectionState.CLOSED;
				}
				break;
				
			case READY:
				if((!inputWorker.isAlive()) || (!outputWorker.isAlive()) ){
					myState = A3PConnectionState.CLOSED;
				}
				break;
				
			case CLOSED:
				// Connection is already closed, do nothing
				break;
		}
	}
	
	public int getNumMessagesLost() {
		return missingMap.size();
	}

	// A small enumerated type to track session state
	private enum A3PConnectionState {
		STARTED,
		READY,
		CLOSED;
	}

	// Only call this in response to receiving an initial handshake from the board!
	public void confirmHandshake() {
		Log.d(LOG_TAG, "Confirming handshake!");
		if(isClosed()){
			throw new IllegalStateException("Cannot complete handshake when connection is closed!");
		}
		
		if(handshakeTime < 0){
			// This is our first handshake!  Record the timestamp!
			handshakeTime = System.currentTimeMillis();
		} else {
			// This is a repeat handshake or a handshake due to a reset
//			if(System.currentTimeMillis() - handshakeTime > MAX_HANDSHAKE_WINDOW_MS){
//				// This handshake attempt is probably due to a reset
//				// Close this A3PSession now!
//				endConnection();
//				return;
//			}
		}
		
		// It's not a bad handshake, so respond!
		
		myState = A3PConnectionState.READY;
		
		A3PMessage handshake = new A3PMessage(A3PMsgType.SETUP_HANDSHAKE_COMMAND_TYPE, 
				A3PCommon.A3P_HANDSHAKE_MSG_NUM , (byte) 0, new byte[0]);
		
		sendA3PMessage(handshake);
	}


	
	// Returns a new A3PSession reset to initial state but
	// also includes messages that were pending to be read!
	// Uses the same UsbManager and FileDescriptor as was used by 'old'
	// If old is null, returns null
//	public static A3PSession freshCopy(A3PSession old, ParcelFileDescriptor newFileDescriptor){
//		if (old == null){
//			return null;
//		}
//		A3PSession cleanSession = new A3PSession(old.arduinoChannel, newFileDescriptor);
//		cleanSession.incomingQ.addAll(old.incomingQ);
//		return cleanSession;
//	}
	
	// ^
	// |
	// | replaces |
	//            |
	//            v
	
	// Copy the input/output queues of a previous session to this one 
	/*public void copyQueues(A3PSession oldSession) {
		this.incomingQ.addAll(oldSession.incomingQ);
		this.commandQ.addAll(oldSession.commandQ);
		
	}*/
}