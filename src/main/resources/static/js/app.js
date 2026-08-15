(function () {
  const chipList = document.getElementById('chipList');
  const customInterestInput = document.getElementById('customInterest');
  const addInterestBtn = document.getElementById('addInterestBtn');
  const startBtn = document.getElementById('startBtn');
  const modeVideoBtn = document.getElementById('modeVideoBtn');
  const modeTextBtn = document.getElementById('modeTextBtn');

  const ageGateModal = document.getElementById('ageGateModal');
  const acceptAgeBtn = document.getElementById('acceptAge');
  const declineAgeBtn = document.getElementById('declineAge');
  const termsModal = document.getElementById('termsModal');
  const openTerms = document.getElementById('openTerms');
  const openTermsInline = document.getElementById('openTermsInline');
  const closeTerms = document.getElementById('closeTerms');

  const selected = new Set();
  let videoEnabled = true;

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

  // ---------- chat mode (video & text vs. text only) ----------
  function setMode(enableVideo) {
    videoEnabled = enableVideo;
    modeVideoBtn.classList.toggle('selected', enableVideo);
    modeVideoBtn.setAttribute('aria-checked', String(enableVideo));
    modeTextBtn.classList.toggle('selected', !enableVideo);
    modeTextBtn.setAttribute('aria-checked', String(!enableVideo));
  }
  modeVideoBtn.addEventListener('click', () => setMode(true));
  modeTextBtn.addEventListener('click', () => setMode(false));

  startBtn.addEventListener('click', () => {
    sessionStorage.setItem('quikko_interests', JSON.stringify(Array.from(selected)));
    sessionStorage.setItem('quikko_video_enabled', videoEnabled ? '1' : '0');
    if (sessionStorage.getItem('quikko_age_confirmed') === '1') {
      goToChat();
    } else {
      showModal(ageGateModal);
    }
  });

  function goToChat() {
    window.location.href = '/chat';
  }
})();
