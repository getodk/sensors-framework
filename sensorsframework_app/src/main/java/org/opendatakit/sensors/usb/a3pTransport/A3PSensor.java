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

import java.util.concurrent.ConcurrentLinkedQueue;

import android.os.Bundle;

/**
 * A class to allow the A3P transport layer to track the status of various sensors.
 * 
 * TODO May not be needed, since sensor state is maintained in sensor objects in
 * org.opendatakit.sensors
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class A3PSensor {

	public static final int UNKNOWN_PIN = -1;
	
	//TODO: create static list of sensorNumbers to be sure all are unique
	
	private ConcurrentLinkedQueue<A3PMessage> messageQ;
	private String name;
	private int sensorNumber;
	private int pinNumber;
	
	public A3PSensor(String name, int sensorNumber){
		this(name, sensorNumber, UNKNOWN_PIN);
	}
	
	public A3PSensor(String name, int sensorNumber, int pinNumber){
		this.messageQ = new ConcurrentLinkedQueue<A3PMessage>();
		this.name = name;
		this.sensorNumber = sensorNumber;
		this.pinNumber = pinNumber;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setSensorNumber(int sensorNumber) {
		this.sensorNumber = sensorNumber;
	}

	public int getSensorNumber() {
		return sensorNumber;
	}

	public void setPinNumber(int pinNumber) {
		this.pinNumber = pinNumber;
	}

	public int getPinNumber() {
		return pinNumber;
	}
	
	// Is this thread-safe?
	public void addMessage(A3PMessage newMessage){
		messageQ.add(newMessage);
	}
	
	// Is this thread-safe?
	public A3PMessage getMessage(){
		return messageQ.poll();
	}
	
	public int getNumMessages(){
		return messageQ.size();
	}
	
	public boolean hasMessages(){
		//return messageQ.size() != 0;
		return messageQ.peek() != null;
	}
	
	public Bundle getInfoBundle(){
		Bundle toReturn = new Bundle();

		toReturn.putInt("Sensor Number", sensorNumber);
		
		toReturn.putString("Sensor Name", name);
		
		toReturn.putInt("Current queue size", messageQ.size());
		
		return toReturn;
	}
}
