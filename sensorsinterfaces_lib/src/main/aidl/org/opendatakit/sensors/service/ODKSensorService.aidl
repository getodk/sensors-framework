package org.opendatakit.sensors.service;

interface ODKSensorService {
  
   	/**
   	 * Connect to a Sensor
   	 * @param id sensor id 
   	 */
   	void sensorConnect( in String id, String appForDatabase);
    
   	boolean startSensor(in String id);
   	
   	boolean stopSensor(in String id);
   	
   	void configure(in String id, in String setting, in Bundle params);
    
    /**
     * Get a set of data 
     * @param id 			 	sensor id
     * @param maxNumReadings 	maxNumReadings to return
     
     * @return		Bundle of data readings
     */
	List<Bundle> getSensorData(in String id, long maxNumReadings);
	
	void sendDataToSensor(in String id, in Bundle data);

	boolean isConnected(in String id);
	
	boolean isBusy(in String id);
	
	boolean hasSensor(in String id);
	
	void removeAllSensors();
	
	boolean hasSensorReadingUi(in String id);
	
	boolean hasSensorConfigUi(in String id);
	
	String getSensorReadingUiIntentStr(in String id);
	
	String getSensorConfigUiIntentStr(in String id);
	
}