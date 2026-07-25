package com.mondaycloset.shop.domain.order;

/** 스마트택배(Sweettracker) t_code 기준 주요 택배사 코드. 관리자 배송정보 입력 select와 화면 표시에 함께 쓴다. */
public enum Carrier {
    POST("01", "우체국택배"),
    CJ("04", "CJ대한통운"),
    HANJIN("05", "한진택배"),
    LOGEN("06", "로젠택배"),
    LOTTE("08", "롯데택배");

    private final String code;
    private final String label;

    Carrier(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(String code) {
        if (code == null) {
            return null;
        }
        for (Carrier carrier : values()) {
            if (carrier.code.equals(code)) {
                return carrier.label;
            }
        }
        return code;
    }
}
