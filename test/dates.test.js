import { test } from "node:test";
import assert from "node:assert/strict";
import {
  parseDate,
  daysBetween,
  orderedRange,
  totalDays,
} from "../src/dates.js";

test("dates validate leap years and invalid calendar days", () => {
  assert.ok(parseDate("2028-02-29"));
  assert.equal(parseDate("2027-02-29"), null);
  assert.equal(parseDate("2026-04-31"), null);
});
test("vacation includes both endpoints across months, years and DST", () => {
  assert.equal(daysBetween("2026-10-20", "2026-10-20"), 1);
  assert.equal(daysBetween("2026-12-30", "2027-01-02"), 4);
  assert.equal(daysBetween("2026-03-28", "2026-03-30"), 3);
  assert.equal(daysBetween("2026-10-22", "2026-10-20"), 0);
});
test("reverse calendar selection orders the dates", () => {
  assert.deepEqual(orderedRange("2026-11-01", "2026-10-29"), [
    "2026-10-29",
    "2026-11-01",
  ]);
});
test("year total clips cross-year ranges and counts overlapping days once", () => {
  assert.equal(
    totalDays(
      [
        { start: "2025-12-29", end: "2026-01-03" },
        { start: "2026-01-02", end: "2026-01-05" },
        { start: "2026-12-31", end: "2027-01-03" },
      ],
      2026,
    ),
    6,
  );
});
