export function isoDate(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function parseDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const [year, month, day] = value.split("-").map(Number);
  const date = new Date(year, month - 1, day, 12);
  return isoDate(date) === value ? date : null;
}

export function daysBetween(start, end) {
  const a = parseDate(start);
  const b = parseDate(end);
  if (!a || !b || start > end) return 0;
  return (
    Math.round(
      (Date.UTC(b.getFullYear(), b.getMonth(), b.getDate()) -
        Date.UTC(a.getFullYear(), a.getMonth(), a.getDate())) /
        86400000,
    ) + 1
  );
}

export function orderedRange(a, b) {
  return a <= b ? [a, b] : [b, a];
}

export function totalDays(trips, year) {
  const dates = new Set();
  for (const trip of trips) {
    const start = trip.start < `${year}-01-01` ? `${year}-01-01` : trip.start;
    const end = trip.end > `${year}-12-31` ? `${year}-12-31` : trip.end;
    if (start > end) continue;
    const date = parseDate(start);
    while (isoDate(date) <= end) {
      dates.add(isoDate(date));
      date.setDate(date.getDate() + 1);
    }
  }
  return dates.size;
}
