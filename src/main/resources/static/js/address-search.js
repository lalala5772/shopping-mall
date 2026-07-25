/**
 * 도로명주소 API(행정안전부, business.juso.go.kr) 팝업 검색 연동.
 * 승인키(confmKey)를 받아 검색 팝업을 열고, 팝업이 /address-callback.html 로 이동하면서
 * postMessage로 넘겨주는 선택 결과를 받아 지정된 입력 필드들을 채운다.
 *
 * 사용법 (HTML):
 *   <input id="zipcode" readonly>
 *   <input id="roadAddr" readonly>
 *   <input id="detailAddr">
 *   <input type="hidden" id="address">  <!-- 실제 폼이 서버로 보내는 필드 -->
 *   <button type="button" onclick="AddressSearch.open()">주소 검색</button>
 *   <script>
 *     AddressSearch.init({
 *       confirmKey: /*[[${jusoConfirmKey}]]* /'',
 *       zipcodeInputId: 'zipcode',
 *       roadAddrInputId: 'roadAddr',
 *       detailAddrInputId: 'detailAddr',
 *       targetInputId: 'address'
 *     });
 *   </script>
 */
window.AddressSearch = (function () {
  var config = null;

  function syncTarget() {
    if (!config) return;
    var road = document.getElementById(config.roadAddrInputId);
    var detail = document.getElementById(config.detailAddrInputId);
    var target = document.getElementById(config.targetInputId);
    if (!target) return;
    var roadValue = road ? road.value : '';
    var detailValue = detail ? detail.value : '';
    target.value = detailValue ? (roadValue + ' ' + detailValue) : roadValue;
  }

  function onMessage(event) {
    if (!event.data || event.data.source !== 'juso-address-search' || !config) {
      return;
    }
    var zipcodeEl = document.getElementById(config.zipcodeInputId);
    var roadEl = document.getElementById(config.roadAddrInputId);
    var detailEl = document.getElementById(config.detailAddrInputId);
    if (zipcodeEl) zipcodeEl.value = event.data.zipNo || '';
    if (roadEl) roadEl.value = event.data.roadAddr || '';
    // juso 팝업 자체의 "상세주소입력" 칸에 이미 입력한 값이 있으면 그대로 이어받는다(다시 입력할 필요 없게).
    if (detailEl && event.data.detailAddr) {
      detailEl.value = event.data.detailAddr;
    }
    syncTarget();
    if (detailEl) detailEl.focus();
  }

  function init(options) {
    config = options;
    window.addEventListener('message', onMessage);
    var detailEl = document.getElementById(config.detailAddrInputId);
    if (detailEl) {
      detailEl.addEventListener('input', syncTarget);
    }
    syncTarget();
  }

  function open() {
    if (!config) return;
    if (!config.confirmKey) {
      alert('주소 검색 API 키가 아직 설정되지 않았습니다. 관리자에게 문의해 주세요.');
      return;
    }
    var form = document.createElement('form');
    form.method = 'POST';
    form.action = 'https://business.juso.go.kr/addrlink/addrLinkUrl.do';
    form.target = 'jusoAddrPopup';

    function addHidden(name, value) {
      var input = document.createElement('input');
      input.type = 'hidden';
      input.name = name;
      input.value = value;
      form.appendChild(input);
    }
    addHidden('confmKey', config.confirmKey);
    addHidden('returnUrl', window.location.origin + '/address-callback.html');
    addHidden('resultType', '4');

    document.body.appendChild(form);
    window.open('', 'jusoAddrPopup', 'width=570,height=420,scrollbars=yes');
    form.submit();
    document.body.removeChild(form);
  }

  return { init: init, open: open };
})();
