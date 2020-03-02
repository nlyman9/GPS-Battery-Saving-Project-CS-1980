import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * 
 * This program seeks to serve as PoC to scrape travel time data from
 * Waze's LiveMap service. It requires JSoup.
 * 
 * Example web query: 
 * 
 * https://www.waze.com/livemap/directions?to=ll.40.441541%2C-79.9564292&from=ll.40.441541%2C-79.9564292
 * 
 * The attribute we are seeking to grab is:
 * 
 * #map > div.wm-cards > div.wm-card.is-routing > div > div.wm-routes > ul > li > div > div > div.wm-routing-item__time
 *
 */
public class WazeScraper {
	
	public static final String WEB_URL = "https://www.waze.com/RoutingManager/routingRequest?at=0&clientVersion=4.0.0&from=x%3A-79.9541667%20y%3A40.4425&nPaths=3&options=AVOID_TRAILS%3At%2CALLOW_UTURNS%3At&returnGeometries=true&returnInstructions=true&returnJSON=true&timeout=60000&to=x%3A-79.9564292%20y%3A40.441541";
	
	public static void main(String[] args) {
		try {
			// Connect to the webpage.
			Document document = Jsoup.connect(WEB_URL)
									 .header("accept-encoding", "gzip, deflate, br")
									 .header("accept-language", "en-US,en;q=0.9")
									 .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.122 Safari/537.36")
				      				 .referrer("https://www.waze.com/livemap/directions?utm_campaign=waze_website&utm_source=waze_website&to=ll.40.441541%2C-79.9564292")
				      				 .get();
			
			//System.out.println(document.title());
			
			Elements selected = document.select("#map > div.wm-cards > div.wm-card.is-routing");
					    
			System.out.println(document.html());


		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
