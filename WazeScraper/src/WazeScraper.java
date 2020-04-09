import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * 
 * This program seeks to serve as PoC to scrape travel time data from
 * Waze's LiveMap service. It requires JSoup.
 * 
 * Example web query: 
 * 
 * https://www.waze.com/livemap/directions?to=ll.40.441541%2C-79.9564292&from=ll.40.441541%2C-79.9564292
 * 
 **/
public class WazeScraper {
	
	public static final String TO_LATITUDE = "40.441541";
	public static final String TO_LONGITUDE = "-79.9564292";
	public static final String FROM_LATITUDE = "40.4425";
	public static final String FROM_LONGITUDE = "-79.9541667";
	
	public static final String REQUEST_URL_FORMAT_STRING = "https://www.waze.com/RoutingManager/routingRequest?at=0&clientVersion=4.0.0&from=x:%s y:%s&nPaths=3&options=AVOID_TRAILS:t,ALLOW_UTURNS:t&returnGeometries=true&returnInstructions=true&returnJSON=true&timeout=60000&to=x:%s y:%s";
	public static final String REFERRER_FORMAT_STRING = "https://www.waze.com/livemap/directions?utm_campaign=waze_website&utm_source=waze_website&to=ll.%s,%s";
	public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36";
	//"https://www.waze.com/RoutingManager/routingRequest?at=0&clientVersion=4.0.0&from=x:-79.9541667 y:40.4425&nPaths=3&options=AVOID_TRAILS:t,ALLOW_UTURNS:t&returnGeometries=true&returnInstructions=true&returnJSON=true&timeout=60000&to=x:-79.9564292 y:40.441541"
	
	public static final String WEB_URL = String.format(REQUEST_URL_FORMAT_STRING, FROM_LONGITUDE, FROM_LATITUDE, TO_LONGITUDE, TO_LATITUDE); 
	public static final String REFERRER_URL = String.format(REFERRER_FORMAT_STRING, TO_LATITUDE, TO_LONGITUDE);
	
	// Given a to and from latitude/longitude touple, returns the expected travel time.
	public static long getTravelTime(String toLatitude, String toLongitude, String fromLatitude, String fromLongitude) {
		try {
			// Connect to the webpage.
			Document document = Jsoup.connect(WEB_URL)
									 .header("accept-encoding", "gzip, deflate, br")
									 .header("accept-language", "en-US,en;q=0.9")
									 .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36")
				      				 .referrer(REFERRER_URL)
				      				 .get();
			
			// Parse the JSON response to get the total route time out of the trip.
			String json = document.body().text();											    
			JsonParser parser = new JsonParser();
			JsonObject obj = parser.parse(json).getAsJsonObject();
			JsonObject response = obj.getAsJsonObject("response");
			JsonPrimitive totalRouteTime = response.getAsJsonPrimitive("totalRouteTime");
			return totalRouteTime.getAsLong();
		} catch (IOException e) {
			e.printStackTrace();
		}
	return -1;
	}
	
	
	public static void main(String[] args) {
		try {
			// Connect to the webpage.
			Document document = Jsoup.connect(WEB_URL)
									 .header("accept-encoding", "gzip, deflate, br")
									 .header("accept-language", "en-US,en;q=0.9")
									 .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36")
				      				 .referrer(REFERRER_URL)
				      				 .get();
			
			String json = document.body().text();
			
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
											    
			System.out.println(json);
						
			JsonParser parser = new JsonParser();
			JsonObject obj = parser.parse(json).getAsJsonObject();
			JsonObject response = obj.getAsJsonObject("response");
			JsonPrimitive totalRouteTime = response.getAsJsonPrimitive("totalRouteTime");
			System.out.println(totalRouteTime);
			JsonArray results = response.getAsJsonArray("results");
			
			for (JsonElement result : results) {
				System.out.println(result);
				JsonObject resultObject = result.getAsJsonObject();
				JsonObject path = resultObject.getAsJsonObject("path");

				System.out.println(path);
			}



		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
