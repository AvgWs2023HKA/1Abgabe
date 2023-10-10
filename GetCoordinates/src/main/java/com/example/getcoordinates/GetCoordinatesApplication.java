


package com.example.getcoordinates;

import com.example.getcoordinates.Entity.Adresse;
import com.example.getcoordinates.Entity.Koordinaten;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Iterator;
import java.util.Map;

@SpringBootApplication
@Getter
@AllArgsConstructor
public class GetCoordinatesApplication {
	public static void main(String[] args) throws JsonProcessingException {


		//Koordinaten erhalten

        SpringApplication.run(GetCoordinatesApplication.class, args);
        String url = getUrl();

        WebClient.Builder builder = WebClient.builder();

        String geoData = builder
				.build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(geoData);

        String lat = jsonNode.get(0).get("lat").asText();
        String lon = jsonNode.get(0).get("lon").asText();
        Koordinaten koordinaten = new Koordinaten(lat, lon);



		//__________________________________________________________________________________________________________________

		// Energiewerte für die nächsten 24 Stunden

        String forecastSolarUrl = getForecastSolarUrl(koordinaten);
		String forecastData = builder
				.build()
				.get()
				.uri(forecastSolarUrl)
				.retrieve()
				.bodyToMono(String.class)
				.block();

		String timestamp = extractTimestampFromJsonResponse(forecastData);


		//__________________________________________________________________________________________________________________


		System.out.println("---------------------------------------------------");
        System.out.println("Latitude: " + koordinaten.getLat());
        System.out.println("Longitude: " + koordinaten.getLon());
		System.out.println("Watt per Day: " + timestamp);
        System.out.println("---------------------------------------------------");

    }


	private static String getUrl() {
		Adresse adresse = new Adresse("Hans-Thoma-Straße", 29, "Au am Rhein", "BW", "76474","DE");

		String strasse = String.valueOf(adresse.strasse);
		int hausnummer = adresse.hausnummer;
		String stadt = adresse.stadt;
		String bundesland = adresse.bundesland;
		String postleitzahl = adresse.plz;
		String land = adresse.land;

// Den URI-String mit den Adresse-Attributen füllen
        return String.format(
				"https://geocode.maps.co/search?street=%s+%d&city=%s&state=%s&postalcode=%s&country=%s",
				strasse, hausnummer, stadt, bundesland, postleitzahl, land
		);
	}




	//__________________________________________________________________________________________________________________
	private static String getForecastSolarUrl(Koordinaten koordinaten) {
		String lat = koordinaten.getLat();
		String lon = koordinaten.getLon();

		return String.format("https://api.forecast.solar/estimate/%s/%s/45/0/15?time=utc",
			lat, lon);
	}


	private static String extractTimestampFromJsonResponse(String jsonResponse) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode jsonNode = objectMapper.readTree(jsonResponse);
			JsonNode wattHoursDayNode = jsonNode.path("result").path("watt_hours_day");
			Iterator<Map.Entry<String, JsonNode>> fields = wattHoursDayNode.fields();
			if (fields.hasNext()) {
				Map.Entry<String, JsonNode> entry = fields.next();
				return entry.getValue().asText();
			}
		} catch (Exception e) {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
		}
		return null;
	}
}



// Um die best möglichste Ausrichtung zu erzielen; Prototype

/*
package com.example.getcoordinates;

import com.example.getcoordinates.Entity.Adresse;
import com.example.getcoordinates.Entity.Koordinaten;
import com.example.getcoordinates.Entity.SolarForecast;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Iterator;
import java.util.Map;

@SpringBootApplication
@Getter
@AllArgsConstructor
public class GetCoordinatesApplication {

    public static void main(String[] args) throws JsonProcessingException {


        SpringApplication.run(GetCoordinatesApplication.class, args);
        String url = getUrl();

        WebClient.Builder builder = WebClient.builder();

        String geoData = builder
                .build()
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(geoData);

        String lat = jsonNode.get(0).get("lat").asText();
        String lon = jsonNode.get(0).get("lon").asText();
        Koordinaten koordinaten = new Koordinaten(lat, lon);

        double bestWattPeak = 0.0;
        double bestAzimuth = 0.0;
        double kwp = 15.0;
        String bestTimestamp = "";

        for (double winkel = 0.0; winkel <= 5.0; winkel += 5.0) {
            for (double ausrichtung = 0.0; ausrichtung <= 10.0; ausrichtung += 5.0) {

                    String forecastSolarUrl = getForecastSolarUrl(koordinaten, winkel, ausrichtung, kwp);

                    String forecastData = builder
                            .build()
                            .get()
                            .uri(forecastSolarUrl)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

                    SolarForecast forecast = objectMapper.readValue(forecastData, SolarForecast.class);

                    double currentWattPeak = forecast.getWattPeak(bestTimestamp);
                    double currentAzimuth = forecast.getAzimuth(bestTimestamp);
                    double currentKwp = forecast.getKwp(bestTimestamp);

                    if (currentWattPeak > bestWattPeak) {
                        bestWattPeak = currentWattPeak;
                        bestAzimuth = currentAzimuth;
                        kwp = currentKwp;
                        bestTimestamp = extractTimestampFromJsonResponse(forecastData);

                }
            }
        }





        System.out.println("---------------------------------------------------");
        System.out.println("Latitude: " + koordinaten.getLat());
        System.out.println("Longitude: " + koordinaten.getLon());

        System.out.println("Best Watt Peak: " + bestWattPeak);
        System.out.println("Best Azimuth: " + bestAzimuth);
        System.out.println("kWp: " + kwp);
        System.out.println("Timestamp: " + bestTimestamp);
        System.out.println("---------------------------------------------------");
    }

    private static String getUrl() {
        Adresse adresse = new Adresse("Leopoldstraße", 5, "Karlsruhe", "BW", "76133","DE");

        String strasse = String.valueOf(adresse.strasse);
        int hausnummer = adresse.hausnummer;
        String stadt = adresse.stadt;
        String bundesland = adresse.bundesland;
        String postleitzahl = adresse.plz;
        String land = adresse.land;

// Den URI-String mit den Adresse-Attributen füllen
        return String.format(
                "https://geocode.maps.co/search?street=%s+%d&city=%s&state=%s&postalcode=%s&country=%s",
                strasse, hausnummer, stadt, bundesland, postleitzahl, land
        );
    }
    private static String getForecastSolarUrl(Koordinaten koordinaten) {
        String lat = koordinaten.getLat();
        String lon = koordinaten.getLon();

        return String.format("https://api.forecast.solar/estimate/%s/%s/90/90/5?time=utc",
                lat, lon);
    }
    private static String extractTimestampFromJsonResponse(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            JsonNode wattsNode = jsonNode.path("result").path("watts");
            Iterator<Map.Entry<String, JsonNode>> fields = wattsNode.fields();
            if (fields.hasNext()) {
                return fields.next().getKey();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private static String getForecastSolarUrl(Koordinaten koordinaten, double winkel, double ausrichtung, double kwp) {
        String lat = koordinaten.getLat();
        String lon = koordinaten.getLon();

        return String.format("https://api.forecast.solar/estimate/%s/%s/%f/%f/%f?time=utc",
                lat, lon, winkel, ausrichtung, kwp);
    }


}

 */



