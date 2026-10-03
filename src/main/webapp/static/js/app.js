/* Small site-wide behaviours: dismiss alerts, confirm destructive forms, close the account menu. */
(function () {
  "use strict";
  document.addEventListener("click", (e) => {
    const x = e.target.closest(".alert .x");
    if (x) x.parentElement.remove();
    const menu = document.querySelector("details.account[open]");
    if (menu && !menu.contains(e.target)) menu.removeAttribute("open");
  });
  document.addEventListener("submit", (e) => {
    const msg = e.target.getAttribute("data-confirm");
    if (msg && !window.confirm(msg)) e.preventDefault();
  });
})();
