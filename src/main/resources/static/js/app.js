(function () {
  const chipList = document.getElementById('chipList');
  const customInterestInput = document.getElementById('customInterest');
  const addInterestBtn = document.getElementById('addInterestBtn');
  const startBtn = document.getElementById('startBtn');
  const videoToggle = document.getElementById('videoToggle');

  const ageGateModal = document.getElementById('ageGateModal');
  const acceptAgeBtn = document.getElementById('acceptAge');
  const declineAgeBtn = document.getElementById('declineAge');
  const termsModal = document.getElementById('termsModal');
  const openTerms = document.getElementById('openTerms');
  const openTermsInline = document.getElementById('openTermsInline');
  const closeTerms = document.getElementById('closeTerms');

  const captchaRow = document.querySelector('.captcha-row');
  const captchaQuestion = document.getElementById('captchaQuestion');
  const captchaAnswer = document.getElementById('captchaAnswer');
  const captchaRefresh = document.getElementById('captchaRefresh');
  const captchaStatus = document.getElementById('captchaStatus');

  const selected = new Set();
  let captchaEnabled = true;
  let captchaChallengeId = null;

  function anonId() {
    let id = sessionStorage.getItem('quikko_anon_id');
    if (!id) {
      id = (crypto.randomUUID ? crypto.randomUUID() : ('id-' + Math.random().toString(36).slice(2) + Date.now()));
      sessionStorage.setItem('quikko_anon_id', id);
    }
    return id;
  }
  anonId();

  function renderChip(tag) {
    const chip = document.createElement('button');
    chip.type = 'button';
    chip.className = 'chip';
    chip.textContent = tag;
    chip.dataset.tag = tag;
    chip.addEventListener('click', () => {
      if (selected.has(tag)) {
        selected.delete(tag);
        chip.classList.remove('selected');
      } else {
        selected.add(tag);
        chip.classList.add('selected');
      }
    });
    chipList.appendChild(chip);
  }

  fetch('/api/interests')
    .then((r) => r.json())
    .then((data) => (data.suggested || []).forEach(renderChip))
    .catch(() => {
      ['Music', 'Movies', 'Gaming', 'Sports', 'Books', 'Travel'].forEach(renderChip);
    });

  function addCustomInterest() {
    const val = customInterestInput.value.trim();
    if (!val) return;
    if (!selected.has(val)) {
      selected.add(val);
      renderChip(val);
      const chips = chipList.querySelectorAll('.chip');
      chips[chips.length - 1].classList.add('selected');
    }
    customInterestInput.value = '';
  }

  addInterestBtn.addEventListener('click', addCustomInterest);
  customInterestInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addCustomInterest();
    }
  });

  function showModal(el) { el.hidden = false; }
  function hideModal(el) { el.hidden = true; }

  openTerms.addEventListener('click', (e) => { e.preventDefault(); showModal(termsModal); });
  openTermsInline.addEventListener('click', (e) => { e.preventDefault(); showModal(termsModal); });
  closeTerms.addEventListener('click', () => hideModal(termsModal));

  declineAgeBtn.addEventListener('click', () => {
    window.location.href = 'https://www.google.com';
  });

  acceptAgeBtn.addEventListener('click', () => {
    sessionStorage.setItem('quikko_age_confirmed', '1');
    hideModal(ageGateModal);
    goToChat();
  });

  // ---------- bot-check (CAPTCHA) ----------
  function loadNewCaptcha() {
    captchaAnswer.value = '';
    captchaStatus.hidden = true;
    captchaQuestion.textContent = 'Loading check…';
    fetch('/api/captcha/new')
      .then((r) => r.json())
      .then((data) => {
        captchaEnabled = data.enabled !== false;
        if (!captchaEnabled) {
          captchaRow.hidden = true;
          return;
        }
        captchaRow.hidden = false;
        captchaChallengeId = data.challengeId;
        captchaQuestion.textContent = data.question;
      })
      .catch(() => {
        captchaQuestion.textContent = 'Verification unavailable — try again shortly.';
      });
  }
  loadNewCaptcha();
  captchaRefresh.addEventListener('click', loadNewCaptcha);

  function showCaptchaError(msg) {
    captchaStatus.textContent = msg;
    captchaStatus.hidden = false;
    captchaRow.classList.remove('shake');
    // restart the CSS animation
    void captchaRow.offsetWidth;
    captchaRow.classList.add('shake');
  }

  function verifyCaptcha() {
    if (!captchaEnabled) {
      return Promise.resolve(true);
    }
    return fetch('/api/captcha/verify', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ challengeId: captchaChallengeId, answer: captchaAnswer.value }),
    })
      .then((r) => r.json())
      .then((data) => {
        if (data.valid) {
          sessionStorage.setItem('quikko_captcha_id', captchaChallengeId);
          return true;
        }
        showCaptchaError("That's not quite right — try the new question.");
        loadNewCaptcha();
        return false;
      })
      .catch(() => {
        showCaptchaError("Couldn't verify right now — please try again.");
        return false;
      });
  }

  startBtn.addEventListener('click', () => {
    startBtn.disabled = true;
    const originalLabel = startBtn.textContent;
    startBtn.textContent = 'Checking…';

    verifyCaptcha().then((ok) => {
      startBtn.disabled = false;
      startBtn.textContent = originalLabel;
      if (!ok) return;

      sessionStorage.setItem('quikko_interests', JSON.stringify(Array.from(selected)));
      sessionStorage.setItem('quikko_video_enabled', videoToggle.checked ? '1' : '0');
      if (sessionStorage.getItem('quikko_age_confirmed') === '1') {
        goToChat();
      } else {
        showModal(ageGateModal);
      }
    });
  });

  function goToChat() {
    window.location.href = '/chat';
  }
})();
