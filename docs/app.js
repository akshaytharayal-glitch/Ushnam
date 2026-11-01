// Reads the real latest GitHub Release and points the download buttons at its APK.
// If no release exists yet (or the API is unreachable) the buttons fall back to the
// GitHub releases page, so a link is never faked.
(function () {
  var OWNER = "akshaytharayal-glitch";
  var REPO = "Ushnam";
  var API = "https://api.github.com/repos/" + OWNER + "/" + REPO + "/releases/latest";
  var RELEASES = "https://github.com/" + OWNER + "/" + REPO + "/releases";

  var el = function (id) { return document.getElementById(id); };
  var buttons = document.querySelectorAll("[data-download]");
  var note = el("release-note");

  function setLinks(href) {
    buttons.forEach(function (a) { a.setAttribute("href", href); });
  }

  function showNote(html) {
    note.innerHTML = html;
    note.hidden = false;
  }

  function noRelease(reason) {
    el("v-version").textContent = "Not released yet";
    el("v-date").textContent = "—";
    el("v-size").textContent = "—";
    el("download-label").textContent = "Go to releases";
    setLinks(RELEASES);
    showNote(reason);
  }

  fetch(API, { headers: { Accept: "application/vnd.github+json" } })
    .then(function (res) {
      if (res.status === 404) { throw new Error("none"); }
      if (!res.ok) { throw new Error("http " + res.status); }
      return res.json();
    })
    .then(function (rel) {
      var apk = (rel.assets || []).find(function (a) { return /\.apk$/i.test(a.name); });
      if (!apk) { throw new Error("noapk"); }

      el("v-version").textContent = (rel.tag_name || "").replace(/^v/, "") || rel.name;
      el("v-date").textContent = new Date(rel.published_at).toLocaleDateString(undefined, {
        year: "numeric", month: "short", day: "numeric"
      });
      el("v-size").textContent = (apk.size / 1048576).toFixed(1) + " MB";
      el("download-label").textContent = "Download APK";
      setLinks(apk.browser_download_url);

      if (/debug-signed/i.test(rel.name || "")) {
        showNote("This release is <strong>debug-signed</strong>. It installs and runs normally, but can only be updated by builds signed with the same key.");
      }
    })
    .catch(function (err) {
      if (err && err.message === "none") {
        noRelease("The first APK hasn't been published yet. Once a version tag such as <code>v1.0.0</code> is pushed, this button links straight to it.");
      } else if (err && err.message === "noapk") {
        noRelease("The latest release has no APK attached yet. Check the <a href=\"" + RELEASES + "\">releases page</a> in a moment.");
      } else {
        el("v-version").textContent = "See releases";
        el("v-date").textContent = "—";
        el("v-size").textContent = "—";
        setLinks(RELEASES + "/latest");
        showNote("Couldn't check the latest version just now. The button opens the latest release page.");
      }
    });
})();
