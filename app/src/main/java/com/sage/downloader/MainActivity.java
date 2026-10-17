package com.sage.downloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.os.Environment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final String BACKEND_URL = "https://sage-backend-fnrx.onrender.com";
    final int bg = Color.rgb(7, 12, 9);
    final int card = Color.rgb(15, 23, 18);
    final int green = Color.rgb(174, 224, 177);
    final int muted = Color.rgb(151, 169, 155);
    final int white = Color.rgb(239, 245, 239);

    EditText link;
    TextView status;
    LinearLayout root, content;
    String currentTab = "HOME";
    long lastDownloadId = -1L;

    int dp(float n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    GradientDrawable shape(int color, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    GradientDrawable glassShape(int radius) {
        GradientDrawable d = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {
                        Color.argb(235, 29, 49, 37),
                        Color.argb(218, 12, 24, 17),
                        Color.argb(232, 21, 37, 27)
                });
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.argb(165, 166, 225, 178));
        return d;
    }

    TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setFontFeatureSettings("kern");
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    TextView label(String value, int size, int color) {
        TextView t = text(value, size, color);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return t;
    }

    LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    void pad(View v, int l, int t, int r, int b) {
        v.setPadding(dp(l), dp(t), dp(r), dp(b));
    }

    void animate(View v) {
        ScaleAnimation pop = new ScaleAnimation(
                0.94f, 1f, 0.94f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        pop.setDuration(180);
        pop.setInterpolator(new android.view.animation.DecelerateInterpolator());
        v.startAnimation(pop);
    }

    TextView action(String value, boolean primary, Runnable run) {
        TextView b = label(value, 15, primary ? bg : green);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(15), dp(16), dp(15));
        android.graphics.drawable.Drawable base = shape(
                primary ? green : card,
                primary ? green : Color.rgb(48, 70, 52), 22);
        android.content.res.ColorStateList rippleColor =
                android.content.res.ColorStateList.valueOf(
                        Color.argb(65, 170, 255, 195));
        b.setBackground(new android.graphics.drawable.RippleDrawable(
                rippleColor, base, null));
        b.setElevation(primary ? dp(5) : 0);
        b.setOnClickListener(v -> {
            animate(v);
            run.run();
        });
        return b;
    }

    LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(
                w < 0 ? w : dp(w), h < 0 ? h : dp(h));
    }

    void gap(LinearLayout parent, int h) {
        space(parent, h);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showPage("HOME");
        handleShare(getIntent());
    }


    private float swipeStartX, swipeStartY;

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            swipeStartX = event.getX();
            swipeStartY = event.getY();
        } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
            float dx = event.getX() - swipeStartX;
            float dy = event.getY() - swipeStartY;
            if (Math.abs(dx) > dp(100) && Math.abs(dx) > Math.abs(dy) * 1.5f) {
                if (dx < 0) {
                    if ("HOME".equals(currentTab)) showPage("DOWNLOADS");
                    else if ("DOWNLOADS".equals(currentTab)) showPage("SETTINGS");
                } else {
                    if ("SETTINGS".equals(currentTab)) showPage("DOWNLOADS");
                    else if ("DOWNLOADS".equals(currentTab)) showPage("HOME");
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    void showPage(String page) {
        currentTab = page;
        root = column();
        root.setBackgroundColor(bg);

        LinearLayout header = row();
        pad(header, 22, 14, 22, 12);

        TextView mark = label("✦", 24, green);
        header.addView(mark, lp(35, 42));

        LinearLayout brand = column();
        TextView title = label("S A G E", 25, green);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        title.setGravity(Gravity.CENTER);
        brand.addView(title);
        TextView sub = text("DOWNLOAD • YOUR WAY", 9, muted);
        sub.setGravity(Gravity.CENTER);
        brand.addView(sub);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView menu = label("•••", 20, green);
        menu.setGravity(Gravity.CENTER);
        header.addView(menu, lp(35, 42));
        root.addView(header);

        View line = new View(this);
        line.setBackgroundColor(Color.rgb(27, 43, 31));
        root.addView(line, lp(-1, 1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        content = column();
        pad(content, 20, 20, 20, 100);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        if ("HOME".equals(page)) home();
        else if ("DOWNLOADS".equals(page)) downloads();
        else settings();

        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER);
        pad(nav, 10, 9, 10, 10);
        nav.setBackground(glassShape(28));
        nav.setElevation(dp(12));
        nav.setClipToOutline(true);

        addNav(nav, "⌂", "HOME");
        addNav(nav, "↓", "DOWNLOADS");
        addNav(nav, "⚙", "SETTINGS");
        LinearLayout.LayoutParams np = lp(-1, 64);
        np.setMargins(dp(14), dp(5), dp(14), dp(10));
        root.addView(nav, np);

        setContentView(root);
        android.view.animation.TranslateAnimation slide =
                new android.view.animation.TranslateAnimation(
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.08f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0f);
        slide.setDuration(320);
        slide.setInterpolator(new android.view.animation.DecelerateInterpolator());
        content.startAnimation(slide);
        AlphaAnimation fade = new AlphaAnimation(0.55f, 1f);
        fade.setDuration(320);
        content.startAnimation(fade);

    }


    void addNav(LinearLayout nav, String icon, String name) {
        LinearLayout item = column();
        item.setGravity(Gravity.CENTER);
        TextView i = label(icon, 22,
                currentTab.equals(name) ? green : muted);
        i.setGravity(Gravity.CENTER);
        TextView t = text(name, 10,
                currentTab.equals(name) ? green : muted);
        t.setGravity(Gravity.CENTER);
        item.addView(i);
        item.addView(t);
        if (currentTab.equals(name)) {
            item.setBackground(shape(Color.rgb(24, 40, 28),
                    Color.rgb(55, 83, 59), 17));
        }
        item.setOnClickListener(v -> showPage(name));
        nav.addView(item, new LinearLayout.LayoutParams(0, dp(55), 1));
    }

    void home() {
        TextView eyebrow = label("✦  YOUR PERSONAL MEDIA SPACE", 10, green);
        content.addView(eyebrow);
        gap(content, 12);

        TextView heading = label("Save what you love.", 27, white);
        heading.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        content.addView(heading);
        TextView desc = text("A calm place for your favourite videos.", 13, muted);
        content.addView(desc);
        gap(content, 24);

        LinearLayout panel = column();
        pad(panel, 16, 18, 16, 18);
        panel.setBackground(glassShape(26));

        TextView fieldTitle = label("VIDEO LINK", 11, green);
        panel.addView(fieldTitle);
        gap(panel, 10);

        LinearLayout inputRow = row();
        inputRow.setBackground(shape(Color.rgb(8, 13, 10),
                Color.rgb(45, 62, 48), 17));
        pad(inputRow, 12, 3, 8, 3);

        TextView chain = label("↗", 20, green);
        inputRow.addView(chain, lp(30, 48));

        link = new EditText(this);
        link.setSingleLine(true);
        link.setTextSize(13);
        link.setTextColor(white);
        link.setHintTextColor(Color.rgb(112, 131, 116));
        link.setHint("Paste a direct video URL...");
        link.setBackgroundColor(Color.TRANSPARENT);
        link.setSelectAllOnFocus(false);
        inputRow.addView(link, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView clear = text("×", 25, muted);
        clear.setGravity(Gravity.CENTER);
        clear.setOnClickListener(v -> link.setText(""));
        inputRow.addView(clear, lp(30, 48));
        panel.addView(inputRow);

        gap(panel, 14);
        panel.addView(action("✦    ANALYZE LINK    →", true, () -> {
            hideKeyboard();
            String url = link.getText().toString().trim();
            if (!(url.startsWith("https://") || url.startsWith("http://"))) {
                setStatus("Paste a valid HTTP or HTTPS link first.");
            } else if (isDirectMedia(url)) {
                setStatus("Direct media URL detected. Ready to download.");
                startDownload();
            } else {
                setStatus("This looks like a webpage link. Direct media URLs are supported in this version.");
            }
        }), lp(-1, 52));

        gap(panel, 12);
        panel.addView(action("▣    PASTE FROM CLIPBOARD", false, () -> {
            ClipboardManager cm = (ClipboardManager)
                    getSystemService(CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData d = cm.getPrimaryClip();
                if (d != null && d.getItemCount() > 0) {
                    link.setText(d.getItemAt(0).coerceToText(this));
                    setStatus("Link pasted. Check it before downloading.");
                }
            } else setStatus("Clipboard is empty.");
        }), lp(-1, 48));

        content.addView(panel);
        gap(content, 22);

        TextView features = label("✧  MADE FOR YOUR FLOW", 15, white);
        content.addView(features);
        gap(content, 12);

        LinearLayout r1 = row();
        r1.addView(feature("◈", "Direct media", "MP4 • MP3 • WEBM"), new LinearLayout.LayoutParams(0, dp(100), 1));
        View spacer = new View(this);
        r1.addView(spacer, lp(10, 1));
        r1.addView(feature("↓", "Easy saving", "To Downloads"), new LinearLayout.LayoutParams(0, dp(100), 1));
        content.addView(r1);
        gap(content, 10);

        LinearLayout r2 = row();
        r2.addView(feature("✦", "Minimal design", "Less clutter"), new LinearLayout.LayoutParams(0, dp(100), 1));
        View spacer2 = new View(this);
        r2.addView(spacer2, lp(10, 1));
        r2.addView(feature("◷", "Notifications", "Track completion"), new LinearLayout.LayoutParams(0, dp(100), 1));
        content.addView(r2);

        gap(content, 22);
        LinearLayout quote = column();
        pad(quote, 17, 16, 17, 16);
        quote.setBackground(shape(Color.rgb(11, 20, 13),
                Color.rgb(35, 55, 39), 22));
        TextView q = text("“Keep the moments that matter.”", 17, green);
        q.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        quote.addView(q);
        gap(quote, 6);
        quote.addView(text("— S A G E", 10, muted));
        content.addView(quote);

        gap(content, 14);
        status = text("●  READY WHEN YOU ARE", 12, muted);
        pad(status, 3, 4, 3, 4);
        content.addView(status);
    }

    View feature(String icon, String title, String detail) {
        LinearLayout c = column();
        pad(c, 13, 12, 10, 12);
        c.setBackground(shape(Color.rgb(14, 22, 16),
                Color.rgb(36, 53, 39), 19));
        c.addView(label(icon, 22, green));
        gap(c, 5);
        c.addView(label(title, 13, white));
        c.addView(text(detail, 10, muted));
        return c;
    }

    void downloads() {
        content.addView(label("Your downloads", 26, white));
        gap(content, 6);
        TextView progressInfo = text(getDownloadProgress(), 13, green);
        progressInfo.setTag("download_progress");
        content.addView(progressInfo);
        Handler progressHandler = new Handler(Looper.getMainLooper());
        Runnable progressUpdater = new Runnable() {
            @Override
            public void run() {
                if ("DOWNLOADS".equals(currentTab) && progressInfo.getParent() != null) {
                    progressInfo.setText(getDownloadProgress());
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.post(progressUpdater);
        gap(content, 12);
        content.addView(text("Files saved by SAGE appear in your Downloads folder.", 13, muted));
        gap(content, 22);
        LinearLayout c = column();
        pad(c, 17, 17, 17, 17);
        c.setBackground(shape(card, Color.rgb(42, 61, 45), 22));
        c.addView(label("↓   DOWNLOAD LOCATION", 12, green));
        gap(c, 10);
        c.addView(text("Internal Storage / Download", 14, white));
        gap(c, 18);
        c.addView(action("OPEN DOWNLOADS", false, () -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS).toURI().toString()),
                        "resource/folder");
                startActivity(i);
            } catch (Exception e) {
                setStatus("Open the Downloads folder using your File Manager.");
            }
        }), lp(-1, 48));
        content.addView(c);
    }

    void settings() {
        content.addView(label("Your space.", 27, white));
        gap(content, 6);
        content.addView(text("A few details about SAGE.", 13, muted));
        gap(content, 22);
        settingsCard("THEME", "Midnight • Sage Green");
        settingsCard("DOWNLOADS", "Saved to your Downloads folder");
        settingsCard("SUPPORTED LINKS", "Direct media URLs only");
        settingsCard("VERSION", "SAGE 1.0 • Android");
        gap(content, 12);
        content.addView(text("Only download media you own or have permission to save.", 12, muted));
    }

    void settingsCard(String title, String value) {
        LinearLayout c = column();
        pad(c, 16, 14, 16, 14);
        c.setBackground(shape(card, Color.rgb(38, 55, 41), 18));
        c.addView(label(title, 10, green));
        gap(c, 5);
        c.addView(text(value, 14, white));
        LinearLayout.LayoutParams p = lp(-1, -2);
        p.bottomMargin = dp(10);
        content.addView(c, p);
    }

    void setStatus(String message) {
        if (status != null) status.setText("●  " + message);
        else Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    void hideKeyboard() {
        try {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(link.getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    boolean isDirectMedia(String url) {
        String lower = url.toLowerCase();
        return lower.matches(".*\\.(mp4|webm|mp3|m4a)(\\?.*)?$");
    }

    void handleShare(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (shared != null && link != null) {
                link.setText(shared.trim());
                setStatus("Shared link received. Check it before downloading.");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShare(intent);
    }


    String getDownloadProgress() {
        if (lastDownloadId < 0) return "No recent download yet";

        try {
            DownloadManager dm = (DownloadManager)
                    getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(lastDownloadId);

            try (android.database.Cursor cursor = dm.query(query)) {
                if (cursor == null || !cursor.moveToFirst())
                    return "Download not found";

                int status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                long downloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                long total = cursor.getLong(
                        cursor.getColumnIndexOrThrow(
                                DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                if (status == DownloadManager.STATUS_SUCCESSFUL)
                    return "Download complete!";
                if (status == DownloadManager.STATUS_FAILED)
                    return "Download failed. Check the URL.";
                if (total > 0)
                    return "Downloading: " + (downloaded * 100 / total) + "%";
                return "Download in progress…";
            }
        } catch (Exception e) {
            return "Could not check download status";
        }
    }

    void startDownload() {
        String url = link.getText().toString().trim();
        if (!isDirectMedia(url)) {
            setStatus("A direct media URL ending in MP4, WEBM, MP3 or M4A is required.");
            return;
        }

        try {
            DownloadManager dm = (DownloadManager)
                    getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request req =
                    new DownloadManager.Request(Uri.parse(url));
            req.setTitle("SAGE media");
            req.setDescription("Saving to Downloads");
            req.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            String path = Uri.parse(url).getPath();
            String ext = "mp4";
            if (path != null && path.lastIndexOf('.') >= 0) {
                String candidate = path.substring(path.lastIndexOf('.') + 1)
                        .toLowerCase();
                if (candidate.matches("mp4|webm|mp3|m4a")) ext = candidate;
            }

            req.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "SAGE_" + System.currentTimeMillis() + "." + ext);
            lastDownloadId = dm.enqueue(req);
            setStatus("Download started. Check your notification and Downloads folder.");
        } catch (Exception e) {
            setStatus("Could not start download. Check the URL and connection.");
        }
    }
}
