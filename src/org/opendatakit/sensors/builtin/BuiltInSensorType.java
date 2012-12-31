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
package org.opendatakit.sensors.builtin;

import org.opendatakit.sensors.builtin.drivers.AbstractBuiltinDriver;
import org.opendatakit.sensors.builtin.drivers.AccelerometerDriver;
import org.opendatakit.sensors.builtin.drivers.GravityDriver;
import org.opendatakit.sensors.builtin.drivers.GyroscopeDriver;
import org.opendatakit.sensors.builtin.drivers.LightDriver;
import org.opendatakit.sensors.builtin.drivers.LinearAccelerationDriver;
import org.opendatakit.sensors.builtin.drivers.MagneticFieldDriver;
import org.opendatakit.sensors.builtin.drivers.OrientationDriver;
import org.opendatakit.sensors.builtin.drivers.PressureDriver;
import org.opendatakit.sensors.builtin.drivers.ProximityDriver;
import org.opendatakit.sensors.builtin.drivers.RotationVectorDriver;
import org.opendatakit.sensors.builtin.drivers.TemperatureDriver;

import android.hardware.Sensor;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public enum BuiltInSensorType {
	ACCELEROMETER(Sensor.TYPE_ACCELEROMETER, AccelerometerDriver.class),
	GRAVITY(Sensor.TYPE_GRAVITY, GravityDriver.class),
	GYROSCOPE(Sensor.TYPE_GYROSCOPE, GyroscopeDriver.class),
	LIGHT(Sensor.TYPE_LIGHT, LightDriver.class),
	LINEAR_ACCELERATION(Sensor.TYPE_LINEAR_ACCELERATION, LinearAccelerationDriver.class),
	MAGNETIC_FIELD(Sensor.TYPE_MAGNETIC_FIELD, MagneticFieldDriver.class),
	ORIENTATION(Sensor.TYPE_ORIENTATION, OrientationDriver.class),
	PRESSURE(Sensor.TYPE_PRESSURE, PressureDriver.class),
	PROXIMITY(Sensor.TYPE_PROXIMITY, ProximityDriver.class),
	ROTATION_VECTOR(Sensor.TYPE_ROTATION_VECTOR, RotationVectorDriver.class),
	TEMPERATURE(Sensor.TYPE_AMBIENT_TEMPERATURE, TemperatureDriver.class);
	
	private final int type;
	private final Class<? extends AbstractBuiltinDriver> driverClass;
	
	private BuiltInSensorType(int type, Class<? extends AbstractBuiltinDriver> driverClass) {
		this.type = type;
		this.driverClass = driverClass;
	}
	
	public final int getType() {
		return type;
	}
	
	public Class<? extends AbstractBuiltinDriver> getDriverClass() {
		return driverClass;
	}
	
	public static BuiltInSensorType convertToBuiltInSensor(int type) {
		for(BuiltInSensorType sensorType : BuiltInSensorType.values()) {
			if(sensorType.type == type) {
				return sensorType;
			}
		}
		return null;
	}
	

}
