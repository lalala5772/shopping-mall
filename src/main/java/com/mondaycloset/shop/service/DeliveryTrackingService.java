package com.mondaycloset.shop.service;

import com.mondaycloset.shop.web.dto.TrackingDtos.TrackingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 스마트택배(Sweettracker) 배송조회 API를 서버에서 대신 호출한다(API 키를 클라이언트에 노출하지 않기 위한 프록시).
 * 키가 설정되지 않았거나 API 호출이 실패해도 주문 조회 자체는 계속 가능해야 하므로 예외를 던지지 않고 null을 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingService {

    private static final String TRACKING_URL = "http://info.sweettracker.co.kr/api/v1/trackingInfo";

    private final RestTemplate restTemplate;

    @Value("${app.delivery-tracking.api-key}")
    private String apiKey;

    public TrackingResponse track(String carrierCode, String trackingNumber) {
        if (apiKey == null || apiKey.isBlank() || carrierCode == null || trackingNumber == null) {
            return null;
        }
        String url = UriComponentsBuilder.fromHttpUrl(TRACKING_URL)
                .queryParam("t_key", apiKey)
                .queryParam("t_code", carrierCode)
                .queryParam("t_invoice", trackingNumber)
                .toUriString();
        try {
            return restTemplate.getForObject(url, TrackingResponse.class);
        } catch (RestClientException e) {
            log.warn("[DeliveryTracking] 조회 실패 carrierCode={}, trackingNumber={}, error={}",
                    carrierCode, trackingNumber, e.getMessage());
            return null;
        }
    }
}
