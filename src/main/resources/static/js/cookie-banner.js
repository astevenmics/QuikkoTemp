(function () {
    var ACK_KEY = 'quikko_cookie_ack';

    var ack;
    try {
        ack = localStorage.getItem(ACK_KEY);
    } catch (e) {
        ack = '1'; // storage unavailable (private mode, etc.) -- don't nag if we can't remember the dismissal anyway
    }
    if (ack === '1') {
        return;
    }

    var banner = document.createElement('div');
    banner.className = 'cookie-banner';
    banner.setAttribute('role', 'region');
    banner.setAttribute('aria-label', 'Cookie notice');

    var text = document.createElement('p');
    text.textContent = 'Quikko only uses strictly-necessary browser storage to run your chat session — no tracking cookies, and no ads or analytics are active today. ';
    var link = document.createElement('a');
    link.href = '/cookies';
    link.textContent = 'Cookie Policy';
    text.appendChild(link);

    var dismissBtn = document.createElement('button');
    dismissBtn.type = 'button';
    dismissBtn.className = 'btn btn-primary';
    dismissBtn.textContent = 'Got it';
    dismissBtn.addEventListener('click', function () {
        try {
            localStorage.setItem(ACK_KEY, '1');
        } catch (e) {
            // ignore -- worst case the banner reappears next visit
        }
        banner.remove();
    });

    banner.appendChild(text);
    banner.appendChild(dismissBtn);
    document.body.appendChild(banner);
})();
