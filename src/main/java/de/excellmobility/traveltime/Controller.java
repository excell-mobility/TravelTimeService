package de.excellmobility.traveltime;
import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.excellmobility.traveltime.calculation.Calculation;
import de.excellmobility.traveltime.calculation.Config;
import de.excellmobility.traveltime.calculation.DbConnector;
import io.swagger.annotations.ApiOperation;

@RestController
public class Controller {
	
	@Autowired
	private DbConnector dbConn;
	
	Calculation calculation;
		
	public Controller() {
		System.out.println("##################  Rest Api gestartet  ##################");
		
		calculation = new Calculation();
		
		
					
		//Zugriff ueber: http://localhost:20039/TraveltimeService-swagger/swagger-ui.html
	}
	
		
	@RequestMapping(value = { "/TravelTimeService/getActualTraveltime" }, method = { org.springframework.web.bind.annotation.RequestMethod.GET })
	@ApiOperation(value = "get the actual traveltime for the relationship from sid to next_sid", produces = "application/json")
	public ResponseTraveltime getActualTraveltime(String sid, String next_sid, boolean reverse) {
		System.out.println("DB-Connenctor: "+dbConn);
		System.out.println(dbConn.excelldb_host);
		long time = Calendar.getInstance().getTimeInMillis();
		ResponseTraveltime rtt = calculation.getTraveltime(sid, next_sid, reverse, time);
		return rtt;
	}
	
	@RequestMapping(value = { "/TravelTimeService/getTraveltime" }, method = { org.springframework.web.bind.annotation.RequestMethod.GET })
	@ApiOperation(value = "get the traveltime for the relationship from sid to next_sid for the given time", produces = "application/json")
	public ResponseTraveltime getTraveltime(String sid, String next_sid, boolean reverse, long time) {
		ResponseTraveltime rtt = calculation.getTraveltime(sid, next_sid, reverse, time);
		return rtt;
	}
	
	@RequestMapping(value = { "/TravelTimeService/getActualTraveltimeDresden" }, method = { org.springframework.web.bind.annotation.RequestMethod.GET })
	@ApiOperation(value = "get the actual traveltime for all trafficstreams in dresden", produces = "application/json")
	public List<ResponseTraveltime> getActualTraveltimeDresden() {
		List<ResponseTraveltime> list = calculation.getActualTraveltimeDresden();
		return list;
	}
	
	@RequestMapping(value = { "/TravelTimeService/getTraveltimeDresden" }, method = { org.springframework.web.bind.annotation.RequestMethod.GET })
	@ApiOperation(value = "get the actual traveltime for all trafficstreams in dresden for the given time", produces = "application/json")
	public List<ResponseTraveltime> getTraveltimeDresden(long time) {
		List<ResponseTraveltime> list = calculation.getTraveltimeDresden(time);
		return list;
	}

}
