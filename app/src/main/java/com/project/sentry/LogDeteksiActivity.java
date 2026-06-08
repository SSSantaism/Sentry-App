package com.project.sentry;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.sentry.adapter.LogAdapter;
import com.project.sentry.model.DetectionLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity untuk menampilkan halaman Log Deteksi (Detection Logs).
 * <p>
 * Memuat layout {@code activity_log_deteksi.xml} yang berisi:
 * - Header "Detection Logs" dengan subtitle
 * - Filter chips (TODAY, THIS WEEK, HIGH CONFIDENCE)
 * - Daftar log deteksi statis (hardcoded di XML) + RecyclerView untuk data dinamis
 * - Tombol "LOAD OLDER LOGS"
 * </p>
 * <p>
 * Activity ini mendemonstrasikan bagaimana {@link LogAdapter} bekerja dengan
 * data {@link DetectionLog} dari berbagai sumber (dummy data, FCM push, Firestore).
 * </p>
 */
public class LogDeteksiActivity extends AppCompatActivity {

    private static final String TAG = "LogDeteksiActivity";

    // ── View References ──────────────────────────────────────────
    private LogAdapter logAdapter;

    // ── Lifecycle ────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_deteksi);

        Log.d(TAG, "Halaman Log Deteksi dibuka.");

        // Inisialisasi tombol navigasi kembali
        setupBackNavigation();

        // Inisialisasi filter chips
        setupFilterChips();

        // Inisialisasi tombol load older logs
        setupLoadOlderLogs();
    }

    // ── Navigasi Kembali ────────────────────────────────────────

    /**
     * Mengatur ikon logo di AppBar sebagai tombol kembali ke Dashboard.
     */
    private void setupBackNavigation() {
        // Gunakan logo/app title area sebagai tombol back
        findViewById(R.id.iv_app_logo).setOnClickListener(v -> finish());
        findViewById(R.id.tv_app_title).setOnClickListener(v -> finish());
    }

    // ── Filter Chips ────────────────────────────────────────────

    /**
     * Mengatur interaksi untuk filter chip:
     * - TODAY (aktif default)
     * - THIS WEEK
     * - HIGH CONFIDENCE
     *
     * Saat ini hanya menampilkan Toast karena data masih statis (hardcoded di XML).
     * Di implementasi penuh, chip ini akan memfilter RecyclerView berdasarkan query.
     */
    private void setupFilterChips() {
        findViewById(R.id.chip_today).setOnClickListener(v -> {
            Toast.makeText(this, "Filter: TODAY — Menampilkan log hari ini",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Filter chip TODAY dipilih.");
        });

        findViewById(R.id.chip_this_week).setOnClickListener(v -> {
            Toast.makeText(this, "Filter: THIS WEEK — Menampilkan log minggu ini",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Filter chip THIS WEEK dipilih.");
        });

        findViewById(R.id.chip_high_confidence).setOnClickListener(v -> {
            Toast.makeText(this, "Filter: HIGH CONFIDENCE — Menampilkan log ≥ 85% akurasi",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Filter chip HIGH CONFIDENCE dipilih.");
        });
    }

    // ── Load Older Logs ─────────────────────────────────────────

    /**
     * Mengatur tombol "LOAD OLDER LOGS".
     * Di implementasi penuh, ini akan melakukan pagination query ke Firestore
     * menggunakan cursor/startAfter. Saat ini hanya menampilkan Toast.
     */
    private void setupLoadOlderLogs() {
        findViewById(R.id.btn_load_older_logs).setOnClickListener(v -> {
            Toast.makeText(this,
                    "Memuat log lama... (akan terhubung ke Firestore)",
                    Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Tombol LOAD OLDER LOGS diklik.");
        });
    }

    // ── Helper: Generate Dummy Data ─────────────────────────────

    /**
     * Menghasilkan data dummy untuk demonstrasi RecyclerView.
     * Di implementasi penuh, data ini akan berasal dari Firestore real-time query.
     *
     * @return list berisi 6 DetectionLog dummy
     */
    private List<DetectionLog> generateDummyData() {
        List<DetectionLog> logs = new ArrayList<>();
        logs.add(new DetectionLog("LOG-001", "14:23:05", 98, "Sektor Utara - Blok B"));
        logs.add(new DetectionLog("LOG-002", "11:05:22", 62, "Sektor Timur - Trail 4"));
        logs.add(new DetectionLog("LOG-003", "09:14:45", 75, "Perimeter Road South"));
        logs.add(new DetectionLog("LOG-004", "06:30:12", 91, "Sektor Barat - Zona Konservasi"));
        logs.add(new DetectionLog("LOG-005", "03:45:33", 44, "Sungai Utama - Checkpoint 2"));
        logs.add(new DetectionLog("LOG-006", "01:12:08", 88, "Sektor 7G - Habitat Orangutan"));
        return logs;
    }
}
