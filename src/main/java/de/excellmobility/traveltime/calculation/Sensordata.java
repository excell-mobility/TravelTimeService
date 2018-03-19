package de.excellmobility.traveltime.calculation;

public class Sensordata {
	
	public String sensorgroup = "";
	public String edge_id = "";
	public double speed = 0;
	public long time;
	public boolean reverse;
	public int length = 0;
	public int tt_fusion = 0;
	public int acc_fusion = 0;
	
	public Integer getTt() {
		//System.out.println(speed);
		int tt = (int)(length/(speed/3.6)); 
		return tt;
	}
	
	public double getAccuracy() {
		double past_time = (System.currentTimeMillis() - time)/1000;
		double accuracy = 0;
		accuracy = ((Config.zeroPercentLimit-(double)past_time)/Config.zeroPercentLimit)*100;
		return accuracy;
	}
	
	public int getPastTimeInSecond() {
		return (int)((System.currentTimeMillis() - time)/1000);
	}

}
