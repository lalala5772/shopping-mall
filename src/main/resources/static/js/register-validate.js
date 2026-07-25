(function () {
  // MemberDtos.RegisterRequest의 실제 서버 검증 규칙과 반드시 동일하게 맞춘다 - 클라이언트 검증이
  // 서버보다 느슨하거나 엄격하면 "통과했는데 서버에서 막힘/막혔는데 서버는 통과" 같은 혼란이 생긴다.
  var RULES = {
    password: {
      test: function (v) {
        return v.length >= 8 && v.length <= 50 && /^(?=.*[A-Za-z])(?=.*\d).+$/.test(v);
      },
      empty: '비밀번호를 입력해 주세요.',
      invalid: '영문과 숫자를 포함해 8자 이상 50자 이하로 입력해 주세요.'
    },
    name: {
      test: function (v) {
        return v.length >= 2 && v.length <= 20;
      },
      empty: '이름을 입력해 주세요.',
      invalid: '이름은 2자 이상 20자 이하로 입력해 주세요.'
    },
    phone: {
      test: function (v) {
        return v === '' || /^01[016789]-?\d{3,4}-?\d{4}$/.test(v);
      },
      empty: '',
      invalid: '올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)',
      optional: true
    }
  };

  var EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  function setHint(el, message, state) {
    // state: 'ok' | 'error' | null(비어있어 아직 판단 보류)
    el.textContent = message || '';
    el.classList.toggle('field-hint--error', state === 'error');
    el.classList.toggle('field-hint--ok', state === 'ok');
  }

  function markInput(input, state) {
    input.classList.toggle('input--invalid', state === 'error');
    input.classList.toggle('input--valid', state === 'ok');
  }

  function bindFieldValidation(inputId, hintId, rule) {
    var input = document.getElementById(inputId);
    var hint = document.getElementById(hintId);
    if (!input || !hint) return;

    function run(showEmptyAsError) {
      var v = input.value.trim();
      if (v === '') {
        if (rule.optional) {
          setHint(hint, '', null);
          markInput(input, null);
        } else if (showEmptyAsError) {
          setHint(hint, rule.empty, 'error');
          markInput(input, 'error');
        } else {
          setHint(hint, '', null);
          markInput(input, null);
        }
        return;
      }
      if (rule.test(v)) {
        setHint(hint, '', 'ok');
        markInput(input, 'ok');
      } else {
        setHint(hint, rule.invalid, 'error');
        markInput(input, 'error');
      }
    }

    input.addEventListener('input', function () { run(false); });
    input.addEventListener('blur', function () { run(true); });
  }

  function bindPasswordConfirm() {
    var pw = document.getElementById('password');
    var pwConfirm = document.getElementById('passwordConfirm');
    var hint = document.getElementById('passwordConfirm-hint');
    if (!pw || !pwConfirm || !hint) return;

    function run(showEmptyAsError) {
      var v = pwConfirm.value;
      if (v === '') {
        if (showEmptyAsError) {
          setHint(hint, '비밀번호 확인을 입력해 주세요.', 'error');
          markInput(pwConfirm, 'error');
        } else {
          setHint(hint, '', null);
          markInput(pwConfirm, null);
        }
        return;
      }
      if (v === pw.value) {
        setHint(hint, '', 'ok');
        markInput(pwConfirm, 'ok');
      } else {
        setHint(hint, '비밀번호가 일치하지 않습니다.', 'error');
        markInput(pwConfirm, 'error');
      }
    }

    pwConfirm.addEventListener('input', function () { run(false); });
    pwConfirm.addEventListener('blur', function () { run(true); });
    // 비밀번호 자체를 고친 뒤에도 확인란을 즉시 재검증한다.
    pw.addEventListener('input', function () {
      if (pwConfirm.value !== '') run(true);
    });
  }

  // ---------- 이메일: 로컬파트 + 도메인(select/직접입력 토글) 조합 ----------
  function bindEmail() {
    var local = document.getElementById('emailLocal');
    var domainSelect = document.getElementById('emailDomainSelect');
    var domainCustom = document.getElementById('emailDomainCustom');
    var hidden = document.getElementById('email');
    var hint = document.getElementById('email-hint');
    if (!local || !domainSelect || !domainCustom || !hidden || !hint) return;

    function currentDomain() {
      return domainSelect.value === '__custom__' ? domainCustom.value.trim() : domainSelect.value;
    }

    function sync(showEmptyAsError) {
      var domain = currentDomain();
      var value = local.value.trim() && domain ? local.value.trim() + '@' + domain : '';
      hidden.value = value;

      if (value === '') {
        if (showEmptyAsError) {
          setHint(hint, '이메일을 입력해 주세요.', 'error');
          markInput(local, 'error');
        } else {
          setHint(hint, '', null);
          markInput(local, null);
        }
        return;
      }
      if (EMAIL_RE.test(value)) {
        setHint(hint, '', 'ok');
        markInput(local, 'ok');
      } else {
        setHint(hint, '올바른 이메일 형식이 아닙니다.', 'error');
        markInput(local, 'error');
      }
    }

    function toggleCustomVisibility() {
      var isCustom = domainSelect.value === '__custom__';
      domainCustom.hidden = !isCustom;
      if (isCustom) {
        domainCustom.focus();
      }
    }

    domainSelect.addEventListener('change', function () {
      toggleCustomVisibility();
      sync(false);
    });
    local.addEventListener('input', function () { sync(false); });
    local.addEventListener('blur', function () { sync(true); });
    domainCustom.addEventListener('input', function () { sync(false); });
    domainCustom.addEventListener('blur', function () { sync(true); });

    // 유효성 검사 실패로 폼이 다시 렌더링된 경우, 서버가 들고 있던 email 값을 로컬파트/도메인으로 되돌려 채운다.
    var prevEmail = hidden.getAttribute('data-prev-value') || '';
    if (prevEmail && prevEmail.indexOf('@') > -1) {
      var parts = prevEmail.split('@');
      local.value = parts[0];
      var domain = parts[1];
      var matched = false;
      for (var i = 0; i < domainSelect.options.length; i++) {
        if (domainSelect.options[i].value === domain) {
          domainSelect.value = domain;
          matched = true;
          break;
        }
      }
      if (!matched) {
        domainSelect.value = '__custom__';
        domainCustom.value = domain;
      }
      toggleCustomVisibility();
      sync(false);
    }
  }

  function bindTermsAgree() {
    var agree = document.getElementById('agreeTerms');
    var hint = document.getElementById('agreeTerms-hint');
    var submitBtn = document.getElementById('registerSubmit');
    if (!agree || !submitBtn) return;
    agree.addEventListener('change', function () {
      if (hint) setHint(hint, agree.checked ? '' : '', null);
    });
  }

  document.addEventListener('DOMContentLoaded', function () {
    bindEmail();
    bindFieldValidation('password', 'password-hint', RULES.password);
    bindPasswordConfirm();
    bindFieldValidation('name', 'name-hint', RULES.name);
    bindFieldValidation('phone', 'phone-hint', RULES.phone);
    bindTermsAgree();
  });
})();
