(function () {
  // 배송중/배송완료로 바꿀 때만 택배사·운송장번호 입력이 의미가 있다 - 그 외 상태에서는
  // 접어서 목록이 필요 이상으로 길어지지 않게 한다. 값은 그대로 폼에 남아있으니 감춰도 제출된다.
  function shouldShowTracking(status) {
    return status === 'SHIPPING' || status === 'DELIVERED';
  }

  function bind(form) {
    var select = form.querySelector('.order-status-next');
    var tracking = form.querySelector('.order-status-form__tracking');
    if (!select || !tracking) return;

    function sync() {
      tracking.hidden = !shouldShowTracking(select.value);
    }

    select.addEventListener('change', sync);
    sync();
  }

  document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.order-status-form').forEach(bind);
  });
})();
