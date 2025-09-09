package amgn.amu.service;

import amgn.amu.dto.AddressDto;
import amgn.amu.dto.AddressRequest;
import amgn.amu.entity.UserAddress;
import amgn.amu.entity.UserAddress.AddressStatus;
import amgn.amu.entity.UserAddress.AddressType;
import amgn.amu.repository.UserAddressRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressRepository repo;
    private final GeocodingService geocodingService;

    public List<AddressDto> list(Long userId) {
        return repo.findAllActive(userId, AddressStatus.ACTIVE).stream()
                .map(a -> AddressDto.from(a, null, null))
                .toList();
    }

    public AddressDto get(Long userId, Long addressId) {
        UserAddress a = repo.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("주소를 찾을 수 없습니다."));
        return AddressDto.from(a, null, null);
    }

    @Transactional
    public AddressDto create(Long userId, AddressRequest req) {
        String key = computeAddrKey(req);
        Optional<UserAddress> existing = repo.findByUserIdAndAddrKey(userId, key);
        String q = geocodingService.buildGeoQuery(req);

        if(existing.isPresent()) {
            UserAddress a = existing.get();
            a.setStatus(AddressStatus.ACTIVE);
            a.setPostalCode(req.postalCode);
            a.setAddressLine1(req.addressLine1);
            a.setAddressLine2(req.addressLine2);
            a.setRecipientName(req.recipientName);
            a.setRecipientPhone(req.recipientPhone);
            a.setAddressType(parseType(req.addressType));
            a.setDefault(Boolean.TRUE.equals(req.isDefault));
            geocodingService.geocode(q).ifPresent(ll -> {
                a.setLatitude(BigDecimal.valueOf(ll[0]).setScale(6, RoundingMode.HALF_UP));
                a.setLongitude(BigDecimal.valueOf(ll[1]).setScale(6, RoundingMode.HALF_UP));
            });
            if (Boolean.TRUE.equals(req.isDefault)) {
                repo.unsetDefaultForUser(userId);
                a.setDefault(true);
            }

            repo.save(a);
            return AddressDto.from(a, null, null);
        }
        UserAddress a = mapFrom(req);
        a.setUserId(userId);
        a.setStatus(AddressStatus.ACTIVE);
        if (Boolean.TRUE.equals(req.isDefault)) {
            repo.unsetDefaultForUser(userId);
            a.setDefault(true);
        }

        a = repo.save(a);

        return AddressDto.from(a, null, null);
    }

    @Transactional
    public AddressDto update(Long userId, Long addressId, AddressRequest req) {
        UserAddress a = repo.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("주소를 찾을 수 없습니다."));
        String q = geocodingService.buildGeoQuery(req);

        a.setPostalCode(trimToNull(req.postalCode));
        a.setAddressLine1(requireNonBlank(trimToNull(req.addressLine1), "기본 주소"));
        a.setAddressLine2(trimToNull(req.addressLine2));
        a.setAddressType(parseType(req.addressType));
        a.setRecipientName(trimToNull(req.recipientName));
        a.setRecipientPhone(digitsOrNull(req.recipientPhone));
        geocodingService.geocode(q).ifPresent(ll -> {
            a.setLatitude(BigDecimal.valueOf(ll[0]).setScale(6, RoundingMode.HALF_UP));
            a.setLongitude(BigDecimal.valueOf(ll[1]).setScale(6, RoundingMode.HALF_UP));
        });

        // update()
        boolean wantDefault = Boolean.TRUE.equals(req.isDefault);
        if (wantDefault) {
            repo.unsetDefaultForUser(userId);   // 1) 기존 대표 해제
            a.setDefault(true);                 // 2) 내가 대표
        } else {
            a.setDefault(false);
        }
        UserAddress saved = repo.save(a);

        return AddressDto.from(saved, null, null);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        UserAddress a = repo.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("주소를 찾을 수 없습니다."));
        a.setStatus(AddressStatus.DELETED);
        a.setDefault(false);
        repo.save(a);
    }

    @Transactional
    public void setDefault(Long userId, Long addressId) {
        UserAddress a = repo.findByAddressIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("주소를 찾을 수 없습니다."));
        a.setDefault(true);
        repo.save(a);
        repo.unsetDefaultForUserExcept(userId, a.getAddressId());
    }

    private AddressType parseType(String t) {
        try {
            return (t == null || t.isBlank()) ? AddressType.OTHER : AddressType.valueOf(t);
        } catch (Exception e) {
            return AddressType.OTHER;
        }
    }

    private String computeAddrKey(AddressRequest r) {
        String pc = norm(r.postalCode);
        String l1 = norm(r.addressLine1);
        String l2 = norm(r.addressLine2);
        String joined = String.join("|", pc, l1, l2);
        return md5(joined);
    }
    private String norm(String s){ return s==null? "": s.trim().toLowerCase(); }

    private static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest); // 소문자 32자
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UserAddress mapFrom(AddressRequest req) {
        if (req == null) throw new IllegalArgumentException("요청이 없습니다.");
        String q = geocodingService.buildGeoQuery(req);

        UserAddress a = new UserAddress();

        a.setRegionId(null); // 필요하면 province/city → region 매핑 로직에서 설정
        a.setPostalCode(trimToNull(req.postalCode));
        a.setAddressLine1(requireNonBlank(trimToNull(req.addressLine1), "기본 주소"));
        a.setAddressLine2(trimToNull(req.addressLine2));
        a.setAddressType(parseType(req.addressType));
        a.setRecipientName(trimToNull(req.recipientName));
        a.setRecipientPhone(digitsOrNull(req.recipientPhone)); // 숫자만 저장 권장
        a.setDefault(Boolean.TRUE.equals(req.isDefault));
        geocodingService.geocode(q).ifPresent(ll -> {
            a.setLatitude(BigDecimal.valueOf(ll[0]).setScale(6, RoundingMode.HALF_UP));
            a.setLongitude(BigDecimal.valueOf(ll[1]).setScale(6, RoundingMode.HALF_UP));
        });

        return a;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String requireNonBlank(String v, String fieldName) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(fieldName + "을(를) 입력해 주세요.");
        }
        return v;
    }

    private static String digitsOrNull(String s) {
        if (s == null) return null;
        String d = s.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }
}

