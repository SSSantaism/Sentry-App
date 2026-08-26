package com.project.sentry;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.sentry.adapter.LogAdapter;
import com.project.sentry.model.DetectionLog;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity untuk menampilkan halaman Log Deteksi (Detection Logs).
 * <p>
 * Menampilkan data secara real-time dari Firebase Cloud Firestore (koleksi "detection_logs").
 * </p>
 */
public class LogDeteksiActivity extends AppCompatActivity {

    private static final String TAG = "LogDeteksiActivity";

    // ── View & Adapter ──────────────────────────────────────────
    private RecyclerView rvDetectionLogs;
    private LogAdapter logAdapter;
    private List<DetectionLog> logList;

    // ── Firebase ───────────────────────────────────────────────
    private FirebaseFirestore db;

    // ── Lifecycle ────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_deteksi);

        Log.d(TAG, "Halaman Log Deteksi dibuka.");

        // Inisialisasi Firestore
        db = FirebaseFirestore.getInstance();

        // Setup UI
        setupBackNavigation();
        setupFilterChips();
        setupLoadOlderLogs();
        setupRecyclerView();

        // Mulai memantau data Firestore
        listenToDetectionLogs();
    }

    // ── Setup UI ────────────────────────────────────────────────

    private void setupBackNavigation() {
        findViewById(R.id.iv_app_logo).setOnClickListener(v -> finish());
        findViewById(R.id.tv_app_title).setOnClickListener(v -> finish());
    }

    private void setupFilterChips() {
        findViewById(R.id.chip_today).setOnClickListener(v -> {
            Toast.makeText(this, "Filter: TODAY", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.chip_this_week).setOnClickListener(v -> {
            Toast.makeText(this, "Filter: THIS WEEK", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.chip_high_confidence).setOnClickListener(v -> {
            Toast.makeText(this, "Filter: HIGH CONFIDENCE", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupLoadOlderLogs() {
        findViewById(R.id.btn_load_older_logs).setOnClickListener(v -> {
            Toast.makeText(this, "Memuat log lama...", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerView() {
        rvDetectionLogs = findViewById(R.id.rv_detection_logs);
        rvDetectionLogs.setLayoutManager(new LinearLayoutManager(this));
        
        logList = new ArrayList<>();
        logAdapter = new LogAdapter(logList);
        rvDetectionLogs.setAdapter(logAdapter);
    }

    // ── Firebase Firestore ───────────────────────────────────────

    /**
     * Memantau koleksi "detection_logs" di Firestore secara real-time.
     * Mengurutkan berdasarkan timestamp secara menurun (terbaru di atas).
     */
    private void listenToDetectionLogs() {
        db.collection("detection_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Gagal memuat log dari Firestore.", error);
                        Toast.makeText(this, "Gagal memuat data log.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        logList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                DetectionLog log = doc.toObject(DetectionLog.class);
                                log.setId(doc.getId());
                                logList.add(log);
                            } catch (Exception e) {
                                Log.e(TAG, "Gagal parsing log: " + doc.getId(), e);
                            }
                        }
                        logAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Berhasil memuat " + logList.size() + " log dari Firestore.");
                    }
                });
    }
}
