package org.opencv.engine;

/**
* Class provides Java interface to OpenCV Engine Service. Is synchronous with native OpenCVEngine class.
*/

interface OpenCVEngineInterface
{
	/**
	* @return Return service version
	*/
	int getEngineVersion();

	/**
	* Find installed OpenCV library
	* @param OpenCV version
	* @return Return path to OpenCV native libs or empty string if OpenCV was not found
	*/
	String getLibPathByVersion(String version);

	/**
	* Try to install defined version of OpenCV from Google Play (Android Market).
	* @param OpenCV version
	* @return Return true if installation was successful or OpenCV package has been already installed
	*/
	boolean installVersion(String version);
 
	/**
	* Return list of libraries in loading order separated by ";" symbol
	* @param OpenCV version
	* @return Return OpenCV libraries names separated by symbol ";" in loading order
	*/
	String getLibraryList(String version);
}