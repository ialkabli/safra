package com.iprigner.safra;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Original resolution-independent artwork, drawn locally without network images. */
public class Art extends View {
  final String kind;
  final int seed;
  final Paint p = new Paint(3);

  Art(Context c, String k, int n) {
    super(c);
    kind = k;
    seed = n;
    setContentDescription(
        k.equals("travel")
            ? "رسم جبال وسماء لوجهة السفر"
            : k.equals("food") ? "رسم طبق طعام ومائدة" : "رسم تخطيطي لتنظيم الوقت");
  }

  void color(String c) {
    p.setShader(null);
    p.setColor(Color.parseColor(c));
    p.setStyle(Paint.Style.FILL);
  }

  void circle(Canvas c, float x, float y, float r, String co) {
    color(co);
    c.drawCircle(x, y, r, p);
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    c.save();
    c.scale(getWidth() / 400f, getHeight() / 220f);
    Path clip = new Path();
    clip.addRoundRect(0, 0, 400, 220, 26, 26, Path.Direction.CW);
    c.clipPath(clip);
    if (kind.equals("travel")) {
      String sky = seed % 2 == 0 ? "#B7DAF2" : "#F3D1AC";
      p.setShader(
          new LinearGradient(
              0,
              0,
              0,
              220,
              Color.parseColor(sky),
              Color.parseColor("#EDF4FF"),
              Shader.TileMode.CLAMP));
      c.drawRect(0, 0, 400, 220, p);
      circle(c, 310, 56, 27, "#FFF2C8");
      color(seed % 2 == 0 ? "#829DB0" : "#BA8B70");
      Path m = new Path();
      m.moveTo(-20, 190);
      m.lineTo(110, 45);
      m.lineTo(215, 175);
      m.lineTo(305, 83);
      m.lineTo(430, 195);
      m.close();
      c.drawPath(m, p);
      color("#426F7E");
      Path n = new Path();
      n.moveTo(-10, 220);
      n.lineTo(45, 160);
      n.lineTo(145, 200);
      n.lineTo(230, 120);
      n.lineTo(410, 205);
      n.lineTo(410, 230);
      n.close();
      c.drawPath(n, p);
      color("#234858");
      c.drawRoundRect(28, 147, 58, 220, 8, 8, p);
      c.drawRoundRect(67, 170, 86, 220, 6, 6, p);
      color("#F0E7D7");
      c.drawRect(39, 165, 46, 177, p);
      c.drawRect(39, 188, 46, 199, p);
      color("#FFFFFF");
      Path plane = new Path();
      plane.moveTo(260, 74);
      plane.lineTo(298, 62);
      plane.lineTo(281, 82);
      plane.lineTo(283, 94);
      plane.lineTo(275, 86);
      plane.lineTo(268, 90);
      plane.lineTo(270, 81);
      plane.close();
      c.drawPath(plane, p);
    } else if (kind.equals("food")) {
      color("#E9D6BE");
      c.drawRect(0, 0, 400, 220, p);
      color("#C48D65");
      c.drawRect(0, 165, 400, 220, p);
      circle(c, 200, 121, 96, "#B3A18E");
      circle(c, 200, 115, 94, "#FAF6EA");
      circle(c, 200, 115, 76, "#E7D5BA");
      circle(c, 200, 115, 64, seed % 2 == 0 ? "#C47136" : "#C9A55F");
      for (int i = 0; i < 12; i++) {
        float a = (float) (i * Math.PI / 6);
        circle(
            c,
            200 + (float) Math.cos(a) * 43,
            115 + (float) Math.sin(a) * 39,
            9,
            i % 3 == 0 ? "#AE3D2B" : i % 3 == 1 ? "#3D7345" : "#F2D189");
      }
      color("#FFF4D2");
      c.drawRoundRect(174, 86, 224, 144, 12, 12, p);
      color("#D49B52");
      p.setStrokeWidth(4);
      for (int i = 0; i < 4; i++) c.drawLine(179, 96 + i * 11, 219, 96 + i * 11, p);
      circle(c, 344, 55, 25, "#3C6442");
      circle(c, 337, 48, 12, "#699357");
      color("#F5EAD8");
      c.drawRoundRect(33, 35, 40, 169, 3, 3, p);
      c.drawRoundRect(357, 115, 365, 203, 3, 3, p);
    } else {
      color("#E9E2FA");
      c.drawRect(0, 0, 400, 220, p);
      circle(c, 330, 28, 92, "#D8CBF4");
      circle(c, 30, 216, 90, "#D8F0E8");
      color("#FFFFFF");
      c.save();
      c.rotate(-8, 200, 110);
      c.drawRoundRect(111, 26, 289, 200, 17, 17, p);
      color("#7054CB");
      c.drawRoundRect(129, 46, 269, 70, 7, 7, p);
      for (int i = 0; i < 3; i++) {
        circle(c, 143, 99 + i * 34, 9, "#D8F0E8");
        color("#CBC2DF");
        c.drawRoundRect(163, 94 + i * 34, 252 - i * 10, 101 + i * 34, 3, 3, p);
      }
      c.restore();
      circle(c, 298, 163, 32, "#7054CB");
      color("#FFFFFF");
      p.setStrokeWidth(5);
      p.setStyle(Paint.Style.STROKE);
      c.drawLine(284, 164, 294, 174, p);
      c.drawLine(294, 174, 313, 151, p);
      p.setStyle(Paint.Style.FILL);
    }
    c.restore();
  }
}
