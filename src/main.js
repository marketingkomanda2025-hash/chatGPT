import "./style.css";
import {
  isoDate,
  parseDate,
  daysBetween,
  orderedRange,
  totalDays,
} from "./dates.js";

const star = `<svg viewBox="0 0 40 40" fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" aria-hidden="true"><path d="M20 4v32M4 20h32M9 9l22 22M9 31 31 9"/></svg>`;
const colors = ["#7062d9", "#399b80", "#df9550", "#d36f91", "#4e91c9"];
const colorNames = [
  "Лавандовый",
  "Зелёный",
  "Персиковый",
  "Розовый",
  "Голубой",
];
const storageKey = "peredyshka.vacations.v1";
const today = isoDate(new Date());
let visible = parseDate(today);
let selection = null;
let editing = null;
let selectedColor = colors[0];
let storageWarning = false;
let trips = [];
try {
  const stored = JSON.parse(localStorage.getItem(storageKey) || "[]");
  if (!Array.isArray(stored)) throw new Error("Invalid data");
  trips = stored.filter(
    (t) =>
      t &&
      typeof t.id === "string" &&
      typeof t.title === "string" &&
      parseDate(t.start) &&
      parseDate(t.end) &&
      t.start <= t.end &&
      colors.includes(t.color),
  );
} catch {
  storageWarning = true;
}

const escape = (text) =>
  String(text).replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const format = (value) =>
  parseDate(value).toLocaleDateString("ru-RU", {
    day: "numeric",
    month: "short",
  });
const dayLabel = (n) =>
  `${n} ${n % 10 === 1 && n % 100 !== 11 ? "день" : n % 10 >= 2 && n % 10 <= 4 && !(n % 100 >= 12 && n % 100 <= 14) ? "дня" : "дней"}`;
const app = document.querySelector("#app");
app.innerHTML = `
  <header class="topbar"><a class="brand" href="./"><span class="brand-icon">${star}</span> передышка<span class="brand-dot">.</span></a><span class="top-note"><span class="status-dot"></span> Личное пространство</span></header>
  <main>
    <div class="heading"><div><div class="eyebrow">БОЛЬШЕ ВРЕМЕНИ ДЛЯ СЕБЯ</div><h1>Время отдохнуть.</h1><p>Планируйте отпуск. Предвкушайте приятное.</p></div><button class="primary" id="add"><span>＋</span> Запланировать отпуск</button></div>
    <div id="storage-warning" role="status" class="warning" hidden></div>
    <div class="layout"><section class="calendar-panel" aria-label="Календарь отпусков">
      <div class="calendar-toolbar"><div class="month-navigation"><h2 id="month-title"></h2><div class="arrow-group"><button class="icon-button" id="previous" aria-label="Предыдущий месяц">‹</button><button class="icon-button" id="next" aria-label="Следующий месяц">›</button></div></div><button class="secondary" id="today">Сегодня</button></div>
      <div class="weekdays" aria-hidden="true">${["Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"].map((d) => `<span>${d}</span>`).join("")}</div>
      <div id="calendar" class="calendar"></div>
      <div class="calendar-footer"><span><span class="tiny-calendar">▧</span> <span id="selection-help">Выберите первый и последний день отпуска</span></span><button id="cancel-selection" class="text-button" hidden>Сбросить</button><span class="weekend-key"><i></i> Выходные</span></div>
    </section>
    <aside><section class="summary"><span class="summary-decoration">${star}</span><div class="summary-label">ВАШИ ПЛАНЫ НА <span id="summary-year"></span></div><div class="summary-number"><span id="total-days">0</span><span>дней отдыха</span></div><div class="summary-bottom"><span class="sun">☀︎</span><span>Даже небольшой отпуск —<br>уже хороший план.</span></div></section>
      <section class="plans"><div class="plans-heading"><h2>Мои отпуска</h2><span id="trip-count" class="badge">0</span></div><div id="trip-list"></div></section>
      <div class="local-note"><span>⌂</span><p>Только ваши планы<br><small>Сохраняются в этом браузере.<br>Без синхронизации с календарями.</small></p></div>
    </aside></div>
    <footer>Маленькая пауза. Большая польза.<span>Сделано для ваших хороших дней <span class="footer-star">${star}</span></span></footer>
  </main>
  <dialog id="trip-dialog"><form id="trip-form"><div class="dialog-heading"><div><div class="eyebrow">ВРЕМЯ ДЛЯ СЕБЯ</div><h2 id="dialog-title">Новый отпуск</h2></div><button type="button" class="icon-button" id="close-dialog" aria-label="Закрыть">×</button></div>
    <label>Название<input id="trip-title" name="title" maxlength="80" placeholder="Например, к морю" required /></label>
    <div class="date-fields"><label>Первый день<input id="trip-start" name="start" type="date" min="1900-01-01" max="9999-12-31" required /></label><label>Последний день<input id="trip-end" name="end" type="date" min="1900-01-01" max="9999-12-31" required /></label></div>
    <fieldset><legend>Цвет в календаре</legend><div class="color-options">${colors.map((color, i) => `<button type="button" class="color-choice" style="--color:${color}" data-color="${color}" aria-label="${colorNames[i]}" aria-pressed="false"></button>`).join("")}</div></fieldset>
    <p id="form-error" class="form-error" role="alert"></p><div class="dialog-actions"><button type="button" class="danger" id="delete-trip" hidden>Удалить отпуск</button><button type="button" class="secondary" id="cancel-dialog">Отмена</button><button class="primary" type="submit">Сохранить</button></div>
  </form></dialog>
  <dialog id="delete-dialog"><form method="dialog"><h2>Удалить отпуск?</h2><p>Период исчезнет из календаря. Это действие нельзя отменить.</p><div class="dialog-actions"><button class="secondary" value="cancel">Оставить</button><button class="danger filled" value="delete">Удалить</button></div></form></dialog>
  <div id="announcement" class="sr-only" role="status" aria-live="polite"></div>`;

const dialog = document.querySelector("#trip-dialog");
const form = document.querySelector("#trip-form");
const startInput = document.querySelector("#trip-start");
const endInput = document.querySelector("#trip-end");

function render() {
  const year = visible.getFullYear();
  const month = visible.getMonth();
  document.querySelector("#month-title").textContent = visible
    .toLocaleDateString("ru-RU", { month: "long", year: "numeric" })
    .replace(" г.", "");
  document.querySelector("#summary-year").textContent = year;
  document.querySelector("#total-days").textContent = totalDays(trips, year);
  document.querySelector("#trip-count").textContent = trips.length;
  const first = new Date(year, month, 1, 12);
  const offset = (first.getDay() + 6) % 7;
  const cells =
    Math.ceil((offset + new Date(year, month + 1, 0).getDate()) / 7) * 7;
  document.querySelector("#calendar").innerHTML = Array.from(
    { length: cells },
    (_, i) => {
      const date = new Date(year, month, 1 - offset + i, 12);
      const value = isoDate(date);
      const outside = date.getMonth() !== month;
      const entries = trips.filter((t) => t.start <= value && t.end >= value);
      const label = date.toLocaleDateString("ru-RU", {
        day: "numeric",
        month: "long",
        year: "numeric",
      });
      return `<div class="day ${outside ? "outside" : ""} ${i % 7 >= 5 ? "weekend" : ""} ${selection === value ? "selected" : ""}"><button class="day-select" data-date="${value}" aria-label="${label}${selection === value ? ", начало периода" : ""}" ${value === today ? 'aria-current="date"' : ""}><span class="day-number ${value === today ? "current" : ""}">${date.getDate()}</span></button><div class="day-trips">${entries.map((t) => `<button class="vacation" data-trip="${escape(t.id)}" style="--color:${t.color}" title="${escape(t.title)}: ${format(t.start)} — ${format(t.end)}" aria-label="Изменить отпуск ${escape(t.title)}, ${label}"><span>${escape(t.title)}</span></button>`).join("")}</div></div>`;
    },
  ).join("");
  document.querySelector("#selection-help").textContent = selection
    ? `Начало: ${format(selection)}. Выберите последний день`
    : "Выберите первый и последний день отпуска";
  document.querySelector("#cancel-selection").hidden = !selection;
  document.querySelector("#trip-list").innerHTML = trips.length
    ? [...trips]
        .sort((a, b) => a.start.localeCompare(b.start))
        .map(
          (t) =>
            `<button class="trip-card" data-trip="${escape(t.id)}" style="--color:${t.color}"><span class="trip-color"></span><span class="trip-info"><strong>${escape(t.title)}</strong><span>${format(t.start)} ${t.start.slice(0, 4)} — ${format(t.end)} ${t.end.slice(0, 4)}</span><small>${dayLabel(daysBetween(t.start, t.end))}${t.end < today ? " · Завершён" : t.start <= today ? " · Сейчас отдыхаете" : ""}</small></span><span class="trip-edit" aria-hidden="true">›</span></button>`,
        )
        .join("")
    : `<div class="empty-state"><div class="empty-illustration"><span>☀</span><div>✈</div></div><h3>Отдых начинается с плана</h3><p>Выберите даты в календаре —<br>и первая передышка уже близко.</p><button class="text-button" id="empty-add">Запланировать отпуск <span>↗</span></button></div>`;
  const warning = document.querySelector("#storage-warning");
  warning.hidden = !storageWarning;
  warning.textContent =
    "Сохранение в браузере недоступно или сохранённые данные повреждены. Изменения могут потеряться после закрытия страницы.";
}

function setColor(color) {
  selectedColor = color;
  document
    .querySelectorAll("[data-color]")
    .forEach((button) =>
      button.setAttribute("aria-pressed", button.dataset.color === color),
    );
}

function openEditor(trip, range) {
  editing = trip?.id || null;
  form.reset();
  document.querySelector("#dialog-title").textContent = trip
    ? "Изменить отпуск"
    : "Новый отпуск";
  document.querySelector("#trip-title").value = trip?.title || "";
  startInput.value = trip?.start || range?.[0] || selection || today;
  endInput.value = trip?.end || range?.[1] || selection || today;
  document.querySelector("#delete-trip").hidden = !trip;
  document.querySelector("#form-error").textContent = "";
  setColor(trip?.color || colors[0]);
  dialog.showModal();
}

function save(message) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(trips));
    storageWarning = false;
  } catch {
    storageWarning = true;
  }
  selection = null;
  render();
  document.querySelector("#announcement").textContent = message;
}

document.querySelector("#add").onclick = () => openEditor();
document.querySelector("#today").onclick = () => {
  visible = parseDate(today);
  render();
};
for (const [id, delta] of [
  ["previous", -1],
  ["next", 1],
]) {
  document.querySelector(`#${id}`).onclick = () => {
    visible = new Date(
      visible.getFullYear(),
      visible.getMonth() + delta,
      1,
      12,
    );
    render();
  };
}
document.querySelector("#cancel-selection").onclick = () => {
  selection = null;
  render();
};
app.addEventListener("click", (event) => {
  const tripButton = event.target.closest("[data-trip]");
  if (tripButton) {
    openEditor(trips.find((t) => t.id === tripButton.dataset.trip));
    return;
  }
  const dateButton = event.target.closest("[data-date]");
  if (dateButton) {
    if (!selection) {
      selection = dateButton.dataset.date;
      render();
    } else {
      openEditor(null, orderedRange(selection, dateButton.dataset.date));
    }
  }
  if (event.target.closest("#empty-add")) openEditor();
  const colorButton = event.target.closest("[data-color]");
  if (colorButton) setColor(colorButton.dataset.color);
});
for (const id of ["close-dialog", "cancel-dialog"])
  document.querySelector(`#${id}`).onclick = () => dialog.close();
form.onsubmit = (event) => {
  event.preventDefault();
  const title = document.querySelector("#trip-title").value.trim();
  const start = startInput.value;
  const end = endInput.value;
  if (!title || !parseDate(start) || !parseDate(end) || start > end) {
    document.querySelector("#form-error").textContent = !title
      ? "Введите название отпуска."
      : "Последний день должен быть не раньше первого.";
    return;
  }
  const trip = {
    id: editing || crypto.randomUUID(),
    title,
    start,
    end,
    color: selectedColor,
  };
  trips = editing
    ? trips.map((t) => (t.id === editing ? trip : t))
    : [...trips, trip];
  visible = parseDate(start);
  save(editing ? "Отпуск изменён" : "Отпуск добавлен");
  dialog.close();
};
const deleteDialog = document.querySelector("#delete-dialog");
document.querySelector("#delete-trip").onclick = () => {
  deleteDialog.returnValue = "";
  deleteDialog.showModal();
};
deleteDialog.addEventListener("close", () => {
  if (deleteDialog.returnValue !== "delete") return;
  trips = trips.filter((t) => t.id !== editing);
  save("Отпуск удалён");
  dialog.close();
});
window.addEventListener("storage", (event) => {
  if (event.key !== storageKey) return;
  // Avoid overwriting a form being edited with changes from another tab.
  if (!dialog.open) location.reload();
});
render();
