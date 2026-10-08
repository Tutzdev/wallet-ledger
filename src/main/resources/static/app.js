"use strict";

const API = "/api/v1";
const TOKEN_KEY = "ledger.token";
const PAGE_SIZE = 15;

const state = {
  token: readToken(),
  accounts: new Map(),
  transfersPage: 0,
  moneyKind: "deposit",
  idempotencyKey: newKey(),
  reversalTarget: null,
};

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => document.querySelectorAll(selector);

/* ---------- API ---------- */

async function api(path, { method = "GET", body, idempotencyKey } = {}) {
  const headers = { Accept: "application/json" };
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  if (body) headers["Content-Type"] = "application/json";
  if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;

  const response = await fetch(API + path, { method, headers, body: body && JSON.stringify(body) });
  if (response.status === 401 && path !== "/auth/token") {
    signOut();
    throw new Error("Session expired. Sign in again.");
  }
  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) {
    const problem = data || {};
    throw new Error(problem.code ? `${problem.detail} (${problem.code})` : `Request failed (${response.status})`);
  }
  return data;
}

/* ---------- Session ---------- */

function readToken() {
  try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}

function storeToken(token) {
  state.token = token;
  try { token ? sessionStorage.setItem(TOKEN_KEY, token) : sessionStorage.removeItem(TOKEN_KEY); } catch { /* private mode */ }
}

function signOut() {
  storeToken(null);
  showLogin();
}

function showLogin() {
  $("#app").hidden = true;
  $("#login").hidden = false;
}

async function showApp() {
  $("#login").hidden = true;
  $("#app").hidden = false;
  await refresh();
}

$("#login-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  $("#login-error").textContent = "";
  try {
    const { accessToken } = await api("/auth/token", {
      method: "POST",
      body: { email: form.get("email"), password: form.get("password") },
    });
    storeToken(accessToken);
    await showApp();
  } catch (error) {
    $("#login-error").textContent = error.message;
  }
});

$("#logout").addEventListener("click", signOut);

/* ---------- Navigation ---------- */

const TITLES = { overview: "Overview", accounts: "Accounts", transfers: "Transfers" };

$$(".sidebar nav .nav-item").forEach((item) =>
  item.addEventListener("click", () => {
    $$(".sidebar nav .nav-item").forEach((other) => other.classList.toggle("active", other === item));
    $$(".view").forEach((view) => { view.hidden = view.id !== `view-${item.dataset.view}`; });
    $("#view-title").textContent = TITLES[item.dataset.view];
  }));

/* ---------- Data ---------- */

async function refresh() {
  await loadAccounts();
  await Promise.all([renderOverview(), renderAccounts(), renderTransfers()]);
}

async function loadAccounts() {
  const [customers, system] = await Promise.all([
    api("/accounts?type=CUSTOMER&size=100"),
    api("/accounts?type=SYSTEM&size=10"),
  ]);
  state.accounts = new Map([...customers.content, ...system.content].map((account) => [account.id, account]));
}

function customerAccounts() {
  return [...state.accounts.values()].filter((account) => account.type === "CUSTOMER");
}

/* ---------- Overview ---------- */

async function renderOverview() {
  const [summary, integrity, latest] = await Promise.all([
    api("/ledger/summary"),
    api("/ledger/integrity"),
    api(`/transfers?size=6`),
  ]);

  const kpis = [
    kpi("Customer accounts", summary.customerAccounts, `${summary.frozenAccounts} frozen`),
    kpi("Transfers today", summary.transfersToday, amounts(summary.volumeToday) || "No volume yet"),
    kpi("Held by customers", amounts(summary.heldByCustomers) || "0.00", "Sum of customer balances"),
    kpi("Outbox pending", summary.outboxPending,
      summary.oldestPendingEventSeconds == null ? "All events published" : `Oldest waiting ${summary.oldestPendingEventSeconds}s`),
    kpi("Ledger integrity", integrity.balanced ? "Balanced" : "Unbalanced",
      integrity.balanced ? "Every transfer sums to zero" : `${integrity.unbalancedTransfers} unbalanced transfers`,
      integrity.balanced ? "kpi-ok" : "kpi-bad"),
  ];
  $("#kpis").innerHTML = kpis.join("");
  $("#latest-transfers").innerHTML = transferTable(latest.content, false);
}

function kpi(label, value, sub, modifier = "") {
  return `<article class="kpi ${modifier}">
    <p class="kpi-label">${escape(label)}</p>
    <p class="kpi-value">${escape(value)}</p>
    <p class="kpi-sub">${escape(sub)}</p>
  </article>`;
}

function amounts(list) {
  return list.map((item) => money(item.amount, item.currency)).join(" · ");
}

/* ---------- Accounts ---------- */

async function renderAccounts() {
  const rows = customerAccounts().map((account) => `<tr>
      <td><strong>${escape(account.ownerName)}</strong><br><code class="muted">${shortId(account.id)}</code></td>
      <td>${account.currency}</td>
      <td><span class="badge badge-${account.status}">${account.status}</span></td>
      <td class="num">${money(account.balance, account.currency)}</td>
      <td class="muted">${date(account.createdAt)}</td>
      <td><div class="row-actions">
        <button class="btn-link" data-statement="${account.id}">Statement</button>
        <button class="btn-link" data-status="${account.id}" data-next="${account.status === "ACTIVE" ? "FROZEN" : "ACTIVE"}">
          ${account.status === "ACTIVE" ? "Freeze" : "Unfreeze"}</button>
      </div></td>
    </tr>`);

  $("#accounts-table").innerHTML = `<thead><tr>
      <th>Owner</th><th>Currency</th><th>Status</th><th class="num">Balance</th><th>Opened</th><th></th>
    </tr></thead>
    <tbody>${rows.join("") || `<tr><td colspan="6" class="empty">No accounts yet. Create the first one.</td></tr>`}</tbody>`;
}

$("#accounts-table").addEventListener("click", async (event) => {
  const statementId = event.target.dataset.statement;
  const statusId = event.target.dataset.status;
  try {
    if (statementId) await openStatement(statementId);
    if (statusId) {
      await api(`/accounts/${statusId}/status`, { method: "PATCH", body: { status: event.target.dataset.next } });
      toast(event.target.dataset.next === "FROZEN" ? "Account frozen" : "Account active again");
      await refresh();
    }
  } catch (error) {
    toast(error.message);
  }
});

async function openStatement(accountId) {
  const [account, statement] = await Promise.all([
    api(`/accounts/${accountId}`),
    api(`/accounts/${accountId}/entries?size=50`),
  ]);
  $("#statement-owner").textContent = account.ownerName;
  $("#statement-currency").textContent = `${account.currency} account · ${account.status.toLowerCase()}`;
  $("#statement-balance").textContent = money(account.balance, account.currency);
  $("#statement-lines").innerHTML = statement.content.map((line) => `<li>
      <span><strong>${label(line.type)}</strong><small>${escape(line.description || "No description")} · ${date(line.createdAt)}</small></span>
      <span class="num ${line.amount < 0 ? "debit" : "credit"}">${money(line.amount, line.currency, true)}</span>
    </li>`).join("") || `<li class="muted">No movements yet.</li>`;
  $("#statement").hidden = false;
}

$("#close-statement").addEventListener("click", () => { $("#statement").hidden = true; });

$("#account-form").addEventListener("submit", async (event) => {
  if (event.submitter?.value !== "submit") return;
  event.preventDefault();
  const form = new FormData(event.target);
  try {
    await api("/accounts", { method: "POST", body: { ownerName: form.get("ownerName"), currency: form.get("currency") } });
    event.target.reset();
    $("#account-modal").close();
    toast("Account created");
    await refresh();
  } catch (error) {
    toast(error.message);
  }
});

/* ---------- Transfers ---------- */

async function renderTransfers() {
  const page = await api(`/transfers?page=${state.transfersPage}&size=${PAGE_SIZE}`);
  $("#transfers-table").innerHTML = transferTable(page.content, true);
  $("#page-label").textContent = `Page ${page.page + 1} of ${Math.max(page.totalPages, 1)}`;
  $("#prev-page").disabled = page.page === 0;
  $("#next-page").disabled = page.page + 1 >= page.totalPages;
}

function transferTable(transfers, withActions) {
  const reversed = new Set(transfers.map((transfer) => transfer.reversalOfId).filter(Boolean));
  const rows = transfers.map((transfer) => `<tr>
      <td><span class="badge badge-${transfer.type}">${label(transfer.type)}</span></td>
      <td>${accountName(transfer.sourceAccountId)} → ${accountName(transfer.destinationAccountId)}</td>
      <td class="muted">${escape(transfer.description || "")}</td>
      <td class="num">${money(transfer.amount, transfer.currency)}</td>
      <td class="muted">${date(transfer.createdAt)}</td>
      ${withActions ? `<td>${transfer.type === "REVERSAL" || reversed.has(transfer.id) ? ""
        : `<button class="btn-link" data-reverse="${transfer.id}">Reverse</button>`}</td>` : ""}
    </tr>`);

  return `<thead><tr><th>Type</th><th>Route</th><th>Description</th><th class="num">Amount</th><th>When</th>
      ${withActions ? "<th></th>" : ""}</tr></thead>
    <tbody>${rows.join("") || `<tr><td colspan="6" class="empty">No transfers yet.</td></tr>`}</tbody>`;
}

$("#prev-page").addEventListener("click", () => { state.transfersPage--; renderTransfers().catch((e) => toast(e.message)); });
$("#next-page").addEventListener("click", () => { state.transfersPage++; renderTransfers().catch((e) => toast(e.message)); });

$("#transfers-table").addEventListener("click", (event) => {
  if (!event.target.dataset.reverse) return;
  state.reversalTarget = event.target.dataset.reverse;
  state.idempotencyKey = newKey();
  $("#reversal-error").textContent = "";
  $("#reversal-modal").showModal();
});

$("#reversal-form").addEventListener("submit", async (event) => {
  if (event.submitter?.value !== "submit") return;
  event.preventDefault();
  const form = new FormData(event.target);
  try {
    await api(`/transfers/${state.reversalTarget}/reversal`, {
      method: "POST",
      body: { reason: form.get("reason") },
      idempotencyKey: state.idempotencyKey,
    });
    event.target.reset();
    $("#reversal-modal").close();
    toast("Transfer reversed");
    await refresh();
  } catch (error) {
    $("#reversal-error").textContent = error.message;
  }
});

/* ---------- Move money ---------- */

$$("[data-open]").forEach((button) => button.addEventListener("click", () => {
  if (button.dataset.open === "money-modal") prepareMoneyForm();
  $(`#${button.dataset.open}`).showModal();
}));

$$(".tab").forEach((tab) => tab.addEventListener("click", () => {
  state.moneyKind = tab.dataset.kind;
  $$(".tab").forEach((other) => other.classList.toggle("active", other === tab));
  prepareMoneyForm();
}));

/**
 * One key per intended operation. A retry after a network error reuses it, so the server
 * answers with the transfer it already posted instead of moving the money twice.
 */
function prepareMoneyForm() {
  const options = customerAccounts()
    .map((account) => `<option value="${account.id}">${escape(account.ownerName)} · ${money(account.balance, account.currency)}</option>`)
    .join("");
  $("#money-form [name=source]").innerHTML = options;
  $("#money-form [name=destination]").innerHTML = options;
  $("[data-field=source]").hidden = state.moneyKind === "deposit";
  $("[data-field=destination]").hidden = state.moneyKind === "withdrawal";
  $("#money-error").textContent = "";
  state.idempotencyKey = newKey();
  $("#idempotency-key").textContent = state.idempotencyKey;
}

$("#money-form").addEventListener("submit", async (event) => {
  if (event.submitter?.value !== "submit") return;
  event.preventDefault();
  const form = new FormData(event.target);
  const amount = form.get("amount");
  const description = form.get("description") || null;
  const requests = {
    deposit: ["/deposits", { accountId: form.get("destination"), amount, description }],
    withdrawal: ["/withdrawals", { accountId: form.get("source"), amount, description }],
    transfer: ["/transfers", { sourceAccountId: form.get("source"), destinationAccountId: form.get("destination"), amount, description }],
  };
  const [path, body] = requests[state.moneyKind];
  try {
    await api(path, { method: "POST", body, idempotencyKey: state.idempotencyKey });
    event.target.reset();
    $("#money-modal").close();
    toast("Money moved");
    await refresh();
  } catch (error) {
    $("#money-error").textContent = error.message;
  }
});

/* ---------- Formatting ---------- */

function money(amount, currency, signed = false) {
  const formatted = new Intl.NumberFormat("en-US", { style: "currency", currency }).format(Math.abs(amount));
  if (!signed) return amount < 0 ? `-${formatted}` : formatted;
  return `${amount < 0 ? "−" : "+"}${formatted}`;
}

function date(iso) {
  return new Date(iso).toLocaleString("en-US", { dateStyle: "medium", timeStyle: "short" });
}

function label(type) {
  return type.charAt(0) + type.slice(1).toLowerCase();
}

function accountName(id) {
  const account = state.accounts.get(id);
  return account ? escape(account.ownerName) : `<code>${shortId(id)}</code>`;
}

function shortId(id) {
  return id.slice(0, 8);
}

function newKey() {
  return crypto.randomUUID();
}

function escape(value) {
  return String(value).replace(/[&<>"']/g, (char) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

let toastTimer;
function toast(message) {
  const element = $("#toast");
  element.textContent = message;
  element.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { element.hidden = true; }, 3500);
}

/* ---------- Start ---------- */

if (state.token) {
  showApp().catch(() => signOut());
} else {
  showLogin();
}
