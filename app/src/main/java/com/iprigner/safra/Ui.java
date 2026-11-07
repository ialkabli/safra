package com.iprigner.safra;

import android.app.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Small native UI toolkit shared in source; each application builds independently. */
public abstract class Ui extends Activity {
  public final int accent = Color.parseColor("#2463EB"),
      bg = Color.parseColor("#EEF4FF"),
      ink = Color.parseColor("#142A50"),
      muted = Color.parseColor("#788191");
  protected LinearLayout root, body, nav;
  protected int tab = 0;
  protected Typeface regular, bold;
  protected final ExecutorService io = Executors.newSingleThreadExecutor();
  protected boolean closed = false;
  AlertDialog activeDialog;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    regular = Typeface.createFromAsset(getAssets(), "font.ttf");
    bold = Typeface.createFromAsset(getAssets(), "font-bold.ttf");
    if (state != null) tab = state.getInt("tab", 0);
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putInt("tab", tab);
  }

  @Override
  protected void onDestroy() {
    closed = true;
    io.shutdown();
    super.onDestroy();
  }

  public int dp(float x) {
    return (int) (getResources().getDisplayMetrics().density * x + .5f);
  }

  public GradientDrawable shape(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  protected TextView text(String s, int size, int color, boolean heavy) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(heavy ? bold : regular);
    t.setGravity(Gravity.START);
    t.setLineSpacing(dp(3), 1);
    return t;
  }

  protected LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(1);
    return l;
  }

  protected LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setGravity(Gravity.CENTER_VERTICAL);
    return l;
  }

  protected void gap(LinearLayout l, int h) {
    View v = new View(this);
    l.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
  }

  protected void add(LinearLayout l, View v) {
    l.addView(v, new LinearLayout.LayoutParams(-1, -2));
  }

  protected void weighted(LinearLayout l, View v) {
    l.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
  }

  protected void shell(String[] items) {
    root = column();
    root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    root.setBackgroundColor(bg);
    root.setOnApplyWindowInsetsListener(
        (v, in) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bar =
                in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(bar.left, bar.top, bar.right, bar.bottom);
          } else {
            v.setPadding(
                in.getSystemWindowInsetLeft(),
                in.getSystemWindowInsetTop(),
                in.getSystemWindowInsetRight(),
                in.getSystemWindowInsetBottom());
          }
          return in;
        });
    LinearLayout head = row();
    head.setPadding(dp(22), dp(13), dp(22), dp(12));
    LinearLayout word = column();
    add(word, text("سَفرة", 27, ink, true));
    add(word, text("TRAVEL, YOUR WAY", 9, muted, false));
    weighted(head, word);
    TextView badge =
        text("safra".equals("safra") ? "✈" : "safra".equals("maeda") ? "M" : "W", 23, accent, true);
    badge.setGravity(Gravity.CENTER);
    badge.setBackground(shape(Color.WHITE, 18));
    head.addView(badge, new LinearLayout.LayoutParams(dp(48), dp(48)));
    add(root, head);
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setClipToPadding(false);
    body = column();
    body.setPadding(dp(20), dp(8), dp(20), dp(24));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    nav = row();
    nav.setBackgroundColor(Color.WHITE);
    nav.setPadding(dp(10), dp(8), dp(10), dp(8));
    for (int i = 0; i < items.length; i++) {
      final int n = i;
      TextView b = text(items[i], 13, i == tab ? accent : muted, i == tab);
      b.setGravity(Gravity.CENTER);
      b.setPadding(dp(2), dp(14), dp(2), dp(14));
      if (i == tab) b.setBackground(shape(bg, 16));
      b.setOnClickListener(
          v -> {
            tab = n;
            refresh();
          });
      weighted(nav, b);
    }
    add(root, nav);
    setContentView(root);
  }

  protected abstract void refresh();

  protected LinearLayout card() {
    LinearLayout c = column();
    c.setPadding(dp(18), dp(18), dp(18), dp(18));
    c.setBackground(shape(Color.WHITE, 24));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(14);
    body.addView(c, p);
    return c;
  }

  protected void heading(String s, String sub) {
    add(body, text(s, 29, ink, true));
    if (sub != null) add(body, text(sub, 13, muted, false));
    gap(body, 20);
  }

  protected TextView button(String label, Runnable action) {
    TextView b = text(label, 15, Color.WHITE, true);
    b.setGravity(Gravity.CENTER);
    b.setPadding(dp(15), dp(15), dp(15), dp(15));
    b.setBackground(shape(accent, 16));
    b.setMinHeight(dp(50));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  protected TextView link(String label, Runnable action) {
    TextView b = text(label, 14, accent, true);
    b.setPadding(0, dp(13), 0, dp(9));
    b.setMinHeight(dp(48));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  protected EditText input(LinearLayout form, String label, String value) {
    add(form, text(label, 13, muted, false));
    EditText e = new EditText(this);
    e.setText(value);
    e.setTextColor(ink);
    e.setTextSize(16);
    e.setTypeface(regular);
    e.setSingleLine(true);
    e.setPadding(dp(12), dp(8), dp(12), dp(8));
    e.setBackground(shape(bg, 12));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
    p.bottomMargin = dp(13);
    form.addView(e, p);
    return e;
  }

  protected Spinner select(LinearLayout form, String label, String[] options) {
    add(form, text(label, 13, muted, false));
    Spinner s = new Spinner(this);
    ArrayAdapter<String> a =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
    s.setAdapter(a);
    form.addView(s, new LinearLayout.LayoutParams(-1, dp(50)));
    gap(form, 10);
    return s;
  }

  protected TextView date(LinearLayout form, String label, LocalDate[] state) {
    add(form, text(label, 13, muted, false));
    TextView t = link(state[0].toString(), () -> {});
    t.setOnClickListener(
        v ->
            new DatePickerDialog(
                    this,
                    (d, y, m, day) -> {
                      state[0] = LocalDate.of(y, m + 1, day);
                      t.setText(state[0].toString());
                    },
                    state[0].getYear(),
                    state[0].getMonthValue() - 1,
                    state[0].getDayOfMonth())
                .show());
    add(form, t);
    return t;
  }

  protected TextView time(LinearLayout form, String label, LocalTime[] state) {
    add(form, text(label, 13, muted, false));
    TextView t = link(state[0].format(DateTimeFormatter.ofPattern("HH:mm")), () -> {});
    t.setOnClickListener(
        v ->
            new TimePickerDialog(
                    this,
                    (d, h, m) -> {
                      state[0] = LocalTime.of(h, m);
                      t.setText(state[0].format(DateTimeFormatter.ofPattern("HH:mm")));
                    },
                    state[0].getHour(),
                    state[0].getMinute(),
                    true)
                .show());
    add(form, t);
    return t;
  }

  protected LinearLayout form() {
    LinearLayout l = column();
    l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    l.setPadding(dp(22), dp(12), dp(22), dp(16));
    return l;
  }

  protected AlertDialog dialog(String title, LinearLayout form) {
    ScrollView s = new ScrollView(this);
    s.addView(form);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setView(s)
            .setNegativeButton("رجوع", null)
            .create();
    d.setOnShowListener(
        v -> {
          d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        });
    activeDialog = d;
    d.show();
    return d;
  }

  protected void submit(AlertDialog d, TextView b, Callable<?> job, Runnable after) {
    b.setEnabled(false);
    String old = b.getText().toString();
    b.setText("جارٍ الحفظ…");
    io.execute(
        () -> {
          try {
            job.call();
            runOnUiThread(
                () -> {
                  if (!closed) {
                    d.dismiss();
                    after.run();
                  }
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!closed) {
                    b.setEnabled(true);
                    b.setText(old);
                    new AlertDialog.Builder(this)
                        .setTitle("راجع البيانات")
                        .setMessage(e.getMessage())
                        .setPositiveButton("حسنًا", null)
                        .show();
                  }
                });
          }
        });
  }

  protected void load(Callable<Runnable> job) {
    io.execute(
        () -> {
          try {
            Runnable render = job.call();
            runOnUiThread(
                () -> {
                  if (!closed) render.run();
                });
          } catch (Exception e) {
            runOnUiThread(
                () -> {
                  if (!closed)
                    new AlertDialog.Builder(this)
                        .setMessage(e.getMessage())
                        .setPositiveButton("حسنًا", null)
                        .show();
                });
          }
        });
  }

  protected void confirm(String message, Callable<?> action) {
    new AlertDialog.Builder(this)
        .setTitle("تأكيد العملية")
        .setMessage(message)
        .setNegativeButton("رجوع", null)
        .setPositiveButton(
            "تأكيد",
            (d, w) ->
                load(
                    () -> {
                      action.call();
                      return this::refresh;
                    }))
        .show();
  }

  protected void about(String description) {
    heading("مصمّم ليكون بسيطًا.", "مشروع أندرويد تجريبي · عبدالمجيد حسن كابلي");
    LinearLayout c = card();
    add(c, text(description, 16, ink, false));
    gap(c, 16);
    add(
        c,
        text(
            "البيانات محفوظة على هذا الجهاز فقط. لا حسابات أو دفع أو ربط بمزوّد خارجي. حذف بيانات"
                + " التطبيق يحذف سجلاتك.\n\n"
                + "الشيفرة والاختبارات موثقة للعرض على GitHub.\n"
                + "@iprigner",
            14,
            muted,
            false));
  }

  protected void empty(String title) {
    LinearLayout c = card();
    add(c, text(title, 19, ink, true));
    add(c, text("ستظهر التفاصيل هنا بعد الإضافة.", 14, muted, false));
  }

  protected void art(String kind, int height, int seed) {
    Art a = new Art(this, kind, seed);
    body.addView(a, new LinearLayout.LayoutParams(-1, dp(height)));
    gap(body, 18);
  }
}
