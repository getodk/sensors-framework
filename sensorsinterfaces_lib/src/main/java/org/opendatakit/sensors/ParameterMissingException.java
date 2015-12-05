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
package org.opendatakit.sensors;

/**
 * 
 * @author wbrunette@gmail.com
 * @author rohitchaudhri@gmail.com
 * 
 */
public class ParameterMissingException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2420170487471243813L;

	  /**
	   * Default constructor
	   */
	  public ParameterMissingException() {
	    super();
	  }
	  
	  /**
	   * Construct exception with the error message
	   * 
	   * @param message
	   *    exception message
	   */
	  public ParameterMissingException(String message) {
	    super(message);
	  }

	  /**
	   * Construction exception with error message and throwable cause
	   * 
	   * @param message
	   *    exception message
	   * @param cause
	   *    throwable cause
	   */
	  public ParameterMissingException(String message, Throwable cause) {
	    super(message, cause);
	  }

	  /**
	   * Construction exception with throwable cause
	   * 
	   * @param cause
	   *    throwable cause
	   */
	  public ParameterMissingException(Throwable cause) {
	    super(cause);
	  }

	
}
