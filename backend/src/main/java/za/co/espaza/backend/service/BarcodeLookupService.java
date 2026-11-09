package za.co.espaza.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Optional catalogue suggestion. Shop prices and stock never come from this service. */
@Service
public class BarcodeLookupService {
    private static final String SOURCE = "Open Food Facts";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper mapper;

    public BarcodeLookupService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public LookupResult lookup(String barcode) {
        if (barcode == null || !barcode.matches("[0-9]{8,14}")) {
            return new LookupResult("NOT_FOUND", null, null, SOURCE);
        }
        var request = HttpRequest.newBuilder()
                .uri(URI.create("https://world.openfoodfacts.org/api/v3/product/" + barcode
                        + "?fields=code,product_name,product_name_en,quantity"))
                .timeout(Duration.ofSeconds(7))
                .header("User-Agent", "e-SpazaSethu/1.0 (https://github.com/Sam-168/e-SpazaSethu)")
                .header("Accept", "application/json")
                .GET().build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return new LookupResult("NOT_FOUND", null, null, SOURCE);
            }
            if (response.statusCode() != 200) {
                return new LookupResult("UNAVAILABLE", null, null, SOURCE);
            }
            JsonNode root = mapper.readTree(response.body());
            if (!"product_found".equals(root.path("result").path("id").asText())) {
                return new LookupResult("NOT_FOUND", null, null, SOURCE);
            }
            JsonNode product = root.path("product");
            String name = text(product, "product_name_en");
            if (name == null) name = text(product, "product_name");
            if (name == null) return new LookupResult("NOT_FOUND", null, null, SOURCE);
            return new LookupResult("FOUND", name, text(product, "quantity"), SOURCE);
        } catch (IOException e) {
            return new LookupResult("UNAVAILABLE", null, null, SOURCE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new LookupResult("UNAVAILABLE", null, null, SOURCE);
        }
    }

    private static String text(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    public record LookupResult(String status, String name, String quantity, String source) {}
}
