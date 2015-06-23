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
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

/**
 * A thread that simply writes to stream any A3PMessage placed in the commandQ
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class A3POutputWorker extends Thread {
	
	private static final String LOG_TAG = "A3POutputWorker";
	private static final int MAX_CONSECUTIVE_IO_EXCEPTIONS = 5;
	
	private ConcurrentLinkedQueue<A3PMessage> commandQ;
	private OutputStream outputStream;
	private boolean runWorker;			// Instruct the run() method to exit its while loop
	private int numConsecutiveIOExceptions;
	private A3PSession mySession;
	private final boolean DEBUG = false;
	
	public A3POutputWorker(A3PSession a3psession, ConcurrentLinkedQueue<A3PMessage> commandQ, OutputStream outputStream){
		super("A3POutputWorker Thread");
		this.commandQ = commandQ;
		this.outputStream = outputStream;
		this.runWorker = true;
		this.mySession = a3psession;
		numConsecutiveIOExceptions = 0;
	}
	
	/**
	 * Indirectly stop the worker thread
	 */
	public void stopWorker(){
		runWorker = false;
		this.interrupt();
	}
	
	/**
	 * Called to stop the thread based on too many IO errors
	 */
	private void fail(){
		Log.e(LOG_TAG, "******\t Output worker dying from too many I/O errors (" +
				numConsecutiveIOExceptions + " errors)\t******");
		stopWorker();
	}
	
	public int getNumberOfOutputExceptions(){
		return numConsecutiveIOExceptions;
	}
	
	@Override
	/**
	 * The main run loop for the thread.  Checks the queue for a message to send,
	 * converts it to a byte array if a message is found, then sends the message. 
	 */
	public void run() {
		Log.d(LOG_TAG, "Entered outputWorker run loop");
		while(runWorker){
			
			// Sleep to yield control out of this loop
			try {
				sleep(10);
			} catch (InterruptedException e1) {
				// do nothing; it's ok if the sleep is interrupted
			}
			
			if(mySession.isConnected()) {
				A3PMessage toSend = commandQ.poll();
				if(toSend != null){
					try {

						if(DEBUG ){
							Log.d(LOG_TAG, "About to send...");
							Log.d(LOG_TAG, toSend.toString());
						}
						sendMessage(toSend);
						if(DEBUG){
							Log.d(LOG_TAG, "Sent message!    " + toSend);
						}

						// Reset consecutive error count!
						numConsecutiveIOExceptions = 0;

					} catch (IOException e){
						numConsecutiveIOExceptions++;

						Log.e(LOG_TAG, "Error sending message " + toSend.toString() + "\nErrors so far: " + numConsecutiveIOExceptions);

						if(numConsecutiveIOExceptions >= MAX_CONSECUTIVE_IO_EXCEPTIONS){
							fail();
						}

						e.printStackTrace();
					}
				}
			}
		}
		// Out of the while loop!
		// We should close the connection
		Log.d(LOG_TAG,"calling A3PSession.endConnection");
		mySession.endConnection();
	}

	// Actually send a message
	private void sendMessage(A3PMessage toSend) throws IOException{
		outputStream.write(toSend.bytesToSend());
//		Log.e(LOG_TAG, "sent");
	}
}
