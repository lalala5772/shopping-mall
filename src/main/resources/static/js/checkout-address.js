(function () {
  document.addEventListener('DOMContentLoaded', function () {
    var checkbox = document.getElementById('useSavedAddress');
    if (!checkbox) return;

    var nameInput = document.getElementById('receiverName');
    var phoneInput = document.getElementById('receiverPhone');
    var roadAddrInput = document.getElementById('roadAddr');
    var detailAddrInput = document.getElementById('detailAddr');
    var addressHidden = document.getElementById('receiverAddress');
    var searchBtn = document.getElementById('addressSearchBtn');
    if (!nameInput || !phoneInput || !roadAddrInput || !addressHidden) return;

    var saved = {
      name: checkbox.getAttribute('data-name') || '',
      phone: checkbox.getAttribute('data-phone') || '',
      address: checkbox.getAttribute('data-address') || ''
    };

    checkbox.addEventListener('change', function () {
      if (checkbox.checked) {
        nameInput.value = saved.name;
        phoneInput.value = saved.phone;
        roadAddrInput.value = saved.address;
        addressHidden.value = saved.address;
      } else {
        nameInput.value = '';
        phoneInput.value = '';
        roadAddrInput.value = '';
        addressHidden.value = '';
      }
      if (detailAddrInput) detailAddrInput.value = '';
    });

    // 저장된 주소가 아닌 새 주소를 검색하려는 것이므로, 체크 상태가 실제 입력값과
    // 어긋나지 않도록 검색 버튼을 누르는 순간 체크를 풀어준다.
    if (searchBtn) {
      searchBtn.addEventListener('click', function () {
        checkbox.checked = false;
      });
    }
  });
})();
