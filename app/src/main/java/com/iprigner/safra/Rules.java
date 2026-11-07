package com.iprigner.safra;

import java.time.*;

/** Pure domain validation. No Android dependency. Money is stored as integer halalas. */
public final class Rules {
  private Rules() {}

  public static String required(String value, int min, int max) {
    String s = value == null ? "" : value.trim();
    if (s.length() < min || s.length() > max)
      throw new IllegalArgumentException("أدخل نصًا من " + min + " إلى " + max + " حرفًا");
    return s;
  }

  public static int range(int value, int min, int max) {
    if (value < min || value > max)
      throw new IllegalArgumentException("العدد يجب أن يكون بين " + min + " و" + max);
    return value;
  }

  public static LocalDate futureDate(String day, LocalDate today) {
    LocalDate d = LocalDate.parse(day);
    if (d.isBefore(today) || d.isAfter(today.plusDays(180)))
      throw new IllegalArgumentException("اختر تاريخًا خلال الأشهر الستة القادمة");
    return d;
  }

  public static boolean overlap(long a, long b, long c, long d) {
    return a < d && b > c;
  }

  public static void interval(long start, long end, long now) {
    if (start <= now || end <= start || end - start > 86400000L)
      throw new IllegalArgumentException("اختر وقتًا قادمًا ومدة لا تتجاوز يومًا");
  }

  public static long cost(int perPerson, int people) {
    range(people, 1, 8);
    if (perPerson < 0) throw new IllegalArgumentException("سعر غير صالح");
    return Math.multiplyExact((long) perPerson, people);
  }

  public static String money(long halalas) {
    return String.format(java.util.Locale.US, "%.2f", halalas / 100.0) + " ر.س";
  }
}
