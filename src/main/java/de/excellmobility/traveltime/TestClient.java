package de.excellmobility.traveltime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.springframework.web.client.RestTemplate;

public class TestClient {

	public static void main(String[] args) {
		RestTemplate rt = new RestTemplate();
		System.out.println(Calendar.getInstance().getTimeInMillis());
		
		//long time = 1513948927;
		//TODO: test jetziger Zeitpunkt
		//Calendar date = Calendar.getInstance();
		//long time = date.getTimeInMillis();
				
		//TODO: Test für die TGL
		Calendar date = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		try {
			date.setTime(sdf.parse("2018-01-05 12:11:00"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(date.getTimeInMillis());			
		
		ResponseTraveltime x = rt.getForObject("http://localhost:20039/TraveltimeService/getActualTraveltime?sid=ghE%2347689&next_sid=ghE%2347688", ResponseTraveltime.class);
		System.out.println("sid: "+x.sid);
		System.out.println("next_sid: "+x.next_sid);
		System.out.println("tt: "+x.traveltime);
		System.out.println("accuracy: "+x.accuracy);
		System.out.println("standard deviation: "+x.standardDeviation);
				

	}

}
