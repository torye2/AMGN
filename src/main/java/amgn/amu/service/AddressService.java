package amgn.amu.service;

import amgn.amu.common.AppException;
import amgn.amu.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class AddressService {
    public record ParsedAddress(String province, String city, String detail) {}

    @Value("${kakao.api.key}") String kakaoApiKey;
    private final RestClient rest = RestClient.create();

    @SuppressWarnings("unchecked")
    public ParsedAddress parseFromKakao(String query) {
        var res = rest.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("dapi.kakao.com")
                        .path("/v2/local/search/address.json")
                        .queryParam("query", query)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
                .retrieve()
                .body(Map.class);

        List<Map<String, Object>> docs = (List<Map<String, Object>>) res.get("documents");
        if(docs == null || docs.isEmpty()) throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);

        Map<String, Object> first = docs.get(0);
        Map<String, Object> addr = (Map<String, Object>) first.getOrDefault("road_address", first.get("address"));
        if(addr == null) throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);

        String province = (String) addr.getOrDefault("region_1depth_name", "");
        String city = (String) addr.getOrDefault("region_2depth_name", "");
        String detail = (String) addr.getOrDefault("address_name", "");

        if(province.isBlank() || city.isBlank()) throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        return new ParsedAddress(province, city, detail);
    }
}
