(function () {
  var MAX_BYTES = 8 * 1024 * 1024; // nginx/서버 상한(12MB/10MB)보다 낮게 잡아 클라이언트에서 먼저 빠르게 알려준다

  var form = document.getElementById('imageSearchForm');
  if (!form) {
    return;
  }
  var input = document.getElementById('imageSearchInput');
  var trigger = document.getElementById('imageSearchTrigger');
  var preview = document.getElementById('imageSearchPreview');
  var thumb = document.getElementById('imageSearchThumb');
  var filenameEl = document.getElementById('imageSearchFilename');
  var clearBtn = document.getElementById('imageSearchClear');
  var errorEl = document.getElementById('imageSearchError');

  function showError(message) {
    errorEl.textContent = message;
    errorEl.hidden = false;
  }

  function clearSelection() {
    input.value = '';
    preview.hidden = true;
    trigger.hidden = false;
    errorEl.hidden = true;
  }

  trigger.addEventListener('click', function () {
    errorEl.hidden = true;
    input.click();
  });

  clearBtn.addEventListener('click', clearSelection);

  input.addEventListener('change', function () {
    var file = input.files[0];
    if (!file) {
      return;
    }
    if (file.size > MAX_BYTES) {
      showError('이미지 용량이 너무 큽니다 (최대 8MB). 다른 이미지를 선택해 주세요.');
      input.value = '';
      return;
    }
    errorEl.hidden = true;
    var reader = new FileReader();
    reader.onload = function (e) {
      thumb.src = e.target.result;
      filenameEl.textContent = file.name.length > 16 ? file.name.slice(0, 14) + '…' : file.name;
      preview.hidden = false;
      trigger.hidden = true;
      form.requestSubmit();
    };
    reader.readAsDataURL(file);
  });

  form.addEventListener('submit', function () {
    var overlay = document.createElement('div');
    overlay.className = 'image-search-overlay';
    overlay.innerHTML = '<div class="image-search-spinner"></div><p>이미지에서 유사한 상품을 찾는 중…</p>';
    document.body.appendChild(overlay);
  });
})();
