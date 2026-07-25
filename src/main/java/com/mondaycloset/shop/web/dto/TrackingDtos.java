package com.mondaycloset.shop.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 스마트택배(Sweettracker) 배송조회 API 응답 매핑. 필요한 필드만 취하고 나머지는 무시한다. */
public class TrackingDtos {

    private TrackingDtos() {}

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingResponse {
        private boolean status;
        @JsonProperty("completeYN")
        private String completeYn;
        private String level;
        private String msg;
        private List<TrackingDetail> trackingDetails;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TrackingDetail {
        private String time;
        private String where;
        private String kind;
    }
}
