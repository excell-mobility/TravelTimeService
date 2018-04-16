package de.excellmobility.traveltime.calculation;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;

import de.excellmobility.traveltime.ResponseTraveltime;

public class Calculation {
	
	public Calculation(){
		System.out.println("## Reisezeitberechnung gestartet ##");
						
		//Timer für minuetliche Reisezeitberechnung
		Timer timer_CalculateTraveltime = new Timer();
		
		Thread_Calculation threadCalculateTraveltimeNew = new Thread_Calculation();
		timer_CalculateTraveltime.schedule(threadCalculateTraveltimeNew, 10000, Config.intervallThreadCalculateTraveltime);
		
	}
	
	public ResponseTraveltime getTraveltime(String sid, String next_sid, boolean reverse, long time) {
		boolean actualFcdTraveltime = false;
		boolean actualSensorTraveltime = false;
		double[] tt_accuracy = {0,0};
		int standardDeviation = 0;
		Calendar calRequest = Calendar.getInstance();
		calRequest.setTimeInMillis(time);
		
		int daygroup = Function.getDaygroupWithoutHolidays(calRequest);
		
		//if time max 2 minutes in future, response is like actual time
		if(Calendar.getInstance().getTimeInMillis()-calRequest.getTimeInMillis() > -120000) {
			//actual traveltime available?
			tt_accuracy = Function.getActualTraveltimeFromDB(sid, next_sid);
			
			if(tt_accuracy == null) {
				//actual sensor traveltime available?
				tt_accuracy = Function.getActualSensorTraveltimeFromDB(sid, reverse);
				if(tt_accuracy != null)
					actualSensorTraveltime = true;
			}
			else
				actualFcdTraveltime = true;
		}
		
		//use fcd-hydrograph or sensor-hydrograph
		if(!(actualFcdTraveltime || actualSensorTraveltime)) {
			List<double[]> list = new LinkedList<>();
			int[] fcd_hydrograph = Function.getValueFromTraveltimeHydrographDB(sid, next_sid, daygroup, calRequest);
			if(fcd_hydrograph != null) {
				double[] valuesFcdHydrograph = {0,0,0};
				valuesFcdHydrograph[0] = fcd_hydrograph[0];
				standardDeviation = fcd_hydrograph[1];
				valuesFcdHydrograph[1] = Config.accuracyHydrograph;
				valuesFcdHydrograph[2] = Config.weightingFcdTt;
				
				list.add(valuesFcdHydrograph);
			}
			
			//use sensor-data-hydrograph
			int[] sensor_hydrograph = Function.getValueFromSensorHydrographDB(sid, reverse, daygroup, calRequest);
			if(sensor_hydrograph != null) {
				double[] valuesSensorHydrograph = {0,0,0};
				//calculate traveltime
				int speed = sensor_hydrograph[0];
				int[] length = Function.getLengthOfEdge(sid, reverse);
				double hydTraveltimeSensor = (length[0]/((double)speed/3.6));
				valuesSensorHydrograph[0] = hydTraveltimeSensor;
				valuesSensorHydrograph[1] = Config.accuracyHydrograph;
				valuesSensorHydrograph[2] = 100 - Config.weightingFcdTt;
				list.add(valuesSensorHydrograph);
			}
			
			//fusion values of hydrograph - fcd and sensor
			tt_accuracy = (Function.fusionTt(list));
		}
		tt_accuracy[0] = tt_accuracy[0]*10;
		tt_accuracy[0] = Math.round(tt_accuracy[0]);
		tt_accuracy[0] = tt_accuracy[0]/10;
		
		return new ResponseTraveltime(sid, next_sid, reverse, tt_accuracy[0], (int)tt_accuracy[1], standardDeviation, time);
	}
			
	public List<ResponseTraveltime> getActualTraveltimeDresden() {
		List<ResponseTraveltime> list = Function.getActualTraveltimeDresdenFromDB();
		return list;
	}
	
	public List<ResponseTraveltime> getTraveltimeDresden(long time) {
		List<ResponseTraveltime> list = new LinkedList<>(); 
		Calendar calRequest = Calendar.getInstance();
		calRequest.setTimeInMillis(time);
		
		int daygroup = Function.getDaygroupWithoutHolidays(calRequest);
		
		//if time max 2 minutes in future, response is like actual time
		if(Calendar.getInstance().getTimeInMillis()-calRequest.getTimeInMillis() > -120000) {
			list = getActualTraveltimeDresden();
		}
		else {
			//get traveltime from fcd-hydrograph
			HashMap<String, HashMap<String, Hydrograph>> hm_tt_hydrograph = Function.importTraveltimeHydrographDresden(daygroup);
			Iterator<Entry<String, HashMap<String, Hydrograph>>> it_hyd = hm_tt_hydrograph.entrySet().iterator();
			while(it_hyd.hasNext()){
				Entry<String, HashMap<String, Hydrograph>> entry_hyd = it_hyd.next();
				HashMap<String, Hydrograph> hm_hyd = entry_hyd.getValue();
				Iterator<Entry<String, Hydrograph>> it_hyd_nextEdgeId = hm_hyd.entrySet().iterator();
				while(it_hyd_nextEdgeId.hasNext()) {
					Entry<String, Hydrograph> entry_nextEdgeId = it_hyd_nextEdgeId.next();
					Hydrograph hydrograph = entry_nextEdgeId.getValue();
					//[traveltime, standard deviation]
					int[] fcd_hydrograph = Function.getValueFromTraveltimeHydrograph(hydrograph, calRequest);
					if(fcd_hydrograph != null) {
						ResponseTraveltime rt = new ResponseTraveltime(hydrograph.edge_id, hydrograph.next_edge_id, hydrograph.reverse, fcd_hydrograph[0], Config.accuracyHydrograph, fcd_hydrograph[1], time);
						list.add(rt);
						//sqlStatements.add("('"+hydrograph.edge_id+"','"+hydrograph.reverse+"', '"+fcd_hydrograph[0]+"','"+Config.accuracyHydrograph+"','"+hydrograph.next_edge_id+"','"+actualTime+"'),");
					}
				}
			}
			
			//get traveltime from sensor-data-hydrograph			
			List<Hydrograph> list_sensor_hydrograph = Function.importSensorHydrographDresden(daygroup);
			for(Hydrograph hydrograph : list_sensor_hydrograph) {
				int[] speed_sd = Function.getValueFromSensorHydrograph(hydrograph, calRequest);
				if(speed_sd != null) {
					int speed = speed_sd[0];
					int hydTraveltimeSensor = (int)(hydrograph.road_length/((double)speed/3.6));
					ResponseTraveltime rt = new ResponseTraveltime(hydrograph.edge_id, hydrograph.next_edge_id, hydrograph.reverse, hydTraveltimeSensor, Config.accuracyHydrograph, 0, time);
					list.add(rt);
				}
			}
		}
		return list;
	}
	

}