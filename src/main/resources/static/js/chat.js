(function () {
  const anonId = sessionStorage.getItem('quikko_anon_id');
  if (!anonId) {
    window.location.href = '/';
    return;
  }
  const myInterests = JSON.parse(sessionStorage.getItem('quikko_interests') || '[]');
  const videoEnabled = sessionStorage.getItem('quikko_video_enabled') !== '0';
  const captchaId = sessionStorage.getItem('quikko_captcha_id') || null;

  const SEARCHING_TEXT = 'Searching for another match…';

  const statusPill = document.getElementById('statusPill');
  const icebreakerBanner = document.getElementById('icebreakerBanner');
  const icebreakerText = document.getElementById('icebreakerText');
  const remoteVideo = document.getElementById('remoteVideo');
  const localVideo = document.getElementById('localVideo');
  const remoteOverlay = document.getElementById('remoteOverlay');
  const chatLog = document.getElementById('chatLog');
  const chatForm = document.getElementById('chatForm');
  const chatInput = document.getElementById('chatInput');
  const skipBtn = document.getElementById('skipBtn');
  const stopBtn = document.getElementById('stopBtn');
  const reportBtn = document.getElementById('reportBtn');
  const camToggle = document.getElementById('camToggle');
  const micToggle = document.getElementById('micToggle');

  const capsuleModal = document.getElementById('capsuleModal');
  const capsuleText = document.getElementById('capsuleText');
  const capsuleCharCount = document.getElementById('capsuleCharCount');
  const capsuleTokenBox = document.getElementById('capsuleTokenBox');
  const capsuleTokenEl = document.getElementById('capsuleToken');
  const copyTokenBtn = document.getElementById('copyTokenBtn');
  const skipCapsuleBtn = document.getElementById('skipCapsuleBtn');
  const sendCapsuleBtn = document.getElementById('sendCapsuleBtn');

  let stompClient = null;
  let iceServers = [{ urls: ['stun:stun.l.google.com:19302'] }];
  let localStream = null;
  let pc = null;
  let pendingCandidates = [];
  let currentPairId = null;
  let isInitiator = false;
  let capsulePairIdForModal = null;
  let capsuleResolve = null;

  // Text-only users never touch the camera/mic/video-pane at all — this is
  // effectively a different, chat-only experience, not a video call with
  // video hidden.
  if (!videoEnabled) {
    document.body.classList.add('text-only');
  }

  function setStatus(kind, text) {
    statusPill.textContent = text;
    statusPill.className = 'status-pill status-' + kind;
  }

  function appendMessage(text, kind) {
    const div = document.createElement('div');
    div.className = 'msg ' + kind;
    div.textContent = text;
    chatLog.appendChild(div);
    chatLog.scrollTop = chatLog.scrollHeight;
  }

  function resetChatLog() {
    chatLog.innerHTML = '';
  }

  function setSearchingOverlay() {
    if (!videoEnabled) return;
    remoteOverlay.hidden = false;
    remoteOverlay.classList.add('searching');
    remoteOverlay.innerHTML = '<span class="radar"><span class="radar-ring"></span><span class="radar-ring"></span><span class="radar-dot"></span></span>' +
      '<span class="searching-label">' + SEARCHING_TEXT + '</span>';
  }

  // ---------- media ----------
  async function ensureLocalMedia() {
    if (localStream) return localStream;
    try {
      localStream = await navigator.mediaDevices.getUserMedia({ video: videoEnabled, audio: true });
      localVideo.srcObject = localStream;
    } catch (err) {
      console.warn('Could not get local media', err);
      appendMessage('Camera/mic not available — you can still text chat.', 'system');
      localStream = new MediaStream();
    }
    return localStream;
  }

  camToggle.addEventListener('change', () => {
    if (!localStream) return;
    localStream.getVideoTracks().forEach((t) => (t.enabled = camToggle.checked));
  });
  micToggle.addEventListener('change', () => {
    if (!localStream) return;
    localStream.getAudioTracks().forEach((t) => (t.enabled = micToggle.checked));
  });

  // ---------- WebRTC ----------
  function teardownPeerConnection() {
    if (pc) {
      pc.close();
      pc = null;
    }
    pendingCandidates = [];
    if (videoEnabled) {
      remoteVideo.srcObject = null;
      remoteOverlay.classList.remove('searching');
      remoteOverlay.textContent = SEARCHING_TEXT;
      remoteOverlay.hidden = false;
    }
  }

  async function setupPeerConnection() {
    teardownPeerConnection();
    pc = new RTCPeerConnection({ iceServers });

    const stream = await ensureLocalMedia();
    stream.getTracks().forEach((track) => pc.addTrack(track, stream));

    pc.ontrack = (event) => {
      remoteVideo.srcObject = event.streams[0];
      remoteOverlay.classList.remove('searching');
      remoteOverlay.hidden = true;
    };

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        sendSignal('ice-candidate', event.candidate);
      }
    };

    pc.onconnectionstatechange = () => {
      if (pc && (pc.connectionState === 'failed' || pc.connectionState === 'disconnected')) {
        remoteOverlay.classList.remove('searching');
        remoteOverlay.hidden = false;
        remoteOverlay.textContent = 'Connection lost…';
      }
    };

    if (isInitiator) {
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      sendSignal('offer', pc.localDescription);
    }
  }

  async function handleSignal(signalType, payload) {
    if (!pc) return;
    if (signalType === 'offer') {
      await pc.setRemoteDescription(new RTCSessionDescription(payload));
      await flushPendingCandidates();
      const answer = await pc.createAnswer();
      await pc.setLocalDescription(answer);
      sendSignal('answer', pc.localDescription);
    } else if (signalType === 'answer') {
      await pc.setRemoteDescription(new RTCSessionDescription(payload));
      await flushPendingCandidates();
    } else if (signalType === 'ice-candidate') {
      if (pc.remoteDescription && pc.remoteDescription.type) {
        try { await pc.addIceCandidate(new RTCIceCandidate(payload)); } catch (e) { console.warn(e); }
      } else {
        pendingCandidates.push(payload);
      }
    }
  }

  async function flushPendingCandidates() {
    const queued = pendingCandidates;
    pendingCandidates = [];
    for (const c of queued) {
      try { await pc.addIceCandidate(new RTCIceCandidate(c)); } catch (e) { console.warn(e); }
    }
  }

  function sendSignal(signalType, payload) {
    send('/app/signal', { anonId, pairId: currentPairId, signalType, payload });
  }

  // ---------- STOMP ----------
  function send(destination, body) {
    if (stompClient && stompClient.connected) {
      stompClient.send(destination, {}, JSON.stringify(body));
    }
  }

  function joinQueue() {
    setStatus('waiting', SEARCHING_TEXT);
    setSearchingOverlay();
    icebreakerBanner.hidden = true;
    send('/app/queue.join', { anonId, interests: myInterests, videoEnabled, captchaId });
  }

  function connect() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect({}, () => {
      stompClient.subscribe('/topic/session/' + anonId, (frame) => {
        onServerEvent(JSON.parse(frame.body));
      });
      joinQueue();
    }, () => {
      setStatus('ended', 'Disconnected — reload to retry');
    });
  }

  function onServerEvent(event) {
    switch (event.type) {
      case 'WAITING':
        setStatus('waiting', SEARCHING_TEXT);
        break;
      case 'MATCHED':
        onMatched(event);
        break;
      case 'CHAT':
        appendMessage(event.text, 'them');
        break;
      case 'SIGNAL':
        handleSignal(event.signalType, event.payload);
        break;
      case 'PARTNER_SKIPPED':
        onMatchEndedByOther('The stranger skipped to someone new.');
        break;
      case 'PARTNER_LEFT':
        onMatchEndedByOther('The stranger left the chat.');
        break;
      case 'REPORT_ACK':
        appendMessage('Report submitted. Thanks for keeping Quikko safe.', 'system');
        break;
      case 'CAPSULE_SAVED':
        onCapsuleSaved(event.token);
        break;
      case 'BANNED':
        setStatus('ended', 'Blocked');
        appendMessage(event.message || 'You are temporarily blocked.', 'system');
        break;
      case 'RATE_LIMITED':
        appendMessage(event.message || 'Slow down a little.', 'system');
        break;
      case 'CAPTCHA_FAILED':
        setStatus('ended', 'Verification needed');
        appendMessage(event.message || 'Please verify you\'re human again.', 'system');
        sessionStorage.removeItem('quikko_captcha_id');
        setTimeout(() => (window.location.href = '/'), 2200);
        break;
      case 'ERROR':
        appendMessage(event.message || 'Something went wrong.', 'system');
        break;
    }
  }

  function onMatched(event) {
    resetSkipButton();
    currentPairId = event.pairId;
    isInitiator = !!event.initiator;
    setStatus('matched', 'Connected');
    resetChatLog();
    appendMessage("You're connected with a stranger. Say hi!", 'system');
    if (event.icebreaker) {
      icebreakerText.textContent = event.icebreaker;
      icebreakerBanner.hidden = false;
    } else {
      icebreakerBanner.hidden = true;
    }
    if (videoEnabled) {
      if (event.partnerVideoEnabled === false) {
        remoteOverlay.classList.remove('searching');
        remoteOverlay.hidden = false;
        remoteOverlay.textContent = '💬 This stranger is chatting via text only';
      } else {
        setupPeerConnection();
      }
    }
  }

  function onMatchEndedByOther(message) {
    if (!currentPairId) return;
    resetSkipButton();
    appendMessage(message, 'system');
    const pairId = currentPairId;
    currentPairId = null;
    teardownPeerConnection();
    setStatus('waiting', SEARCHING_TEXT);
    promptCapsule(pairId).then(() => joinQueue());
  }

  // ---------- buttons ----------
  let skipConfirmTimeout = null;

  function resetSkipButton() {
    clearTimeout(skipConfirmTimeout);
    skipConfirmTimeout = null;
    skipBtn.classList.remove('btn-confirm');
    skipBtn.textContent = '⏭ Skip';
  }

  function doSkip() {
    const pairId = currentPairId;
    send('/app/match.skip', { anonId, pairId });
    currentPairId = null;
    teardownPeerConnection();
    appendMessage('You skipped. Finding someone new…', 'system');
    promptCapsule(pairId).then(() => joinQueue());
  }

  skipBtn.addEventListener('click', () => {
    if (!currentPairId) return;
    if (skipConfirmTimeout) {
      resetSkipButton();
      doSkip();
      return;
    }
    skipBtn.classList.add('btn-confirm');
    skipBtn.textContent = 'Sure? Click again';
    skipConfirmTimeout = setTimeout(resetSkipButton, 3000);
  });

  stopBtn.addEventListener('click', () => {
    resetSkipButton();
    const pairId = currentPairId;
    send('/app/queue.leave', { anonId, pairId });
    currentPairId = null;
    teardownPeerConnection();
    const after = pairId ? promptCapsule(pairId) : Promise.resolve();
    after.then(() => (window.location.href = '/'));
  });

  reportBtn.addEventListener('click', () => {
    if (!currentPairId) return;
    resetSkipButton();
    const pairId = currentPairId;
    send('/app/report', { anonId, pairId });
    currentPairId = null;
    teardownPeerConnection();
    appendMessage('Reported. Finding someone new…', 'system');
    joinQueue();
  });

  chatForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const text = chatInput.value.trim();
    if (!text || !currentPairId) return;
    send('/app/chat.send', { anonId, pairId: currentPairId, text });
    appendMessage(text, 'me');
    chatInput.value = '';
  });

  // ---------- Time Capsule modal ----------
  capsuleText.addEventListener('input', () => {
    capsuleCharCount.textContent = String(capsuleText.value.length);
  });

  function promptCapsule(pairId) {
    return new Promise((resolve) => {
      capsulePairIdForModal = pairId;
      capsuleText.value = '';
      capsuleCharCount.textContent = '0';
      capsuleTokenBox.hidden = true;
      sendCapsuleBtn.hidden = false;
      sendCapsuleBtn.disabled = false;
      sendCapsuleBtn.textContent = 'Leave capsule & continue';
      skipCapsuleBtn.hidden = false;
      capsuleModal.hidden = false;
      capsuleResolve = resolve;
    });
  }

  function closeCapsuleModal() {
    capsuleModal.hidden = true;
    const r = capsuleResolve;
    capsuleResolve = null;
    capsulePairIdForModal = null;
    if (r) r();
  }

  skipCapsuleBtn.addEventListener('click', closeCapsuleModal);

  sendCapsuleBtn.addEventListener('click', () => {
    const message = capsuleText.value.trim();
    if (!message || !capsulePairIdForModal) {
      closeCapsuleModal();
      return;
    }
    sendCapsuleBtn.disabled = true;
    sendCapsuleBtn.textContent = 'Saving…';
    send('/app/capsule.leave', { anonId, pairId: capsulePairIdForModal, message });
  });

  function onCapsuleSaved(token) {
    capsuleTokenEl.textContent = token;
    capsuleTokenBox.hidden = false;
    sendCapsuleBtn.hidden = true;
    skipCapsuleBtn.textContent = 'Done';
  }

  copyTokenBtn.addEventListener('click', () => {
    navigator.clipboard.writeText(capsuleTokenEl.textContent).then(() => {
      copyTokenBtn.textContent = 'Copied!';
      setTimeout(() => (copyTokenBtn.textContent = 'Copy'), 1500);
    });
  });

  // ---------- boot ----------
  function boot() {
    if (videoEnabled) {
      fetch('/api/webrtc/ice-servers')
        .then((r) => r.json())
        .then((data) => {
          if (data.iceServers && data.iceServers.length) iceServers = data.iceServers;
        })
        .catch(() => {})
        .finally(() => {
          ensureLocalMedia().then(connect);
        });
    } else {
      connect();
    }
  }
  boot();

  window.addEventListener('beforeunload', () => {
    send('/app/queue.leave', { anonId, pairId: currentPairId });
  });
})();
