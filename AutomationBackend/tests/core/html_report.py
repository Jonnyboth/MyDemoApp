from __future__ import annotations

import json
from pathlib import Path
from typing import Any

_PAGE_TEMPLATE = r"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>__TITLE__</title>
<style>
:root {
  --bg: #120a1f;
  --card: #1f1438;
  --border: #34215a;
  --border-faint: #2a1a4a;
  --text: #f2eefb;
  --text-dim: #ad9bd1;
  --accent: #a855f7;
  --accent-2: #c084fc;
  --accent-3: #7c3aed;
  --green: #22c55e;
  --green-soft: #4ade80;
  --red: #f87171;
  --red-soft: #fca5a5;
  --yellow: #fbbf24;
  --yellow-soft: #fcd34d;
}
* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  background: var(--bg);
  color: var(--text);
  font-family: -apple-system, "Segoe UI", Helvetica, Arial, sans-serif;
  padding: 40px 48px 90px;
}
.wrap { max-width: 1180px; margin: 0 auto; }
.header-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 4px;
}
h1 { margin: 0; font-size: 32px; font-weight: 800; color: var(--accent-2); letter-spacing: -0.5px; }
.meta { font-size: 13px; color: var(--text-dim); }
.meta a { color: var(--accent); }
.subtitle { margin: 0 0 30px; color: var(--text-dim); font-size: 14px; }
h2.section {
  font-size: 14px; text-transform: uppercase; letter-spacing: .6px;
  color: var(--text-dim); margin: 0 0 12px; font-weight: 700;
}
#env-grid {
  background: var(--card); border: 1px solid var(--border); border-radius: 12px;
  padding: 6px 0; margin-bottom: 32px; display: grid; grid-template-columns: 180px 1fr;
}
.env-key { padding: 10px 18px; font-size: 12px; font-weight: 700; color: var(--text-dim); border-top: 1px solid var(--border-faint); }
.env-val { padding: 10px 18px; font-size: 13px; color: var(--text); border-top: 1px solid var(--border-faint); }
.env-grid > :nth-child(-n+2) { border-top: none; }
.summary-row { display: flex; gap: 20px; flex-wrap: wrap; margin-bottom: 32px; }
.stat-cards { flex: 1 1 460px; display: grid; grid-template-columns: 1fr 1fr; gap: 14px; align-content: start; }
.stat-card { background: var(--card); border: 1px solid var(--border); border-radius: 12px; padding: 16px 18px; }
.stat-card__label { font-size: 11px; text-transform: uppercase; letter-spacing: .5px; color: var(--text-dim); font-weight: 700; margin-bottom: 6px; }
.stat-card__value { font-size: 26px; font-weight: 800; }
.donut-card {
  flex: 1 1 420px; background: var(--card); border: 1px solid var(--border); border-radius: 12px;
  padding: 20px 24px; display: flex; align-items: center; gap: 28px; flex-wrap: wrap;
}
.donut-wrap { position: relative; width: 168px; height: 168px; flex: 0 0 auto; }
.donut-center {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center;
  justify-content: center; pointer-events: none;
}
.donut-center__total { font-size: 28px; font-weight: 800; color: var(--text); }
.donut-center__label { font-size: 10px; color: var(--text-dim); letter-spacing: .5px; text-transform: uppercase; }
.legend { display: flex; flex-direction: column; gap: 8px; flex: 1 1 160px; min-width: 160px; }
.legend__item { display: flex; align-items: center; gap: 9px; font-size: 13px; cursor: pointer; padding: 5px 8px; border-radius: 8px; background: transparent; }
.legend__item.active { background: rgba(168,85,247,0.18); }
.legend__dot { width: 10px; height: 10px; border-radius: 50%; flex: 0 0 auto; }
.legend__label { color: var(--text); flex: 1 1 auto; }
.legend__count { color: var(--text-dim); }
.results-head { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; margin-bottom: 14px; }
.results-head__left { display: flex; align-items: center; gap: 10px; }
.filter-chip { display: flex; align-items: center; gap: 6px; background: var(--border-faint); color: #e9d5ff; font-size: 12px; padding: 3px 10px; border-radius: 999px; }
.filter-chip__clear { cursor: pointer; color: var(--text-dim); font-weight: 700; }
.controls { display: flex; gap: 10px; background: var(--card); border: 1px solid var(--border); border-radius: 999px; padding: 4px; }
.controls button {
  display: flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; color: #e9d5ff;
  cursor: pointer; padding: 7px 14px; border-radius: 999px; background: transparent; border: none;
  font-family: inherit;
}
.controls button:hover { background: rgba(168,85,247,0.22); }
.controls__sep { width: 1px; background: var(--border); margin: 6px 0; }
.row { border-radius: 10px; margin-bottom: 10px; overflow: hidden; border: 1px solid; border-left-width: 4px; }
.row__head { display: flex; align-items: center; gap: 14px; padding: 14px 18px; cursor: pointer; }
.row__badge { flex: 0 0 auto; padding: 3px 12px; border-radius: 999px; font-size: 11px; font-weight: 700; letter-spacing: .4px; text-transform: uppercase; }
.row__title { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.row__scenario { font-size: 13.5px; font-weight: 700; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row__id { font-family: "SFMono-Regular", Menlo, Consolas, monospace; font-size: 11px; color: var(--text-dim); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.row__asserts { flex: 0 0 auto; font-size: 12px; color: var(--text-dim); }
.row__duration { flex: 0 0 auto; font-size: 12px; color: var(--text-dim); min-width: 56px; text-align: right; }
.row__chevron { flex: 0 0 auto; color: var(--text-dim); font-size: 11px; }
.row__body { padding: 2px 18px 18px; display: flex; flex-direction: column; gap: 10px; }
.block { background: #160c26; border: 1px solid var(--border); border-left: 4px solid var(--accent-3); border-radius: 8px; padding: 10px 14px; }
.block__title { display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 700; margin-bottom: 6px; flex-wrap: wrap; }
.block__title .arrow { color: var(--accent); }
.block__method { background: #5b21b6; color: #fff; padding: 1px 8px; border-radius: 4px; font-size: 10px; font-weight: 700; }
.block__url { color: var(--text-dim); font-weight: 400; word-break: break-all; }
.block__status { padding: 1px 10px; border-radius: 999px; font-size: 10px; font-weight: 700; color: #0b0614; }
.block__body { background: var(--bg); color: #c9bdea; border-radius: 6px; padding: 8px 10px; margin: 0; max-height: 220px; overflow: auto; font-family: "SFMono-Regular", Menlo, Consolas, monospace; font-size: 11.5px; white-space: pre-wrap; }
.assert-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; padding: 9px 14px; border-radius: 8px; border: 1px solid; }
.assert-row__icon { font-weight: 800; }
.assert-row__label { font-weight: 700; font-size: 11px; letter-spacing: .4px; text-transform: uppercase; }
.assert-row__desc { color: var(--text); font-size: 12px; }
.assert-row__values { color: var(--text-dim); font-size: 12px; }
.assert-row__values code { background: var(--bg); padding: 1px 5px; border-radius: 4px; color: var(--text); }
.empty-state { color: var(--text-dim); font-size: 13px; padding: 20px; text-align: center; border: 1px dashed var(--border); border-radius: 10px; }
</style>
</head>
<body>
<div class="wrap">
  <div class="header-row">
    <h1>Reporte de Pruebas</h1>
    <div class="meta">Generado el __GENERATED_AT__ &middot; API Test Framework</div>
  </div>
  <p class="subtitle" id="subtitle"></p>

  <h2 class="section">Entorno</h2>
  <div id="env-grid"></div>

  <h2 class="section">Resumen</h2>
  <div class="summary-row">
    <div class="stat-cards" id="stat-cards"></div>
    <div class="donut-card">
      <div class="donut-wrap">
        <svg viewBox="0 0 160 160" width="168" height="168" style="transform: rotate(-90deg);">
          <circle cx="80" cy="80" r="60" fill="none" stroke="var(--border-faint)" stroke-width="26"></circle>
          <g id="donut-slices"></g>
        </svg>
        <div class="donut-center">
          <div class="donut-center__total" id="donut-total"></div>
          <div class="donut-center__label">pruebas</div>
        </div>
      </div>
      <div class="legend" id="legend"></div>
    </div>
  </div>

  <div class="results-head">
    <div class="results-head__left">
      <h2 class="section" id="results-title" style="margin:0;">Resultados</h2>
      <div class="filter-chip" id="filter-chip" style="display:none;"></div>
    </div>
    <div class="controls">
      <button id="expand-all">&#9660;&#9660; Expandir todo</button>
      <div class="controls__sep"></div>
      <button id="collapse-all">^^ Colapsar todo</button>
    </div>
  </div>
  <div id="rows"></div>
</div>

<script id="report-data" type="application/json">__DATA_JSON__</script>
<script>
(function () {
  "use strict";
  var raw = document.getElementById("report-data").textContent;
  var data = JSON.parse(raw);
  var state = { expanded: {}, filter: null };

  function esc(s) {
    var d = document.createElement("div");
    d.textContent = s == null ? "" : String(s);
    return d.innerHTML;
  }

  function categoryFor(result) {
    var r = (result || "").toLowerCase();
    if (r === "passed") return "passed";
    if (r === "failed" || r === "error") return "failed";
    return "other";
  }

  function colorsFor(category) {
    if (category === "passed") {
      return { border: "#22c55e", bg: "rgba(34,197,94,0.08)", faint: "rgba(34,197,94,0.35)", badgeBg: "rgba(34,197,94,0.2)", badgeText: "#4ade80" };
    }
    if (category === "failed") {
      return { border: "#f87171", bg: "rgba(248,113,113,0.1)", faint: "rgba(248,113,113,0.4)", badgeBg: "rgba(248,113,113,0.22)", badgeText: "#fca5a5" };
    }
    return { border: "#fbbf24", bg: "rgba(251,191,36,0.08)", faint: "rgba(251,191,36,0.4)", badgeBg: "rgba(251,191,36,0.2)", badgeText: "#fcd34d" };
  }

  function statusColorFor(cls) {
    if (cls === "4xx") return "#fbbf24";
    if (cls === "5xx") return "#f87171";
    return "#22c55e";
  }

  function formatDuration(ms) {
    if (ms >= 1000) return (ms / 1000).toFixed(2) + " s";
    return Math.round(ms) + " ms";
  }

  function renderEnvironment() {
    var grid = document.getElementById("env-grid");
    var env = data.environment || {};
    var html = "";
    var keys = Object.keys(env);
    keys.forEach(function (key, idx) {
      var value = env[key];
      var isObj = typeof value === "object" && value !== null;
      html += '<div class="env-key">' + esc(key) + "</div>";
      if (isObj) {
        var items = Object.keys(value).map(function (k) { return esc(k) + ": " + esc(value[k]); });
        html += '<div class="env-val"><div style="display:flex;flex-direction:column;gap:3px;">' +
          items.map(function (it) { return "<div>" + it + "</div>"; }).join("") + "</div></div>";
      } else {
        html += '<div class="env-val">' + esc(value) + "</div>";
      }
    });
    grid.innerHTML = html;
  }

  function computeStats() {
    var tests = data.tests || [];
    var total = tests.length;
    var passed = tests.filter(function (t) { return categoryFor(t.result) === "passed"; }).length;
    var failed = tests.filter(function (t) { return categoryFor(t.result) === "failed"; }).length;
    var totalMs = 0;
    tests.forEach(function (t) { totalMs += t.durationMs || 0; });
    return { total: total, passed: passed, failed: failed, totalMs: totalMs };
  }

  function renderStatCards(stats) {
    var el = document.getElementById("stat-cards");
    var cards = [
      { label: "Total", value: stats.total, color: "var(--accent-2)" },
      { label: "Passed", value: stats.passed, color: "var(--green-soft)" },
      { label: "Failed", value: stats.failed, color: "var(--red-soft)" },
      { label: "Duración total", value: formatDuration(stats.totalMs), color: "var(--accent)" }
    ];
    el.innerHTML = cards.map(function (c) {
      return '<div class="stat-card"><div class="stat-card__label">' + esc(c.label) +
        '</div><div class="stat-card__value" style="color:' + c.color + '">' + esc(c.value) + "</div></div>";
    }).join("");
  }

  var STATUS_META = [
    { key: "passed", label: "Passed", color: "#22c55e" },
    { key: "failed", label: "Failed", color: "#f87171" },
    { key: "error", label: "Error", color: "#ef4444" },
    { key: "skipped", label: "Skipped", color: "#fbbf24" }
  ];

  function renderDonut(stats) {
    var tests = data.tests || [];
    var total = stats.total;
    document.getElementById("donut-total").textContent = total;

    var r = 60;
    var circumference = 2 * Math.PI * r;
    var cumulative = 0;
    var slicesG = document.getElementById("donut-slices");
    var legendEl = document.getElementById("legend");
    slicesG.innerHTML = "";
    var legendHtml = "";

    STATUS_META.forEach(function (meta) {
      var count = tests.filter(function (t) { return (t.result || "").toLowerCase() === meta.key; }).length;
      if (count === 0) return;
      var pct = total ? (count / total) * 100 : 0;
      var dash = (pct / 100) * circumference;
      var active = state.filter === meta.key;
      var opacity = state.filter && !active ? 0.35 : 1;

      var circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
      circle.setAttribute("cx", "80");
      circle.setAttribute("cy", "80");
      circle.setAttribute("r", String(r));
      circle.setAttribute("fill", "none");
      circle.setAttribute("stroke", meta.color);
      circle.setAttribute("stroke-width", "26");
      circle.setAttribute("stroke-dasharray", dash + " " + (circumference - dash));
      circle.setAttribute("stroke-dashoffset", String(-cumulative));
      circle.style.cursor = "pointer";
      circle.style.opacity = String(opacity);
      circle.style.transition = "opacity .15s";
      circle.addEventListener("click", (function (k) { return function () { setFilter(k); }; })(meta.key));
      slicesG.appendChild(circle);

      legendHtml += '<div class="legend__item' + (active ? " active" : "") + '" data-key="' + meta.key + '">' +
        '<span class="legend__dot" style="background:' + meta.color + '"></span>' +
        '<span class="legend__label">' + esc(meta.label) + "</span>" +
        '<span class="legend__count">' + count + " (" + pct.toFixed(1) + "%)</span></div>";

      cumulative += dash;
    });

    legendEl.innerHTML = legendHtml;
    Array.prototype.forEach.call(legendEl.querySelectorAll(".legend__item"), function (node) {
      node.addEventListener("click", function () { setFilter(node.getAttribute("data-key")); });
    });
  }

  function setFilter(key) {
    state.filter = state.filter === key ? null : key;
    renderAll();
  }

  function clearFilter() {
    state.filter = null;
    renderAll();
  }

  function toggleRow(id) {
    state.expanded[id] = !state.expanded[id];
    renderRows();
  }

  function expandAll() {
    (data.tests || []).forEach(function (t) { state.expanded[t.id] = true; });
    renderRows();
  }

  function collapseAll() {
    state.expanded = {};
    renderRows();
  }

  function renderItem(item) {
    if (item.type === "request") {
      var bodyHtml = item.body
        ? '<pre class="block__body">' + esc(item.body) + "</pre>"
        : "";
      return '<div class="block">' +
        '<div class="block__title"><span class="arrow">&#9654; REQUEST</span>' +
        '<span class="block__method">' + esc(item.method) + "</span>" +
        '<span class="block__url">' + esc(item.url) + "</span></div>" +
        bodyHtml + "</div>";
    }
    if (item.type === "response") {
      var color = statusColorFor(item.statusClass);
      var rBodyHtml = item.body
        ? '<pre class="block__body">' + esc(item.body) + "</pre>"
        : "";
      return '<div class="block" style="border-left-color:' + color + '">' +
        '<div class="block__title"><span class="arrow" style="color:' + color + '">&#9664; RESPONSE</span>' +
        '<span class="block__status" style="background:' + color + '">' + esc(item.status) + "</span></div>" +
        rBodyHtml + "</div>";
    }
    // assertion
    var assertColor = item.pass ? "#22c55e" : "#f87171";
    var assertBg = item.pass ? "rgba(34,197,94,0.12)" : "rgba(248,113,113,0.14)";
    var icon = item.pass ? "&#10004;" : "&#10006;";
    var label = item.pass ? "SUCCESS ASSERTION" : "FAILED ASSERTION";
    return '<div class="assert-row" style="background:' + assertBg + ";border-color:" + assertColor + '">' +
      '<span class="assert-row__icon" style="color:' + assertColor + '">' + icon + "</span>" +
      '<span class="assert-row__label" style="color:' + assertColor + '">' + esc(label) + "</span>" +
      '<span class="assert-row__desc">' + esc(item.desc) + "</span>" +
      '<span class="assert-row__values">esperado=<code>' + esc(item.expected) + "</code> obtenido=<code>" +
      esc(item.obtained) + "</code></span></div>";
  }

  function renderRows() {
    var tests = data.tests || [];
    var filtered = state.filter
      ? tests.filter(function (t) { return (t.result || "").toLowerCase() === state.filter; })
      : tests;

    document.getElementById("results-title").textContent = "Resultados (" + filtered.length + ")";

    var chip = document.getElementById("filter-chip");
    if (state.filter) {
      var meta = STATUS_META.filter(function (m) { return m.key === state.filter; })[0];
      chip.style.display = "flex";
      chip.innerHTML = esc(meta ? meta.label : state.filter) + ' <span class="filter-chip__clear" id="clear-filter">&#10005;</span>';
      document.getElementById("clear-filter").addEventListener("click", clearFilter);
    } else {
      chip.style.display = "none";
      chip.innerHTML = "";
    }

    var rowsEl = document.getElementById("rows");
    if (filtered.length === 0) {
      rowsEl.innerHTML = '<div class="empty-state">No hay resultados para este filtro.</div>';
      return;
    }

    rowsEl.innerHTML = filtered.map(function (t) {
      var category = categoryFor(t.result);
      var c = colorsFor(category);
      var expanded = !!state.expanded[t.id];
      var bodyHtml = expanded
        ? '<div class="row__body">' + (t.items || []).map(renderItem).join("") + "</div>"
        : "";
      return '<div class="row" style="background:' + c.bg + ";border-color:" + c.faint + ";border-left-color:" + c.border + '">' +
        '<div class="row__head" data-id="' + esc(t.id) + '">' +
        '<span class="row__badge" style="background:' + c.badgeBg + ";color:" + c.badgeText + '">' + esc(t.result) + "</span>" +
        '<div class="row__title"><span class="row__scenario">' + esc(t.scenarioName || t.testId) + '</span><span class="row__id">' + esc(t.testId) + "</span></div>" +
        '<span class="row__asserts">' + t.assertionPassCount + "/" + t.assertionCount + " asserts</span>" +
        '<span class="row__duration">' + esc(t.duration) + "</span>" +
        '<span class="row__chevron">' + (expanded ? "&#9650; ocultar" : "&#9660; ver detalle") + "</span>" +
        "</div>" + bodyHtml + "</div>";
    }).join("");

    Array.prototype.forEach.call(rowsEl.querySelectorAll(".row__head"), function (node) {
      node.addEventListener("click", function () { toggleRow(node.getAttribute("data-id")); });
    });
  }

  function renderAll() {
    var stats = computeStats();
    document.getElementById("subtitle").textContent = stats.total + " pruebas ejecutadas en " + formatDuration(stats.totalMs);
    renderStatCards(stats);
    renderDonut(stats);
    renderRows();
  }

  renderEnvironment();
  renderAll();

  document.getElementById("expand-all").addEventListener("click", expandAll);
  document.getElementById("collapse-all").addEventListener("click", collapseAll);
})();
</script>
</body>
</html>
"""


def generate_report(output_path: Path, title: str, generated_at: str, data: dict[str, Any]) -> None:
    """Escribe el reporte HTML autocontenido (CSS + JS inline, sin dependencias externas)."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    payload = json.dumps(data, ensure_ascii=False).replace("</script", "<\\/script")
    html = (
        _PAGE_TEMPLATE
        .replace("__TITLE__", title)
        .replace("__GENERATED_AT__", generated_at)
        .replace("__DATA_JSON__", payload)
    )
    output_path.write_text(html, encoding="utf-8")
