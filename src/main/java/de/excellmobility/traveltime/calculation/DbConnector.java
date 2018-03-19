package de.excellmobility.traveltime.calculation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DbConnector {
	
	@Value("${dbconnector.edgerecorddb.host}")
	public static String excelldb_host = "10.24.10.54";
	
	@Value("${dbconnector.edgerecorddb.port}")
  	public static String excelldb_port = "5432";
	
	@Value("${dbconnector.edgerecorddb.user}")
  	public static String excelldb_user = "excell";
	
	@Value("${dbconnector.edgerecorddb.pwd}")
  	public static String excelldb_pass = "teid5eiP";
	
	@Value("${dbconnector.edgerecorddb.dbName}")
  	public static String excelldb_name = "excell";
  	
	
	@Value("${dbconnector.edgerecorddb.table_edges}")
  	public static String table_edges = "infra.edges";
	
	@Value("${dbconnector.edgerecorddb.table_edges_dresden}")
  	public static String table_edges_dresden = "infra.edges_dresden";
	
	@Value("${dbconnector.edgerecorddb.table_edge_records}")
  	public static String table_edge_records = "raw.edge_records";
	
	@Value("${dbconnector.edgerecorddb.table_edges_tt}")
  	public static String table_edges_tt = "proc.edges_tt";
	
	@Value("${dbconnector.edgerecorddb.table_tt_hydrograph_edges}")
  	public static String table_hydrograph_edges_tt = "proc.tt_hydrograph_edges";
	
	@Value("${dbconnector.edgerecorddb.table_sensor_data_speed_hydrograph}")
  	public static String table_sensor_data_speed_hydrograph = "proc.sensor_data_speed_hydrograph";
	
	@Value("${dbconnector.edgerecorddb.table_sensor_data}")
  	public static String table_sensor_data = "raw.sensor_data";
	
	@Value("${dbconnector.edgerecorddb.table_sensor}")
  	public static String table_sensor = "infra.sensor";

	
	private static DbConnector instance;
	
	private DbConnector()
	  {
	    instance = this;
	  }

	  public static DbConnector getInstance()
	  {
	    return instance;
	  }

}
