package de.excellmobility.traveltime;

import java.util.Calendar;

public class ResponseTraveltime
{
	public String sid="";
	public String next_sid = "";
	public boolean reverse = false;
	public double traveltime = 0;
	public int accuracy = 0;
	public int standardDeviation = 0;
	public long timeValidity;
	
	public ResponseTraveltime(String sid, String next_sid, boolean reverse, double traveltime, int accuracy, int standardDeviation, long timeValidity) {
		this.sid = sid;
		this.next_sid = next_sid;
		this.reverse = reverse;
		this.traveltime = traveltime;
		this.accuracy = accuracy;
		this.standardDeviation = standardDeviation;
		this.timeValidity = timeValidity;
	}
	
}