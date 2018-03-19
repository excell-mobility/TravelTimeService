package de.excellmobility.traveltime.calculation;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class Trafficstream {
	//<past time in seconds, traveltime in seconds>
	public TreeMap<Integer, Integer> hm_traveltime = new TreeMap<Integer, Integer>();
	public String edge_id = "";
	public String next_edge_id = "";
	public double length = 0;
	
	public int tt = 0;
	public int accuracy = 0;
	
	public boolean reverse;
	
	public Trafficstream(){
		
	}
	
	//TODO: Kann weg
	public void calculateTraveltime(HashMap<String, HashMap<Boolean, Sensordata>> hm_sensor_tt, int daygroup){
		if(hm_sensor_tt.containsKey(edge_id)) {
			if(hm_sensor_tt.get(edge_id).containsKey(reverse)) {
				Sensordata sensordata = hm_sensor_tt.get(edge_id).get(reverse);
				hm_traveltime.put(sensordata.getPastTimeInSecond(), sensordata.tt_fusion);
			}
		}
				
		
		Iterator<Entry<Integer, Integer>> it = hm_traveltime.entrySet().iterator();
		int sumPastTime = 0;
		double sumAccuracy = 0;
		while(it.hasNext()){
			Entry<Integer, Integer> entry = it.next();
			sumPastTime += Config.intervalMeasuredTraveltime-entry.getKey();
			sumAccuracy += (int)Math.round((((Config.zeroPercentLimit-(double)entry.getKey())/Config.zeroPercentLimit)*100));
		}
		
		tt = 0;
		accuracy = 0;
		Iterator<Entry<Integer, Integer>> it_traveltime = hm_traveltime.entrySet().iterator();
		while(it_traveltime.hasNext()){
			Entry<Integer, Integer> entry = it_traveltime.next();
			tt += ((Config.intervalMeasuredTraveltime-(double)entry.getKey())/sumPastTime)*entry.getValue();
			int accuracy_tmp = (int)Math.round((((Config.zeroPercentLimit-(double)entry.getKey())/Config.zeroPercentLimit)*100));
			accuracy += (int)Math.round( accuracy_tmp * (accuracy_tmp/sumAccuracy));
		}
		
		
		//fusion with values from hydrograph e.g. 80% from measurement-value -> 20% from hydrograph
		{
			Calendar calRequest = Calendar.getInstance();
			
			int[] fcd_hydrograph = Function.getValueFromTraveltimeHydrographDB(edge_id, next_edge_id, daygroup, calRequest);
			if(fcd_hydrograph != null) {
				double tt_fusion = (tt*((double)accuracy/100)) + (fcd_hydrograph[0]*(1-((double)accuracy)));
				tt = (int)Math.round(tt_fusion);
			}
		}
	}
}
