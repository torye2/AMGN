package amgn.amu.service;

import amgn.amu.dto.AddressRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GeocodingService {
    @Value("${KAKAO_REST_API_KEY}") String apiKey;

    public Optional<double[]> geocode(String query) {
        if (query == null || query.isBlank()) return Optional.empty();

        RestTemplate rest = new RestTemplate();
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "KakaoAK " + apiKey);
        URI uri = UriComponentsBuilder
                .fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                .queryParam("query", query)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        ResponseEntity<Map> r = rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(h), Map.class);
        List<?> docs = (List<?>) ((Map<?,?>) r.getBody()).get("documents");
        if (docs == null || docs.isEmpty()) return Optional.empty();

        Map<?,?> doc = (Map<?,?>) docs.get(0);
        // Kakao: x=경도(longitude), y=위도(latitude)
        double lon = Double.parseDouble(String.valueOf(doc.get("x")));
        double lat = Double.parseDouble(String.valueOf(doc.get("y")));
        return Optional.of(new double[]{lat, lon});
    }

    public String buildGeoQuery(AddressRequest req) {
        String line = Stream.of(req.addressLine1, req.addressLine2)
                .filter(s -> s != null && !s.isBlank())
                .reduce("", (a,b) -> (a + " " + b).trim());
        // detailAddress가 오면 우선, 없으면 line
        String q = (req.detailAddress != null && !req.detailAddress.isBlank()) ? req.detailAddress : line;
        return (q != null && !q.isBlank()) ? q : null;
    }

}

