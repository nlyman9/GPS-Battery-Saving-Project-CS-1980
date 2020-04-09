import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
	
	
	/**
	 * 
	 * Given start and end coordinates, queries the Waze routing servers for estimated time of arrival.
	 * 
	 * @param toLatitude latitude String of end point
	 * @param toLongitude longitude String of end point
	 * @param fromLatitude latitude String of start point
	 * @param fromLongitude longitude String of end point
	 * @return travel time on success, -1 on failure
	 */
	public static float getTravelTime(String toLatitude, String toLongitude, String fromLatitude, String fromLongitude) {
		String webURL = String.format(REQUEST_URL_FORMAT_STRING, fromLongitude, fromLatitude, toLongitude, toLatitude); 
		String referrerURL = String.format(REFERRER_FORMAT_STRING, toLatitude, toLongitude);
		
		try {
			// Connect to the web page.
			Document document = Jsoup.connect(webURL)
									 .header("accept-encoding", "gzip, deflate, br")
									 .header("accept-language", "en-US,en;q=0.9")
									 .userAgent(USER_AGENT)
				      				 .referrer(referrerURL)
				      				 .get();
			
			// Parse the JSON response to get the total route time out of the trip.
			String json = document.body().text();											    
			JsonParser parser = new JsonParser();
			JsonObject obj = parser.parse(json).getAsJsonObject();
			JsonObject response = obj.getAsJsonObject("response");
			JsonPrimitive totalRouteTime = response.getAsJsonPrimitive("totalRouteTime");
			return totalRouteTime.getAsFloat();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	
	public static void main(String[] args) {
		System.out.println(getTravelTime(TO_LATITUDE, TO_LONGITUDE, FROM_LATITUDE, FROM_LONGITUDE));
	}

}
