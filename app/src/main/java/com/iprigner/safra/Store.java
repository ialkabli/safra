package com.iprigner.safra;

import android.content.*;
import android.database.*;
import android.database.sqlite.*;
import java.time.*;
import java.util.*;

public class Store extends SQLiteOpenHelper {
  public Store(Context c) {
    this(c, "safra.db");
  }

  public Store(Context c, String name) {
    super(c, name, null, 1);
    setWriteAheadLoggingEnabled(true);
  }

  @Override
  public void onConfigure(SQLiteDatabase d) {
    d.setForeignKeyConstraintsEnabled(true);
  }

  @Override
  public void onCreate(SQLiteDatabase d) {
    d.execSQL(
        "CREATE TABLE routes(id INTEGER PRIMARY KEY,origin TEXT NOT NULL,destination TEXT NOT"
            + " NULL,depart TEXT NOT NULL,minutes INTEGER NOT NULL,price INTEGER NOT NULL"
            + " CHECK(price>0),capacity INTEGER NOT NULL CHECK(capacity>0))");
    d.execSQL(
        "CREATE TABLE bookings(id INTEGER PRIMARY KEY,route INTEGER NOT NULL REFERENCES"
            + " routes(id),day TEXT NOT NULL,name TEXT NOT NULL,people INTEGER NOT NULL"
            + " CHECK(people BETWEEN 1 AND 8),total INTEGER NOT NULL,status TEXT NOT NULL"
            + " CHECK(status IN ('active','cancelled')),token TEXT NOT NULL UNIQUE,created INTEGER"
            + " NOT NULL)");
    d.execSQL("CREATE INDEX booking_capacity ON bookings(route,day,status)");
    Object[][] r = {
      {1, "الرياض", "جدة", "09:30", 105, 34900, 8},
      {2, "الرياض", "جدة", "16:45", 110, 42900, 12},
      {3, "الرياض", "أبها", "10:20", 100, 29900, 10},
      {4, "جدة", "الرياض", "12:15", 105, 35900, 8},
      {5, "جدة", "أبها", "15:00", 70, 26900, 12},
      {6, "أبها", "الرياض", "18:00", 100, 31900, 10}
    };
    for (Object[] x : r) d.execSQL("INSERT INTO routes VALUES(?,?,?,?,?,?,?)", x);
  }

  @Override
  public void onUpgrade(SQLiteDatabase d, int old, int next) {
    throw new IllegalStateException("Migration required");
  }

  public List<Map<String, String>> rows(String sql, String... args) {
    List<Map<String, String>> out = new ArrayList<>();
    try (Cursor c = getReadableDatabase().rawQuery(sql, args)) {
      while (c.moveToNext()) {
        Map<String, String> m = new HashMap<>();
        for (String k : c.getColumnNames()) m.put(k, c.getString(c.getColumnIndexOrThrow(k)));
        out.add(m);
      }
    }
    return out;
  }

  public List<Map<String, String>> search(
      String origin, String destination, String day, int people) {
    Rules.futureDate(day, LocalDate.now());
    Rules.range(people, 1, 8);
    if (origin.equals(destination))
      throw new IllegalArgumentException("مدينة المغادرة والوصول يجب أن تختلفا");
    List<Map<String, String>> result =
        rows(
            "SELECT r.*,capacity-COALESCE((SELECT SUM(people) FROM bookings b WHERE b.route=r.id"
                + " AND b.day=? AND status='active'),0) AS available FROM routes r WHERE origin=?"
                + " AND destination=? ORDER BY price",
            day,
            origin,
            destination);
    result.removeIf(
        m ->
            Integer.parseInt(m.get("available")) < people
                || !LocalDateTime.of(LocalDate.parse(day), LocalTime.parse(m.get("depart")))
                    .isAfter(LocalDateTime.now()));
    return result;
  }

  public long book(long route, String day, String name, int people, String token) {
    name = Rules.required(name, 2, 70);
    Rules.range(people, 1, 8);
    Rules.futureDate(day, LocalDate.now());
    token = Rules.required(token, 1, 100);
    SQLiteDatabase d = getWritableDatabase();
    d.beginTransaction();
    try {
      List<Map<String, String>> old = rows("SELECT * FROM bookings WHERE token=?", token);
      if (!old.isEmpty()) {
        Map<String, String> m = old.get(0);
        if (!m.get("route").equals("" + route)
            || !m.get("day").equals(day)
            || !m.get("name").equals(name)
            || !m.get("people").equals("" + people))
          throw new IllegalArgumentException("تغيرت بيانات محاولة الحجز");
        return Long.parseLong(m.get("id"));
      }
      List<Map<String, String>> matches = rows("SELECT * FROM routes WHERE id=?", "" + route);
      if (matches.isEmpty()) throw new IllegalArgumentException("الرحلة غير موجودة");
      Map<String, String> r = matches.get(0);
      if (!LocalDateTime.of(LocalDate.parse(day), LocalTime.parse(r.get("depart")))
          .isAfter(LocalDateTime.now())) throw new IllegalArgumentException("موعد الرحلة مضى");
      int used =
          Integer.parseInt(
              rows(
                      "SELECT COALESCE(SUM(people),0) n FROM bookings WHERE route=? AND day=? AND"
                          + " status='active'",
                      "" + route,
                      day)
                  .get(0)
                  .get("n"));
      if (used + people > Integer.parseInt(r.get("capacity")))
        throw new IllegalArgumentException("المقاعد المتبقية لا تكفي");
      ContentValues v = new ContentValues();
      v.put("route", route);
      v.put("day", day);
      v.put("name", name);
      v.put("people", people);
      v.put("total", Rules.cost(Integer.parseInt(r.get("price")), people));
      v.put("status", "active");
      v.put("token", token);
      v.put("created", System.currentTimeMillis());
      long id = d.insertOrThrow("bookings", null, v);
      d.setTransactionSuccessful();
      return id;
    } finally {
      d.endTransaction();
    }
  }

  public void cancel(long id) {
    ContentValues v = new ContentValues();
    v.put("status", "cancelled");
    getWritableDatabase().update("bookings", v, "id=? AND status='active'", new String[] {"" + id});
  }

  public List<Map<String, String>> bookings() {
    return rows(
        "SELECT b.*,r.origin,r.destination,r.depart FROM bookings b JOIN routes r ON r.id=b.route"
            + " ORDER BY b.created DESC,b.id DESC");
  }
}
