package com.mondaycloset.shop.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 도로명주소 API(juso.go.kr) 팝업의 콜백 수신처.
 * 사용자가 팝업에서 주소를 선택하면 juso.go.kr이 이 경로로 "POST"를 보낸다(정적 리소스로는 받을 수 없다).
 * GET도 함께 열어 두는 이유: 연동 방식에 따라 GET으로 리다이렉트하는 경우도 있어 방어적으로 둘 다 받는다.
 */
@Controller
public class AddressCallbackController {

    @RequestMapping("/address-callback.html")
    public String callback(@RequestParam(required = false) String roadAddrPart1,
                            @RequestParam(required = false) String roadAddrPart2,
                            @RequestParam(required = false) String roadAddr,
                            @RequestParam(required = false) String zipNo,
                            // 팝업 자체의 "상세주소입력" 칸에 사용자가 입력한 값 (juso.go.kr 쪽 UI, 우리 화면과 별개)
                            @RequestParam(required = false) String addrDetail,
                            Model model) {
        String fullRoadAddr = roadAddr;
        if (fullRoadAddr == null || fullRoadAddr.isBlank()) {
            fullRoadAddr = String.join(" ",
                    roadAddrPart1 == null ? "" : roadAddrPart1,
                    roadAddrPart2 == null ? "" : roadAddrPart2).trim();
        }
        model.addAttribute("roadAddr", fullRoadAddr);
        model.addAttribute("zipNo", zipNo == null ? "" : zipNo);
        model.addAttribute("detailAddr", addrDetail == null ? "" : addrDetail);
        return "address/callback";
    }
}
