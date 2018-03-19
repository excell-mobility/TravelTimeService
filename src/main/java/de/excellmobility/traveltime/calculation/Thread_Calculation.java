package de.excellmobility.traveltime.calculation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimerTask;

public class Thread_Calculation extends TimerTask{
	
	private SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
	private int actualDaygroup = 99;
	private HashMap<String, HashMap<String, Hydrograph>> hm_tt_hydrograph = new HashMap<>();
	
	public void run() {
		System.out.println("**************************************************************");
		long time = System.currentTimeMillis();
		long timeAll = System.currentTimeMillis();
		
		//TODO: use holiday from each federal state
		int daygroup = Function.getDaygroupWithoutHolidays(Calendar.getInstance());
		if(actualDaygroup != daygroup) {
			System.out.println("importiere TGL (Tagesgruppe: "+daygroup+")" );
			//importhydrographs
			hm_tt_hydrograph.clear();
			hm_tt_hydrograph = Function.importTraveltimeHydrographAll(daygroup);
			actualDaygroup = daygroup;
		}
		
		//FCD
		HashMap<String, HashMap<Boolean, List<Trafficstream>>> hm_fcd_tt = importFCD();
		System.out.println("finish importFcd ("+(System.currentTimeMillis()-time)+" ms)");
		time = System.currentTimeMillis();
						
		//Sensordaten
		HashMap<String, HashMap<Boolean, Sensordata>> hm_sensor_tt  = importSensorData();
		System.out.println("finish importSensorData ("+(System.currentTimeMillis()-time)+" ms)");
		time = System.currentTimeMillis();
						
		//calculate traveltime (with fusion)
		calculateTraveltime(hm_fcd_tt, hm_sensor_tt, daygroup);
		System.out.println("finish calculateTraveltime ("+(System.currentTimeMillis()-time)+" ms)");
		time = System.currentTimeMillis();
		
		//export into database
		export2DB(hm_fcd_tt, hm_sensor_tt);
		System.out.println("finish export2DB ("+(System.currentTimeMillis()-time)+" ms)");
		time = System.currentTimeMillis();
		
		System.out.println("finish calculate new traveltime "+sdf.format(new Date())+" ("+(System.currentTimeMillis()-timeAll)+" ms)");
		
	}

	
	private HashMap<String, HashMap<Boolean, List<Trafficstream>>> importFCD() {
		//     edge_id          reverse     
		HashMap<String, HashMap<Boolean, List<Trafficstream>>> hm_fcdtt = new HashMap<String, HashMap<Boolean,List<Trafficstream>>>();
		//import Daten 
		//TODO: wenn Tabelle partitioniert kann limit 20000 wieder raus
		//String query = "select id, sid, did, next_sid, next_did, entertime, exittime, length from "+DbConnector.table_raw_edge_records+" where sid != '' and next_sid != '' order by exittime desc limit 2000";
//		String query = "select id, sid, did, next_sid, next_did, entertime, exittime, length from "+DbConnector.table_raw_edge_records+" where sid != '' and next_sid != '' "
//				+ "and (extract(epoch from now())-exittime/1000) < "+Config.betrachtungszeitraum+" order by exittime desc";
		
		String query = "select id, sid, reverse, next_sid, entertime, exittime, length from (select * from "+DbConnector.table_edge_records+" order by exittime desc limit 20000) as t1 where sid != '' and next_sid != '' "
		+ "and entertime > 0 and exittime >= entertime and (extract(epoch from now())-exittime/1000) < "+Config.intervalMeasuredTraveltime+" order by exittime desc";
		
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			//Calendar date_utc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
			Calendar dateNow = Calendar.getInstance();
			Calendar dateFCD = Calendar.getInstance();
			while (rs.next()) {
				String edge_id = rs.getString("sid");
				String next_edge_id = rs.getString("next_sid");
				boolean reverse = rs.getBoolean("reverse");
				long entertime = rs.getLong("entertime");
				long exittime = rs.getLong("exittime");
				dateFCD.setTimeInMillis(exittime);
				//System.out.println(sdf.format(dateFCD.getTime())+" "+sid+" "+next_sid+" Reisezeit: "+(exittime-entertime)+" ms ("+(exittime-entertime)/1000+" s) Laenge: "+rs.getDouble("length"));
				int time = (int) (((dateNow.getTimeInMillis()-dateFCD.getTimeInMillis())/1000));
				//in seconds
				int tt = Math.round((exittime-entertime)/1000);
				
				Trafficstream trafficstream = new Trafficstream();
				trafficstream.edge_id = edge_id;
				trafficstream.next_edge_id = next_edge_id;
				trafficstream.length = rs.getDouble("length");
				trafficstream.hm_traveltime.put(time, tt);
				trafficstream.reverse = reverse;
				
				
				if(hm_fcdtt.containsKey(edge_id)){
					HashMap<Boolean, List<Trafficstream>> hm = hm_fcdtt.get(edge_id);
					if(hm.containsKey(reverse)) {
						List<Trafficstream> list = hm.get(reverse);
						boolean exist = false;
						for(Trafficstream tstream : list) {
							if(tstream.next_edge_id.equals(next_edge_id)) {
								tstream.hm_traveltime.put(time, tt);
								exist = true;
							}
						}
						if(!exist) {
							list.add(trafficstream);
						}
					}
					else { 
						List<Trafficstream> list = new LinkedList<>();
						list.add(trafficstream);
						hm.put(reverse, list);
					}
				}
				else{
					List<Trafficstream> list = new LinkedList<>();
					list.add(trafficstream);
					HashMap<Boolean, List<Trafficstream>> hm = new HashMap<>();
					hm.put(rs.getBoolean("reverse"), list);
					hm_fcdtt.put(edge_id, hm);
				}
												
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		finally{
			try {
				if (rs != null)
					rs.close();
				rs = null;
			} 
			catch (Exception ex) {ex.printStackTrace();/* nothing to do */}
			try {
				if (st != null)	
					st.close();
				st = null;} 
			catch (Exception ex) {/* nothing to do */}
			try {
				if (conn != null) 
					conn.close();
				conn = null;} 
			catch (Exception ex) {/* nothing to do */}
		}
		return hm_fcdtt;
	}
	
	private HashMap<String, HashMap<Boolean, Sensordata>> importSensorData() {
		//     edge_id          reverse  sensor(group)data
		HashMap<String, HashMap<Boolean, Sensordata>> hm_sensor_tt = new HashMap<String, HashMap<Boolean, Sensordata>>();
				
		String query = "Select k3.sensorgroup , k1.id, k2.values->'Geschwindigkeit' as speed, k3.edge_id, k4.road_length, k3.reverse, k1.time from ("
				+ "select t1.id, (select time from "+DbConnector.table_sensor_data+" where sensor_id = t1.id and (extract(epoch from now()))-(time/1000) < 600 order by time desc limit 1) "
				+ "from "+DbConnector.table_sensor+" as t1) as k1, "+DbConnector.table_sensor_data+" as k2, "+DbConnector.table_sensor+" as k3, "+DbConnector.table_edges+" as k4	"
				+ "where k1.time IS NOT NULL and k1.id = k2.sensor_id and k1.time = k2.time and k1.id = k3.id and k3.edge_id = k4.sid and values->'Geschwindigkeit' != '0' order by time desc;";
		
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			//Calendar date_utc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
			Calendar dateNow = Calendar.getInstance();
			
			//import sensor-data
			HashMap<String, List<Sensordata>> hm_sensordata = new HashMap<String, List<Sensordata>>();
			while (rs.next()) {
				Sensordata sensordata = new Sensordata();
				String sensorgroup = rs.getString("sensorgroup");
				sensordata.sensorgroup = sensorgroup;
				sensordata.edge_id = rs.getString("edge_id");
				sensordata.speed = rs.getDouble("speed");
				sensordata.time = rs.getLong("time");
				sensordata.reverse = rs.getBoolean("reverse");
				sensordata.length = rs.getInt("road_length");
				
				if(hm_sensordata.containsKey(sensorgroup))
					hm_sensordata.get(sensorgroup).add(sensordata);
				else {
					List<Sensordata> list = new LinkedList<>();
					list.add(sensordata);
					hm_sensordata.put(sensorgroup, list);
				}
			}
			
			//fusion sensorgroup
			Iterator<Entry<String, List<Sensordata>>> it = hm_sensordata.entrySet().iterator();
			while(it.hasNext()) {
				double tt_fusion = 0;
				Entry<String, List<Sensordata>> entry = it.next();
				List<Sensordata> list = entry.getValue();
				for(Sensordata sd : list) {
					tt_fusion += sd.getTt();
				}
				tt_fusion = tt_fusion/list.size();
				
				//save in hashmap
				Sensordata sd = list.get(0);
				sd.tt_fusion = (int)Math.round(tt_fusion);
				if(hm_sensor_tt.containsKey(sd.edge_id)) {
					if(hm_sensor_tt.get(sd.edge_id).containsKey(sd.reverse)) {
						//more than one sensorgroup on an edge_id/reverse -> fusion
						Sensordata sd_existing = hm_sensor_tt.get(sd.edge_id).get(sd.reverse);
						//fusion sensor-traveltime
						//TODO:  Test
						double test = ((1/sd.getAccuracy()+sd_existing.getAccuracy())*( (sd.getAccuracy()*sd.getTt()) + (sd_existing.getAccuracy()*sd_existing.getTt())));
						
						if(sd_existing.acc_fusion == 0) {
							sd_existing.tt_fusion = (int)((1/sd.getAccuracy()+sd_existing.getAccuracy())*( (sd.getAccuracy()*sd.getTt()) + (sd_existing.getAccuracy()*sd_existing.getTt())));
							sd_existing.acc_fusion = (int) ( (1/sd.getAccuracy()+sd_existing.getAccuracy())*(sd.getAccuracy() + sd_existing.getAccuracy()) );
						}
						//it exist a fusion of sensor-data
						else{
							sd_existing.tt_fusion = (int)((1/sd.getAccuracy()+sd_existing.acc_fusion)*( (sd.getAccuracy()*sd.getTt()) + (sd_existing.acc_fusion*sd_existing.tt_fusion)));
							sd_existing.acc_fusion = (int) ( (1/sd.getAccuracy()+sd_existing.acc_fusion)*(sd.getAccuracy() + sd_existing.acc_fusion) );
						}
					}
					else {
						hm_sensor_tt.get(sd.edge_id).put(sd.reverse, sd);
					}
				}
				else {
					HashMap<Boolean, Sensordata> hm = new HashMap<>();
					hm.put(sd.reverse, sd);
					hm_sensor_tt.put(sd.edge_id, hm);
				}
			}
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
		finally{
			try {
				if (rs != null)
					rs.close();
				rs = null;
			} 
			catch (Exception ex) {ex.printStackTrace();/* nothing to do */}
			try {
				if (st != null)	
					st.close();
				st = null;} 
			catch (Exception ex) {/* nothing to do */}
			try {
				if (conn != null) 
					conn.close();
				conn = null;} 
			catch (Exception ex) {/* nothing to do */}
		}
		return hm_sensor_tt;
	}
		
	private void calculateTraveltime(HashMap<String, HashMap<Boolean, List<Trafficstream>>> hm_fcd_tt, HashMap<String, HashMap<Boolean, Sensordata>> hm_sensor_tt, int daygroup) {
		Iterator<Entry<String, HashMap<Boolean, List<Trafficstream>>>> it = hm_fcd_tt.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, HashMap<Boolean, List<Trafficstream>>> entry = it.next();
			HashMap<Boolean, List<Trafficstream>> hm = entry.getValue();
			Iterator<Entry<Boolean, List<Trafficstream>>> it_rev = hm.entrySet().iterator();
			while(it_rev.hasNext()) {
				Entry<Boolean, List<Trafficstream>> entry_rev = it_rev.next();
				List<Trafficstream> list = entry_rev.getValue();
				//#############################################################################
				for(Trafficstream ts : list) {
					//trafficstream.calculateTraveltime(hm_sensor_tt, daygroup);
					
					//add sensor-value
					if(hm_sensor_tt.containsKey(ts.edge_id)) {
						if(hm_sensor_tt.get(ts.edge_id).containsKey(ts.reverse)) {
							Sensordata sensordata = hm_sensor_tt.get(ts.edge_id).get(ts.reverse);
							ts.hm_traveltime.put(sensordata.getPastTimeInSecond(), sensordata.tt_fusion);
						}
					}
							
					
					Iterator<Entry<Integer, Integer>> it_tt = ts.hm_traveltime.entrySet().iterator();
					int sumPastTime = 0;
					double sumAccuracy = 0;
					while(it_tt.hasNext()){
						Entry<Integer, Integer> entry_tt = it_tt.next();
						sumPastTime += Config.intervalMeasuredTraveltime-entry_tt.getKey();
						sumAccuracy += (int)Math.round((((Config.zeroPercentLimit-(double)entry_tt.getKey())/Config.zeroPercentLimit)*100));
					}
					
					//fusion traveltime and accuracy of fcd measurment and sensor-data
					ts.tt = 0;
					ts.accuracy = 0;
					it_tt = ts.hm_traveltime.entrySet().iterator();
					while(it_tt.hasNext()){
						Entry<Integer, Integer> entry_tt = it_tt.next();
						ts.tt += ((Config.intervalMeasuredTraveltime-(double)entry_tt.getKey())/sumPastTime)*entry_tt.getValue();
						int accuracy_tmp = (int)Math.round((((Config.zeroPercentLimit-(double)entry_tt.getKey())/Config.zeroPercentLimit)*100));
						ts.accuracy += (int)Math.round( accuracy_tmp * (accuracy_tmp/sumAccuracy));
					}
					
					
					//fusion with values from hydrograph e.g. 80% from measurement-value -> 20% from hydrograph
					{
						Calendar calRequest = Calendar.getInstance();
						if(hm_tt_hydrograph.containsKey(ts.edge_id)) {
							if(hm_tt_hydrograph.get(ts.edge_id).containsKey(ts.next_edge_id)) {
								Hydrograph hydrograph = hm_tt_hydrograph.get(ts.edge_id).get(ts.next_edge_id);
								//[traveltime, standard deviation]
								int[] fcd_hydrograph = Function.getValueFromTraveltimeHydrograph(hydrograph, calRequest);
								if(fcd_hydrograph != null) {
									double tt_fusion = (ts.tt*((double)ts.accuracy/100)) + (fcd_hydrograph[0]*(1-((double)ts.accuracy/100)));
									ts.tt = (int)Math.round(tt_fusion);
								}
							}
						}
					}
				}
			}
		}
	}
	
	private void export2DB(HashMap<String, HashMap<Boolean, List<Trafficstream>>> hm_fcd_tt, HashMap<String, HashMap<Boolean, Sensordata>> hm_sensor_tt) {
		long actualTime = System.currentTimeMillis();
		LinkedList<String> sqlStatements = new LinkedList<>();
		sqlStatements.add("TRUNCATE "+DbConnector.table_edges_tt+";");
		sqlStatements.add("INSERT INTO "+DbConnector.table_edges_tt+" (sid, reverse, tt, tt_accuracy, next_sid, timestamp) VALUES ");

		//fcd-traveltime
		Iterator<Entry<String, HashMap<Boolean, List<Trafficstream>>>> it = hm_fcd_tt.entrySet().iterator();
		while(it.hasNext()){
			Entry<String, HashMap<Boolean, List<Trafficstream>>> entry = it.next();
			HashMap<Boolean, List<Trafficstream>> hm_reverse = entry.getValue();
			Iterator<Entry<Boolean, List<Trafficstream>>> it_reverse = hm_reverse.entrySet().iterator();
			while(it_reverse.hasNext()) {
				Entry<Boolean, List<Trafficstream>> entry_reverse = it_reverse.next();
				List<Trafficstream> list_trafficstream = entry_reverse.getValue();
				for(Trafficstream ts : list_trafficstream) {
					if(ts.edge_id.equals("ghE#8541"))
						System.out.println("");
					sqlStatements.add("('"+ts.edge_id+"','"+ts.reverse+"', '"+ts.tt+"','"+ts.accuracy+"','"+ts.next_edge_id+"','"+actualTime+"'),");
				}
			}
		}
		int numberOfFcdMeasurment = (sqlStatements.size()-2);
		
		//sensor-data-traveltime -> next sid/did = ""
		Iterator<Entry<String, HashMap<Boolean, Sensordata>>> it_sensor = hm_sensor_tt.entrySet().iterator();
		while(it_sensor.hasNext()){
			Entry<String, HashMap<Boolean, Sensordata>> entry_sensor = it_sensor.next();
			HashMap<Boolean, Sensordata> hm_reverse = entry_sensor.getValue();
			Iterator<Entry<Boolean, Sensordata>> it_reverse = hm_reverse.entrySet().iterator();
			while(it_reverse.hasNext()) {
				Entry<Boolean, Sensordata> entry_reverse = it_reverse.next();
				Sensordata sd = entry_reverse.getValue();
				if(sd.tt_fusion > 0)
					sqlStatements.add("('"+sd.edge_id+"','"+sd.reverse+"', '"+sd.tt_fusion+"','"+sd.acc_fusion+"','','"+actualTime+"'),");
			}
		}
		int numberOfSensorMeasurment = (sqlStatements.size()-numberOfFcdMeasurment-2);
		
		//hydrograph-traveltime
		Calendar calRequest = Calendar.getInstance();
		Iterator<Entry<String, HashMap<String, Hydrograph>>> it_hyd = hm_tt_hydrograph.entrySet().iterator();
		while(it_hyd.hasNext()){
			Entry<String, HashMap<String, Hydrograph>> entry_hyd = it_hyd.next();
			HashMap<String, Hydrograph> hm_hyd = entry_hyd.getValue();
			Iterator<Entry<String, Hydrograph>> it_hyd_nextEdgeId = hm_hyd.entrySet().iterator();
			while(it_hyd_nextEdgeId.hasNext()) {
				Entry<String, Hydrograph> entry_nextEdgeId = it_hyd_nextEdgeId.next();
				Hydrograph hydrograph = entry_nextEdgeId.getValue();
				if(hydrograph.edge_id.equals("ghE#8541"))
						System.out.println("");
				//[traveltime, standard deviation]
				int[] fcd_hydrograph = Function.getValueFromTraveltimeHydrograph(hydrograph, calRequest);
				if(fcd_hydrograph != null) {
					sqlStatements.add("('"+hydrograph.edge_id+"','"+hydrograph.reverse+"', '"+fcd_hydrograph[0]+"','"+Config.accuracyHydrograph+"','"+hydrograph.next_edge_id+"','"+actualTime+"'),");
				}
			}
		}
		int numberOfHydrographMeasurment = (sqlStatements.size()-numberOfFcdMeasurment-numberOfSensorMeasurment-2);
		
		
		if(sqlStatements.size()==2)
			sqlStatements.removeLast();
		else{
			String string = sqlStatements.getLast().substring(0, sqlStatements.getLast().length()-1);
			string = string + " ON CONFLICT (sid, reverse, next_sid) DO NOTHING;";
			sqlStatements.removeLast();
			sqlStatements.add(string);
		}
		
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<sqlStatements.size(); i++) {	
			sb.append(sqlStatements.get(i));
			//System.out.println(sqlStatements.get(i));
		}
		
		
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			st.execute(sb.toString());
			
			System.out.println("Number of data: "+(sqlStatements.size()-2)+" (fcd: "+numberOfFcdMeasurment+" sensor: "+numberOfSensorMeasurment+" hydrograph: "+numberOfHydrographMeasurment+")");
						
		}catch(Exception ex){
			ex.printStackTrace();
		}
		finally{
			try {
				if (rs != null)
					rs.close();
				rs = null;
			} 
			catch (Exception ex) {ex.printStackTrace();/* nothing to do */}
			try {
				if (st != null)	
					st.close();
				st = null;} 
			catch (Exception ex) {/* nothing to do */}
			try {
				if (conn != null) 
					conn.close();
				conn = null;} 
			catch (Exception ex) {/* nothing to do */}
		}
	}
}
