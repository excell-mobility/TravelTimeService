
package de.excellmobility.traveltime.calculation;

import java.util.Date;

public class Config {
	
	/**past observation period for consideration of the measured travel times in seconds*/
	public static int intervalMeasuredTraveltime = 600;
	
	/**after how many seconds the measured value is given with 0% correctness -> from this the accuracy is calculated after e.g. 10 mins*/
	public static int zeroPercentLimit = 1800;
	
	/** accuracy value for hydrograph */
	public static int accuracyHydrograph = 60;
  	  	  	
  	/**interval of calculation*/
  	public static int intervallThreadCalculateTraveltime = 60000;
  	
  	/**timestamps for threads*/
  	public static Date lastActionTimeThreadCalculateTraveltime = new Date();
  	public static Date lastActionTimeWS = new Date();

	public static int weightingFcdTt = 75;
	public static int weightingActualValues = 75;
  	
  	
}
