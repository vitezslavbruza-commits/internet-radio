package cz.slvikblue.radio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private LinearLayout stations;
    private TextView status;
    private final int bg = Color.rgb(9,13,19), panel = Color.rgb(20,30,39), teal = Color.rgb(0,201,167), gold = Color.rgb(255,180,92);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22,28,22,24); root.setBackgroundColor(bg); scroll.addView(root);
        TextView brand = new TextView(this); brand.setText("INTERNET RADIO"); brand.setTextColor(teal); brand.setTextSize(13); brand.setLetterSpacing(.18f); brand.setTypeface(null,Typeface.BOLD); root.addView(brand);
        TextView title = new TextView(this); title.setText("Rádio do kapsy"); title.setTextColor(Color.WHITE); title.setTextSize(29); title.setTypeface(null,Typeface.BOLD); title.setPadding(0,12,0,4); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("Živé stanice z Česka i celého světa"); subtitle.setTextColor(0xffb9c7d4); subtitle.setTextSize(14); root.addView(subtitle);
        status = new TextView(this); status.setText("Vyber stanici a začni poslouchat."); status.setTextColor(gold); status.setTextSize(14); status.setPadding(0,22,0,14); root.addView(status);
        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL);
        Button stop = button("ZASTAVIT"); stop.setOnClickListener(v -> { startService(new Intent(this,RadioService.class).setAction(RadioService.STOP)); status.setText("Přehrávání zastaveno."); });
        Button reload = button("OBNOVIT STANICE"); reload.setOnClickListener(v -> loadStations()); controls.addView(stop,new LinearLayout.LayoutParams(0,-2,1)); controls.addView(reload,new LinearLayout.LayoutParams(0,-2,1)); root.addView(controls);
        TextView heading = new TextView(this); heading.setText("STANICE"); heading.setTextColor(teal); heading.setTextSize(12); heading.setTypeface(null,Typeface.BOLD); heading.setPadding(0,26,0,12); root.addView(heading);
        stations = new LinearLayout(this); stations.setOrientation(LinearLayout.VERTICAL); root.addView(stations);
        setContentView(scroll); loadStations();
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextSize(11); b.setTextColor(Color.BLACK); b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(teal)); return b;
    }
    private void loadStations() {
        stations.removeAllViews();
        TextView loading = new TextView(this); loading.setText("Načítám české stanice…"); loading.setTextColor(Color.WHITE); stations.addView(loading);
        new Thread(() -> {
            JSONArray arr = new JSONArray(); String error = null;
            try {
                URL u = new URL("https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/CZ?limit=40&order=clickcount&reverse=true&hidebroken=true");
                HttpURLConnection c = (HttpURLConnection)u.openConnection(); c.setConnectTimeout(10000); c.setReadTimeout(12000); c.setRequestProperty("User-Agent","SlvikblueInternetRadio/1.0");
                try { java.io.InputStream in = c.getInputStream();
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    arr = new JSONArray(new String(out.toByteArray(), "UTF-8"));
                    in.close(); } finally { c.disconnect(); }
            } catch(Exception e) { error = e.getMessage(); }
            final JSONArray result=arr; final String err=error;
            runOnUiThread(() -> {
                stations.removeAllViews();
                if (result.length()==0) {
                    TextView msg = new TextView(this); msg.setText("Stanice se nepodařilo načíst. Zkontroluj internet a stiskni OBNOVIT STANICE."); msg.setTextColor(Color.WHITE); stations.addView(msg); return;
                }
                for(int i=0;i<result.length();i++) {
                    JSONObject o=result.optJSONObject(i); if(o==null) continue;
                    String name=o.optString("name","Radio stanice"), stream=o.optString("url_resolved",o.optString("url",""));
                    String country=o.optString("country",""); if(stream.isEmpty()) continue;
                    LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(14,10,14,10); android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                    shape.setColor(panel); shape.setCornerRadius(18); shape.setStroke(1,0xff29404b);
                    card.setBackground(shape);
                    TextView n=new TextView(this); n.setText(name); n.setTextColor(Color.WHITE); n.setTextSize(16); n.setTypeface(null,Typeface.BOLD); card.addView(n);
                    TextView c=new TextView(this); c.setText(country.isEmpty()?"Česká stanice":country); c.setTextColor(0xff9aabba); c.setTextSize(11); card.addView(c);
                    Button play=button("▶  PŘEHRÁT"); play.setOnClickListener(v -> {
                        Intent in=new Intent(this,RadioService.class).setAction(RadioService.PLAY).putExtra("url",stream).putExtra("name",name);
                        if(android.os.Build.VERSION.SDK_INT>=26) startForegroundService(in); else startService(in);
                        status.setText("Spouštím: "+name+"  •  ovládání najdeš v oznámeních.");
                    }); card.addView(play);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.bottomMargin=10; stations.addView(card,lp);
                }
            });
        }).start();
    }
}
