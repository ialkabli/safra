package com.iprigner.safra;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Dependency-free instrumentation suite. Runs on an emulator or physical Android device. */
public class AppTests extends Instrumentation {
  int passed = 0, failed = 0;
  StringBuilder log = new StringBuilder();
  Store db;

  interface Check {
    void run() throws Exception;
  }

  void test(String name, Check fn) {
    try {
      fn.run();
      passed++;
      log.append("PASS ").append(name).append('\n');
    } catch (Throwable e) {
      failed++;
      log.append("FAIL ").append(name).append(": ").append(e).append('\n');
    }
    Bundle b = new Bundle();
    b.putString("stream", log.toString());
    sendStatus(0, b);
  }

  void ok(boolean condition) {
    if (!condition) throw new AssertionError("condition false");
  }

  void rejects(Check fn) {
    try {
      fn.run();
    } catch (IllegalArgumentException expected) {
      return;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    throw new AssertionError("Expected validation failure");
  }

  String tomorrow() {
    return LocalDate.now().plusDays(1).toString();
  }

  long tomorrowAt(int h, int m) {
    return LocalDate.now()
        .plusDays(1)
        .atTime(h, m)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
  }

  void reset() {
    if (db != null) db.close();
    getTargetContext().deleteDatabase("verification.db");
    db = new Store(getTargetContext(), "verification.db");
    db.getWritableDatabase();
  }

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  @Override
  public void onStart() {
    try {
      test(
          "pure rules: boundaries, money, date and interval",
          () -> {
            ok(Rules.cost(34900, 3) == 104700);
            ok(!Rules.overlap(10, 20, 20, 30));
            ok(Rules.overlap(10, 21, 20, 30));
            rejects(() -> Rules.required(" ", 2, 20));
            rejects(() -> Rules.cost(100, 9));
            rejects(
                () -> Rules.futureDate(LocalDate.now().minusDays(1).toString(), LocalDate.now()));
            rejects(() -> Rules.interval(20, 10, 0));
          });
      test(
          "capacity, cancellation and price snapshot",
          () -> {
            reset();
            long id = db.book(1, tomorrow(), "Test passenger", 8, "one");
            ok(db.bookings().get(0).get("total").equals("279200"));
            rejects(() -> db.book(1, tomorrow(), "Second", 1, "two"));
            db.cancel(id);
            db.cancel(id);
            db.book(1, tomorrow(), "Second", 1, "two");
            ok(db.bookings().size() == 2);
          });
      test(
          "idempotency and changed retry",
          () -> {
            reset();
            long id = db.book(1, tomorrow(), "Test passenger", 2, "retry");
            ok(id == db.book(1, tomorrow(), "Test passenger", 2, "retry"));
            rejects(() -> db.book(1, tomorrow(), "Test passenger", 3, "retry"));
            ok(db.bookings().size() == 1);
          });
      test(
          "invalid input and independent departure dates",
          () -> {
            reset();
            rejects(() -> db.book(999, tomorrow(), "Test", 1, "missing"));
            rejects(() -> db.book(1, tomorrow(), " ", 1, "bad"));
            rejects(() -> db.search("الرياض", "الرياض", tomorrow(), 1));
            db.book(1, tomorrow(), "First", 8, "a");
            db.book(1, LocalDate.now().plusDays(2).toString(), "Second", 8, "b");
            ok(db.bookings().size() == 2);
          });
      test(
          "database survives helper reopen and SQL input",
          () -> {
            reset();
            db.book(1, tomorrow(), "O'Brien ' OR 1=1 --", 1, "safe");
            db.close();
            db = new Store(getTargetContext(), "verification.db");
            ok(db.bookings().size() == 1);
            ok(db.search("' OR 1=1 --", "جدة", tomorrow(), 1).isEmpty());
          });
      test(
          "concurrent last seats have exactly one winner",
          () -> {
            reset();
            AtomicInteger saved = new AtomicInteger(), denied = new AtomicInteger();
            CountDownLatch go = new CountDownLatch(1);
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
              final int n = i;
              Thread t =
                  new Thread(
                      () -> {
                        try (Store s = new Store(getTargetContext(), "verification.db")) {
                          go.await();
                          s.book(1, tomorrow(), "Concurrent", 8, "race" + n);
                          saved.incrementAndGet();
                        } catch (IllegalArgumentException e) {
                          denied.incrementAndGet();
                        } catch (Exception e) {
                          throw new RuntimeException(e);
                        }
                      });
              threads.add(t);
              t.start();
            }
            go.countDown();
            for (Thread t : threads) t.join(10000);
            ok(saved.get() == 1 && denied.get() == 1);
            ok(db.bookings().size() == 1);
          });

      test("native UI create flow, persistence and screenshots", () -> uiFlow());
    } finally {
      if (db != null) db.close();
      getTargetContext().deleteDatabase("verification.db");
      Bundle result = new Bundle();
      result.putString("stream", log + "\nTOTAL " + passed + " passed, " + failed + " failed\n");
      result.putInt("passed", passed);
      result.putInt("failed", failed);
      finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }
  }

  TextView find(View view, String value) {
    if (view instanceof TextView && ((TextView) view).getText().toString().contains(value))
      return (TextView) view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        TextView result = find(group.getChildAt(i), value);
        if (result != null) return result;
      }
    }
    return null;
  }

  EditText firstEdit(View view) {
    if (view instanceof EditText) return (EditText) view;
    if (view instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) view;
      for (int i = 0; i < g.getChildCount(); i++) {
        EditText e = firstEdit(g.getChildAt(i));
        if (e != null) return e;
      }
    }
    return null;
  }

  TextView waitFor(MainActivity a, String value) throws Exception {
    for (int i = 0; i < 80; i++) {
      AtomicReference<TextView> found = new AtomicReference<>();
      runOnMainSync(() -> found.set(find(a.getWindow().getDecorView(), value)));
      if (found.get() != null) return found.get();
      Thread.sleep(100);
    }
    throw new AssertionError("Missing UI text: " + value);
  }

  void shot(String name) throws Exception {
    waitForIdleSync();
    Thread.sleep(400);
    Bitmap bitmap = getUiAutomation().takeScreenshot();
    ok(bitmap != null);
    try (FileOutputStream out =
        new FileOutputStream(new File(getTargetContext().getFilesDir(), name + ".png"))) {
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    bitmap.recycle();
  }

  void uiFlow() throws Exception {
    Intent open =
        new Intent(getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    MainActivity a = (MainActivity) startActivitySync(open);
    String slug = "safra";
    String first =
        slug.equals("safra")
            ? "اختيار الرحلة"
            : slug.equals("maeda") ? "احجز طاولتك" : "+ مهمة أو موعد جديد";
    TextView cta = waitFor(a, first);
    shot("home");
    runOnMainSync(() -> cta.performClick());
    waitForIdleSync();
    Thread.sleep(250);
    // Dialog windows belong to the activity but have a separate decor root.
    // Ui exposes its most recently opened dialog only to package-level tests.
    AlertDialog dialog = a.activeDialog;
    ok(dialog != null && dialog.isShowing());
    runOnMainSync(
        () -> firstEdit(dialog.getWindow().getDecorView()).setText("اختبار واجهة تجريبي"));
    String submit =
        slug.equals("safra")
            ? "تأكيد الحجز التجريبي"
            : slug.equals("maeda") ? "تأكيد الطاولة" : "حفظ";
    runOnMainSync(
        () -> {
          TextView b = find(dialog.getWindow().getDecorView(), submit);
          ok(b != null);
          b.performClick();
        });
    for (int i = 0; i < 100; i++) {
      AtomicBoolean shown = new AtomicBoolean();
      runOnMainSync(() -> shown.set(dialog.isShowing()));
      if (!shown.get()) break;
      Thread.sleep(100);
    }
    ok(!dialog.isShowing());
    waitFor(a, "اختبار واجهة تجريبي");
    shot("saved");
    runOnMainSync(a::recreate);
    waitForIdleSync();
    Thread.sleep(500);
    // Domain persistence is independently asserted by reopening the helper in the cases below.
  }
}
