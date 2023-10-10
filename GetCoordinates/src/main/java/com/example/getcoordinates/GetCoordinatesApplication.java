package com.example.getcoordinates;

import com.example.getcoordinates.Entity.Adresse;
import com.example.getcoordinates.Entity.Koordinaten;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Iterator;
import java.util.Map;

@SpringBootApplication
public class GetCoordinatesApplication {

	public static void main(String[] args) {
		SpringApplication.run(GetCoordinatesApplication.class, args);
		WebClient.Builder builder = WebClient.builder();
		Adresse adresse = new Adresse("Leopoldstraße", 5, "Karlsruhe", "BW", "76133", "DE");
		getCoordinates(adresse);
		String forecastSolarUrl = getForecastSolarUrl(adresse.koordinaten);
		String forecastData = (String)builder.build()
				.get()
				.uri(forecastSolarUrl, new Object[0])
				.retrieve()
				.bodyToMono(String.class)
				.block();
		String timestamp = extractTimestampFromJsonResponse(forecastData);
		System.out.println("---------------------------------------------------");
		System.out.println("Latitude: " + adresse.koordinaten.getLat());
		System.out.println("Longitude: " + adresse.koordinaten.getLon());
		System.out.println("Watt per Day: " + timestamp);
		System.out.println("---------------------------------------------------");

	}

	private static void getCoordinates(Adresse adresse) {
		String url = getUrl(adresse);
		WebClient.Builder builder = WebClient.builder();
		String geoData = builder.build()
				.get()
				.uri(url)
				.retrieve()
				.bodyToMono(String.class)
				.block();

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = null;
		try {
			jsonNode = objectMapper.readTree(geoData);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		String lat = jsonNode.get(0).get("lat").asText();
		String lon = jsonNode.get(0).get("lon").asText();
		adresse.setKoordinaten(new Koordinaten(lon,lat));


	}

	private static String getUrl(Adresse adresse) {
		String strasse = adresse.getStrasse();
		int hausnummer = adresse.getHausnummer();
		String stadt = adresse.getStadt();
		String bundesland = adresse.getBundesland();
		String postleitzahl = adresse.getPlz();
		String land = adresse.getLand();

		return String.format(
				"https://geocode.maps.co/search?street=%s+%d&city=%s&state=%s&postalcode=%s&country=%s",
				strasse, hausnummer, stadt, bundesland, postleitzahl, land
		);
	}
	private static String getForecastSolarUrl(Koordinaten koordinaten) {
		String lat = koordinaten.getLat();
		String lon = koordinaten.getLon();
		return String.format("https://api.forecast.solar/estimate/%s/%s/45/0/15?time=utc", lat, lon);
	}

	private static String extractTimestampFromJsonResponse(String jsonResponse) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(jsonResponse);
			JsonNode wattHoursDayNode = jsonNode.path("result").path("watt_hours_day");
			Iterator<Map.Entry<String, JsonNode>> fields = wattHoursDayNode.fields();
			if (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = (Map.Entry)fields.next();
				return ((JsonNode)entry.getValue()).asText();
			}
		} catch (Exception var6) {
			var6.printStackTrace();
		}

		return null;
	}

}
