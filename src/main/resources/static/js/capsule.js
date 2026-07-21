(function () {
  const tokenInput = document.getElementById('tokenInput');
  const claimBtn = document.getElementById('claimBtn');
  const claimResult = document.getElementById('claimResult');

  const params = new URLSearchParams(window.location.search);
  const prefilled = params.get('token');
  if (prefilled) tokenInput.value = prefilled;

  function render(status, html) {
    claimResult.hidden = false;
    claimResult.className = 'claim-result ' + status.toLowerCase();
    claimResult.innerHTML = html;
  }

  function claim() {
    const token = tokenInput.value.trim();
    if (!token) return;
    claimBtn.disabled = true;
    claimBtn.textContent = 'Checking…';

    fetch('/api/capsules/claim?token=' + encodeURIComponent(token))
      .then((r) => r.json())
      .then((data) => {
        switch (data.status) {
          case 'UNLOCKED':
            render('unlocked', '<strong>🎉 It unlocked!</strong><p>' + escapeHtml(data.message) + '</p>');
            break;
          case 'PENDING':
            render('pending', '<strong>⏳ Not yet.</strong><p>Your stranger hasn\'t left a capsule for you yet, ' +
              'or the 7-day window hasn\'t passed. Come back later' +
              (data.unlockAt ? ' (unlocks ' + new Date(data.unlockAt).toLocaleString() + ')' : '') + '.</p>');
            break;
          case 'EXPIRED':
            render('expired', '<strong>💨 This one\'s gone.</strong><p>Only one side left a capsule, so it never unlocked.</p>');
            break;
          default:
            render('notfound', '<strong>🔍 Not found.</strong><p>Double-check your token and try again.</p>');
        }
      })
      .catch(() => render('notfound', '<strong>Something went wrong.</strong><p>Please try again.</p>'))
      .finally(() => {
        claimBtn.disabled = false;
        claimBtn.textContent = 'Check';
      });
  }

  function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
  }

  claimBtn.addEventListener('click', claim);
  tokenInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') claim();
  });
  if (prefilled) claim();
})();
