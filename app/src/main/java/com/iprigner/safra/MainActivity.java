package com.iprigner.safra;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.time.*;
import java.util.*;

public class MainActivity extends Ui {
  Store db;
  String origin = "الرياض", destination = "جدة";
  LocalDate day = LocalDate.now().plusDays(1);
  int people = 1;

  @Override
  protected void onDestroy() {
    io.execute(() -> db.close());
    super.onDestroy();
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    db = new Store(this);
    if (state != null) {
      origin = state.getString("origin", origin);
      destination = state.getString("destination", destination);
      day = LocalDate.parse(state.getString("day", day.toString()));
      people = state.getInt("people", 1);
    }
    refresh();
  }

  @Override
  protected void onSaveInstanceState(Bundle s) {
    super.onSaveInstanceState(s);
    s.putString("origin", origin);
    s.putString("destination", destination);
    s.putString("day", day.toString());
    s.putInt("people", people);
  }

  @Override
  protected void refresh() {
    final int requested = tab;
    if (tab == 0)
      load(
          () -> {
            List<Map<String, String>> routes =
                db.search(origin, destination, day.toString(), people);
            return () -> {
              if (tab != requested) return;
              shell(new String[] {"استكشف", "حجوزاتي", "عن سَفرة"});
              home(routes);
            };
          });
    else if (tab == 1)
      load(
          () -> {
            List<Map<String, String>> list = db.bookings();
            return () -> {
              if (tab != requested) return;
              shell(new String[] {"استكشف", "حجوزاتي", "عن سَفرة"});
              bookings(list);
            };
          });
    else {
      shell(new String[] {"استكشف", "حجوزاتي", "عن سَفرة"});
      about(
          "سَفرة يحوّل البحث عن الرحلات إلى تجربة بسيطة. المدن والجداول والأسعار والمقاعد بيانات"
              + " تجريبية محلية، وليست عروضًا من شركات طيران. تأكيد التطبيق لا يصدر تذكرة سفر"
              + " فعلية.\n\n"
              + "الحجز يحتسب جميع المسافرين ويحفظ الإجمالي، ويمنع تجاوز المقاعد المتاحة على هذا"
              + " الجهاز.");
    }
  }

  void home(List<Map<String, String>> list) {
    heading("وين ودّك تسافر؟", "وجهات قريبة، وذكريات تستاهل.");
    art("travel", 175, destination.equals("أبها") ? 0 : 1);
    LinearLayout search = card();
    add(search, text(origin + "  ←  " + destination, 23, ink, true));
    add(search, text(day + "  ·  " + people + " مسافر", 14, muted, false));
    gap(search, 12);
    add(search, button("تعديل البحث", this::searchForm));
    add(body, text("رحلات تناسب خطتك", 22, ink, true));
    gap(body, 6);
    add(body, text("أسعار تجريبية · رحلة ذهاب فقط · الدرجة الاقتصادية", 12, muted, false));
    gap(body, 14);
    if (list.isEmpty()) empty("ما فيه رحلات متاحة لهذه الخيارات");
    for (Map<String, String> r : list) {
      LinearLayout c = card();
      LinearLayout top = row();
      weighted(top, text(r.get("depart"), 25, ink, true));
      weighted(
          top,
          text(
              Rules.money(Rules.cost(Integer.parseInt(r.get("price")), people)), 21, accent, true));
      add(c, top);
      add(c, text(r.get("origin") + "  ←  " + r.get("destination"), 16, ink, true));
      add(
          c,
          text(
              "مباشرة · " + r.get("minutes") + " دقيقة · " + r.get("available") + " مقاعد متاحة",
              13,
              muted,
              false));
      gap(c, 12);
      add(c, button("اختيار الرحلة", () -> bookingForm(r)));
    }
  }

  void searchForm() {
    LinearLayout f = form();
    String[] cities = {"الرياض", "جدة", "أبها"};
    Spinner from = select(f, "المغادرة", cities), to = select(f, "الوصول", cities);
    from.setSelection(Arrays.asList(cities).indexOf(origin));
    to.setSelection(Arrays.asList(cities).indexOf(destination));
    LocalDate[] d = {day};
    date(f, "تاريخ السفر", d);
    Spinner n = select(f, "المسافرون", new String[] {"1", "2", "3", "4", "5", "6", "7", "8"});
    n.setSelection(people - 1);
    TextView b = button("عرض الرحلات", () -> {});
    add(f, b);
    AlertDialog dialog = dialog("خطّط لرحلتك", f);
    b.setOnClickListener(
        v -> {
          String a = from.getSelectedItem().toString(), z = to.getSelectedItem().toString();
          try {
            Rules.futureDate(d[0].toString(), LocalDate.now());
            if (a.equals(z)) throw new IllegalArgumentException("اختر مدينتين مختلفتين");
            origin = a;
            destination = z;
            day = d[0];
            people = n.getSelectedItemPosition() + 1;
            dialog.dismiss();
            refresh();
          } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
          }
        });
  }

  void bookingForm(Map<String, String> r) {
    final String selectedDay = day.toString();
    final int count = people;
    final String token = UUID.randomUUID().toString();
    LinearLayout f = form();
    add(f, text(r.get("origin") + " ← " + r.get("destination"), 23, ink, true));
    add(
        f,
        text(selectedDay + " · " + r.get("depart") + " · " + count + " مسافر", 14, muted, false));
    gap(f, 15);
    EditText name = input(f, "اسم صاحب الحجز", "");
    add(
        f,
        text(
            "الإجمالي: " + Rules.money(Rules.cost(Integer.parseInt(r.get("price")), count)),
            23,
            accent,
            true));
    add(f, text("هذا حجز تجريبي محلي، لا يتطلب دفعًا ولا يصدر تذكرة.", 13, muted, false));
    gap(f, 16);
    TextView b = button("تأكيد الحجز التجريبي", () -> {});
    add(f, b);
    AlertDialog dialog = dialog("تفاصيل رحلتك", f);
    b.setOnClickListener(
        v -> {
          String entered = name.getText().toString();
          submit(
              dialog,
              b,
              () -> db.book(Long.parseLong(r.get("id")), selectedDay, entered, count, token),
              () -> {
                tab = 1;
                refresh();
              });
        });
  }

  void bookings(List<Map<String, String>> list) {
    heading("كل رحلاتك، هنا.", "تأكيدات محلية محفوظة على جهازك.");
    if (list.isEmpty()) empty("ابدأ بحجز رحلتك الأولى");
    for (Map<String, String> b : list) {
      boolean active = b.get("status").equals("active");
      LinearLayout c = card();
      add(
          c,
          text(
              "SF-" + String.format(Locale.US, "%05d", Long.parseLong(b.get("id"))),
              12,
              accent,
              true));
      add(c, text(b.get("origin") + " ← " + b.get("destination"), 23, ink, true));
      add(c, text(b.get("day") + " · " + b.get("depart"), 16, ink, false));
      add(c, text(b.get("name") + " · " + b.get("people") + " مسافر", 14, muted, false));
      add(c, text(Rules.money(Long.parseLong(b.get("total"))), 21, accent, true));
      add(c, text(active ? "مؤكد تجريبيًا" : "ملغى", 14, active ? accent : muted, true));
      if (active)
        add(
            c,
            link(
                "إلغاء الحجز",
                () ->
                    confirm(
                        "إلغاء هذا الحجز وإعادة المقاعد للتوفر؟",
                        () -> {
                          db.cancel(Long.parseLong(b.get("id")));
                          return null;
                        })));
    }
  }
}
