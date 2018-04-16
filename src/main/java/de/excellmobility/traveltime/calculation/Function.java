package de.excellmobility.traveltime.calculation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import de.excellmobility.traveltime.ResponseTraveltime;
import excell.dailycurves.DaygroupsCreator;
import excell.dailycurves.daygroups.Tagesgruppe;

public class Function {
	
	public static int[] getValueFromTraveltimeHydrographDB(String sid, String next_sid, int daygroup, Calendar calRequest) {
		int[] value = null;
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select * from "+DbConnector.table_hydrograph_edges_tt+" where sid = '"+sid+"' and next_sid = '"+next_sid+"' and daygroup = "+daygroup;
		
		
		try{
			SimpleDateFormat sdf_quarter = new SimpleDateFormat( "HH:mm:ss" );
			Calendar calReqQuarter = Calendar.getInstance();
			calReqQuarter.setTime(sdf_quarter.parse(sdf_quarter.format(calRequest.getTime())));
			//System.out.println(sdf_quarter.format(calReqQuarter.getTime()));
			
			Calendar cal_q_from = Calendar.getInstance();
			Calendar cal_q_until = Calendar.getInstance();
			
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				String[] tt = rs.getString("traveltime").replace("{", "").replace("}", "").replace("\"", "").split(",");
				String[] standard_deviation = rs.getString("standard_deviation").replace("{", "").replace("}", "").replace("\"", "").split(",");
				String[] from_quarter = rs.getString("from_quarter").replace("{", "").replace("}", "").split(",");
				String[] until_quarter = rs.getString("until_quarter").replace("{", "").replace("}", "").split(",");
				
				boolean abort = false;
				for(int i=0; i<from_quarter.length && !abort; i++) {
					cal_q_from.setTime(sdf_quarter.parse(from_quarter[i]));
					cal_q_until.setTime(sdf_quarter.parse(until_quarter[i]));
					
					if(calReqQuarter.compareTo(cal_q_from) > 0 && calReqQuarter.compareTo(cal_q_until) < 0) {
						int a = Double.valueOf(tt[i]).intValue();
						int b = Double.valueOf(standard_deviation[i]).intValue();
						value = new int[2];
						value[0] = a;
						value[1] = b;
						abort = true;
					}
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
		return value;
	}
	
public static int[] getValueFromTraveltimeHydrograph(Hydrograph hg, Calendar calRequest) {
		int[] value = null;
		
		try{
			SimpleDateFormat sdf_quarter = new SimpleDateFormat( "HH:mm:ss" );
			Calendar calReqQuarter = Calendar.getInstance();
			calReqQuarter.setTime(sdf_quarter.parse(sdf_quarter.format(calRequest.getTime())));
			//System.out.println(sdf_quarter.format(calReqQuarter.getTime()));
			
			Calendar cal_q_from = Calendar.getInstance();
			Calendar cal_q_until = Calendar.getInstance();
						
			boolean abort = false;
			for(int i=0; i<hg.from_quarter.length && !abort; i++) {
				cal_q_from.setTime(sdf_quarter.parse(hg.from_quarter[i]));
				cal_q_until.setTime(sdf_quarter.parse(hg.until_quarter[i]));
					
				if(calReqQuarter.compareTo(cal_q_from) > 0 && calReqQuarter.compareTo(cal_q_until) < 0) {
					int a = Double.valueOf(hg.value[i]).intValue();
					int b = Double.valueOf(hg.standard_deviation[i]).intValue();
					value = new int[2];
					value[0] = a;
					value[1] = b;
					abort = true;
				}
			}
			
						
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return value;
	}
	
	public static Hydrograph importTraveltimeHydrograph(String sid, String next_sid, int daygroup) {
		Hydrograph hydrograph = null;
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select * from "+DbConnector.table_hydrograph_edges_tt+" where sid = '"+sid+"' and next_sid = '"+next_sid+"' and daygroup = "+daygroup;
		
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			hydrograph = new Hydrograph();
			while (rs.next()) {
				hydrograph.edge_id = sid;
				hydrograph.next_edge_id = next_sid;
				hydrograph.daygroup = daygroup;
				hydrograph.value = rs.getString("traveltime").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.standard_deviation = rs.getString("standard_deviation").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.from_quarter = rs.getString("from_quarter").replace("{", "").replace("}", "").split(",");
				hydrograph.until_quarter = rs.getString("until_quarter").replace("{", "").replace("}", "").split(",");
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
		return hydrograph;
	}
	
	public static HashMap<String, HashMap<String, Hydrograph>> importTraveltimeHydrographAll(int daygroup) {
		HashMap<String, HashMap<String, Hydrograph>> hm = new HashMap<>();
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select * from "+DbConnector.table_hydrograph_edges_tt+" where daygroup = "+daygroup;
		
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			
			while (rs.next()) {
				Hydrograph hydrograph = new Hydrograph();
				hydrograph.edge_id = rs.getString("sid");
				hydrograph.reverse = rs.getBoolean("reverse");
				hydrograph.next_edge_id = rs.getString("next_sid");;
				hydrograph.daygroup = daygroup;
				hydrograph.value = rs.getString("traveltime").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.standard_deviation = rs.getString("standard_deviation").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.from_quarter = rs.getString("from_quarter").replace("{", "").replace("}", "").split(",");
				hydrograph.until_quarter = rs.getString("until_quarter").replace("{", "").replace("}", "").split(",");
				if(hm.containsKey(rs.getString("sid"))) {
					hm.get(rs.getString("sid")).put(rs.getString("next_sid"), hydrograph);
				}
				else {
					HashMap<String, Hydrograph> hm_tmp = new HashMap<>();
					hm_tmp.put(rs.getString("next_sid"), hydrograph);
					hm.put(rs.getString("sid"), hm_tmp);
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
		return hm;
	}
	
	public static HashMap<String, HashMap<String, Hydrograph>> importTraveltimeHydrographDresden(int daygroup) {
		HashMap<String, HashMap<String, Hydrograph>> hm = new HashMap<>();
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select * from "+DbConnector.table_hydrograph_edges_tt+" as t1, "+DbConnector.table_edges_dresden+" as t2 where t1.daygroup = "+daygroup+" and t1.sid = t2.sid";
		
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			
			while (rs.next()) {
				Hydrograph hydrograph = new Hydrograph();
				hydrograph.edge_id = rs.getString("sid");
				hydrograph.reverse = rs.getBoolean("reverse");
				hydrograph.next_edge_id = rs.getString("next_sid");;
				hydrograph.daygroup = daygroup;
				hydrograph.value = rs.getString("traveltime").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.standard_deviation = rs.getString("standard_deviation").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.from_quarter = rs.getString("from_quarter").replace("{", "").replace("}", "").split(",");
				hydrograph.until_quarter = rs.getString("until_quarter").replace("{", "").replace("}", "").split(",");
				if(hm.containsKey(rs.getString("sid"))) {
					hm.get(rs.getString("sid")).put(rs.getString("next_sid"), hydrograph);
				}
				else {
					HashMap<String, Hydrograph> hm_tmp = new HashMap<>();
					hm_tmp.put(rs.getString("next_sid"), hydrograph);
					hm.put(rs.getString("sid"), hm_tmp);
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
		return hm;
	}
	
	
	public static int[] getValueFromSensorHydrographDB(String sid, boolean reverse, int daygroup, Calendar calRequest) {
		
		int[] value = null;
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select t1.id, t1.sensorgroup, t2.speed, t2.standard_deviation, t2.from_quarter, t2.until_quarter  "
				+ "from (select * from "+DbConnector.table_sensor+" where edge_id = '"+sid+"' and reverse = "+reverse+") as t1, "+DbConnector.table_sensor_data_speed_hydrograph+" as t2 "
				+ "where t1.id = t2.sensor_id and t2.daygroup = "+daygroup+";";
		
		try{
			SimpleDateFormat sdf_quarter = new SimpleDateFormat( "HH:mm:ss" );
			Calendar calReqQuarter = Calendar.getInstance();
			calReqQuarter.setTime(sdf_quarter.parse(sdf_quarter.format(calRequest.getTime())));
			//System.out.println(sdf_quarter.format(calReqQuarter.getTime()));
			
			Calendar cal_q_from = Calendar.getInstance();
			Calendar cal_q_until = Calendar.getInstance();
			
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			int num = 0; 
			while (rs.next()) {
				String[] speed = rs.getString("speed").replace("{", "").replace("}", "").replace("\"", "").split(",");
				String[] standard_deviation = rs.getString("standard_deviation").replace("{", "").replace("}", "").replace("\"", "").split(",");
				String[] from_quarter = rs.getString("from_quarter").replace("{", "").replace("}", "").split(",");
				String[] until_quarter = rs.getString("until_quarter").replace("{", "").replace("}", "").split(",");
				
				boolean abort = false;
				for(int i=0; i<from_quarter.length && !abort; i++) {
					cal_q_from.setTime(sdf_quarter.parse(from_quarter[i]));
					cal_q_until.setTime(sdf_quarter.parse(from_quarter[i]));
					
					if(calReqQuarter.compareTo(cal_q_from) > 0 && calReqQuarter.compareTo(cal_q_until) < 0) {
						int a = Double.valueOf(speed[i]).intValue();
						int b = Double.valueOf(standard_deviation[i]).intValue();
						if(value == null)
							value = new int[2];
						value[0] += a;
						value[1] += b;
						abort = true;
						num++;
					}
				}
			}
			if(num > 0) {
				value[0] = (int)((double)value[0]/num);
				value[1] = (int)((double)value[1]/num);
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
		return value;
	}
	
public static List<Hydrograph> importSensorHydrographDresden(int daygroup) {
		List<Hydrograph> list_hydrograph = new LinkedList<>();
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select t1.sensor_id, t1.daygroup, t1.speed, t1.standard_deviation, t1.from_quarter, t1.until_quarter, t2.edge_id, t2.reverse, t3.road_length "
				+ "from (select * from "+DbConnector.table_sensor_data_speed_hydrograph+" where daygroup = "+daygroup+") as t1, "+DbConnector.table_sensor+" as t2, "+DbConnector.table_edges+" as t3 "
				+ "where t1.sensor_id = t2.id and t2.edge_id != '' and t2.edge_id = t3.sid;";
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				Hydrograph hydrograph = new Hydrograph();
				hydrograph.edge_id = rs.getString("edge_id");
				hydrograph.reverse = rs.getBoolean("reverse");
				hydrograph.daygroup = daygroup;
				hydrograph.value = rs.getString("speed").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.standard_deviation = rs.getString("standard_deviation").replace("{", "").replace("}", "").replace("\"", "").split(",");
				hydrograph.from_quarter = rs.getString("from_quarter").replace("{", "").replace("}", "").split(",");
				hydrograph.until_quarter = rs.getString("until_quarter").replace("{", "").replace("}", "").split(",");
				hydrograph.road_length = rs.getDouble("road_length");
				list_hydrograph.add(hydrograph);
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
		return list_hydrograph;
	}

	public static int[] getValueFromSensorHydrograph(Hydrograph hg, Calendar calRequest) {
		int[] value = null;
		
		try{
			SimpleDateFormat sdf_quarter = new SimpleDateFormat( "HH:mm:ss" );
			Calendar calReqQuarter = Calendar.getInstance();
			calReqQuarter.setTime(sdf_quarter.parse(sdf_quarter.format(calRequest.getTime())));
			//System.out.println(sdf_quarter.format(calReqQuarter.getTime()));
			
			Calendar cal_q_from = Calendar.getInstance();
			Calendar cal_q_until = Calendar.getInstance();
						
			boolean abort = false;
			for(int i=0; i<hg.from_quarter.length && !abort; i++) {
				cal_q_from.setTime(sdf_quarter.parse(hg.from_quarter[i]));
				cal_q_until.setTime(sdf_quarter.parse(hg.until_quarter[i]));
					
				if(calReqQuarter.compareTo(cal_q_from) > 0 && calReqQuarter.compareTo(cal_q_until) < 0) {
					int a = Double.valueOf(hg.value[i]).intValue();
					int b = Double.valueOf(hg.standard_deviation[i]).intValue();
					value = new int[2];
					value[0] = a;
					value[1] = b;
					abort = true;
				}
			}
			
						
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return value;
	}
	
	public static int[] getLengthOfEdge(String sid, boolean reverse) {
		int length = -1;
		int maxspeed = -1;
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "SELECT sid, road_length, CASE WHEN "+reverse+" THEN maxspeed_reverse ELSE maxspeed END AS speed "
				+ "FROM "+DbConnector.table_edges+" where sid = '"+sid+"';"; 
		
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				length = rs.getInt("road_length");
				maxspeed = rs.getInt("speed");
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
		int[] output = {length, maxspeed};
		return output;
	}
	
	//only for dresden
	public static int getDaygroupDresden(Calendar cal) {
		int tg = 99;
		DaygroupsCreator erzeuger=new DaygroupsCreator();
		Tagesgruppe tagesgruppe=erzeuger.erzeugeNeueTagesgruppeZumDatum(cal);	
		//System.out.println(tagesgruppe);
		if (tagesgruppe!= null)
			tg = tagesgruppe.getTagesgruppenId();
		return tg;
	}
	
	public static int getDaygroupWithoutHolidays(Calendar cal) {
		//day of week: from 1-sunday until 7-saturday
		int daygroup = cal.get(Calendar.DAY_OF_WEEK);
		//System.out.println(tagesgruppe);
		return daygroup;
	}
	
	public static double[] getActualTraveltimeFromDB(String sid, String next_sid) {
		double[] value = null;
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select * from "+DbConnector.table_edges_tt+" where sid = '"+sid+"' and next_sid = '"+next_sid+"'";
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				value = new double[2];
				int tt = rs.getInt("tt");
				int accuracy = rs.getInt("tt_accuracy");
				value[0] = tt;
				value[1] = accuracy;
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
		return value;
	}
	
	public static double[] getActualSensorTraveltimeFromDB(String sid, boolean reverse) {
		double[] value = null;
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select * from "+DbConnector.table_edges_tt+" where sid = '"+sid+"' and next_sid = '' and reverse = '"+reverse+"'";
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				value = new double[2];
				int tt = rs.getInt("tt");
				int accuracy = rs.getInt("tt_accuracy");
				value[0] = tt;
				value[1] = accuracy;
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
		return value;
	}

	public static List<ResponseTraveltime> getActualTraveltimeDresdenFromDB() {
		Calendar calRequest = Calendar.getInstance();
		List<ResponseTraveltime> list = new LinkedList<>();
		Statement st = null;
		ResultSet rs = null;
		Connection conn = null;
		String query = "select t1.sid, t1.next_sid, t1.reverse, t1.tt, t1.tt_accuracy, t1.timestamp, to_timestamp(t1.timestamp/1000) from "+DbConnector.table_edges_tt+" as t1, infra.edges_dresden as t2 where t1.sid = t2.sid";
		try{
			conn = DriverManager.getConnection("jdbc:postgresql://" + DbConnector.excelldb_host + ":" + DbConnector.excelldb_port + "/" + DbConnector.excelldb_name + "", DbConnector.excelldb_user, DbConnector.excelldb_pass);
			st = conn.createStatement();
			rs = st.executeQuery(query);
			while (rs.next()) {
				ResponseTraveltime rt = new ResponseTraveltime(rs.getString("sid"), rs.getString("next_sid"), rs.getBoolean("reverse"), rs.getInt("tt"), rs.getInt("tt_accuracy"), 0, calRequest.getTimeInMillis());
				list.add(rt);
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
		return list;
		
	}
	
	public static double[] fusionTt(List<double[]> listFusion) {
		double fusionTt = 0;
		double fusionAccuracy = 0;
		int sumWeighting = 0;
		
		if(listFusion.get(0)[1] == 0)
			System.out.print("");
		
		//sum weightung * accuracy
		for(int i=0;i<listFusion.size();i++){
			sumWeighting += listFusion.get(i)[1]*listFusion.get(i)[2];
		}
		if(sumWeighting != 0){
			for(int i=0;i<listFusion.size();i++){
				fusionTt += (double)listFusion.get(i)[0]*((listFusion.get(i)[1]*listFusion.get(i)[2])/sumWeighting);
				fusionAccuracy += (double)listFusion.get(i)[1]*((listFusion.get(i)[1]*listFusion.get(i)[2])/sumWeighting);
			}
		}
		else{
			fusionTt = 1;
			fusionAccuracy = 0;
		}
			
		double [] output = new double[2];
		output[0] = fusionTt;
		output[1] = fusionAccuracy;
		return output;
	}
	

}
