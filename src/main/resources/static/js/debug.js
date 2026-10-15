// Temporary diagnostic aid: loads an on-page console (eruda) when the page
// is visited with ?debug=1, so connection errors can be inspected directly
// on a device with no attached desktop devtools (e.g. a phone). Remove once
// no longer needed.
//
// Uses document.write with external (same-origin) <script src> tags so the
// browser's classic parser-blocking behavior guarantees eruda finishes
// loading and initializing before the next script tag (sockjs/stomp/
// chat.js) runs -- important since the connection failure this is meant to
// catch happens immediately on page load.
(function () {
  if (location.search.indexOf('debug=1') === -1) {
    return;
  }
  document.write('<script src="/js/eruda.js"><\/script>');
  document.write('<script src="/js/eruda-init.js"><\/script>');
})();
