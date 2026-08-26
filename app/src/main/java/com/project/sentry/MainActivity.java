package com.project.sentry;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.sentry.service.SentryListenerService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Activity utama: Dashboard Monitoring SENTRY.
 * <p>
 * Menampilkan layout {@code activity_main.xml} yang berisi:
 * - Pulse radar indicator (status monitoring aktif)
 * - Alert card (deteksi tembakan terakhir)
 * - Recent Telemetry list (RecyclerView)
 * - Tombol simulasi notifikasi untuk testing lokal
 * - Tombol navigasi ke halaman Log Deteksi
 * </p>
 * <p>
 * Activity ini juga menangani:
 * - Permintaan izin {@code POST_NOTIFICATIONS} secara runtime (Android 13+)
 * - Intent dari klik notifikasi FCM (menampilkan data deteksi)
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Kode request untuk izin notifikasi
    private static final int REQ_CODE_POST_NOTIFICATIONS = 1001;

    // Notification channel ID — sama dengan yang dipakai di SentryFCMService
    private static final String CHANNEL_ID = "sentry_gunshot_alert";
    private static final String CHANNEL_NAME = "Peringatan Tembakan";
    private static final String CHANNEL_DESC =
            "Notifikasi untuk deteksi suara tembakan ilegal dari jaringan sensor SENTRY.";

    // Counter untuk notifikasi simulasi agar ID unik
    private int simulasiCounter = 0;

    // ── View References ──────────────────────────────────────────
    private RecyclerView rvLogSuara;

    // ── Lifecycle ────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Minta izin notifikasi untuk Android 13+ (API 33)
        mintaIzinNotifikasi();

        // Mulai Foreground Service untuk mendengarkan perubahan
        // /gunshot_status di Realtime Database secara real-time
        startSentryListenerService();

        // Inisialisasi komponen UI
        setupTelemetryRecyclerView();
        setupSimulationButton();
        setupNavigationButton();

        // Cek apakah Activity dibuka dari klik notifikasi
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle jika Activity sudah terbuka dan notifikasi diklik lagi
        handleNotificationIntent(intent);
    }

    // ── Izin Notifikasi (Android 13+ / API 33) ──────────────────

    /**
     * Meminta izin POST_NOTIFICATIONS secara runtime.
     * Wajib untuk Android 13 (Tiramisu) ke atas agar notifikasi push
     * dari FCM bisa tampil di perangkat.
     */
    private void mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Meminta izin POST_NOTIFICATIONS kepada pengguna...");
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_CODE_POST_NOTIFICATIONS
                );
            } else {
                Log.d(TAG, "Izin POST_NOTIFICATIONS sudah diberikan.");
            }
        }
    }

    // ── Start Listener Service ───────────────────────────────────

    /**
     * Menjalankan SentryListenerService sebagai Foreground Service.
     * Service ini akan mendengarkan perubahan /gunshot_status di
     * Firebase Realtime Database. Ketika ESP32 mengubah status ke 1
     * (bahaya terdeteksi), service langsung menampilkan notifikasi.
     */
    private void startSentryListenerService() {
        Intent serviceIntent = new Intent(this, SentryListenerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Log.d(TAG, "✅ SentryListenerService dimulai.");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Izin notifikasi DIBERIKAN oleh pengguna.");
                Toast.makeText(this, "Izin notifikasi aktif — SENTRY siap beroperasi.",
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "❌ Izin notifikasi DITOLAK oleh pengguna.");
                Toast.makeText(this,
                        "Peringatan: Notifikasi deteksi tembakan tidak akan muncul tanpa izin ini.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ── Handle Intent dari Notifikasi ───────────────────────────

    /**
     * Menangani intent yang dikirim saat pengguna mengklik notifikasi FCM.
     * Data deteksi (timestamp, confidence, location) dikirimkan sebagai extras
     * oleh SentryFCMService. Data ini ditampilkan sebagai Toast dan diupdate
     * di alert card pada dashboard.
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null) return;

        boolean fromNotification = intent.getBooleanExtra("from_notification", false);
        if (!fromNotification) return;

        String waktu   = intent.getStringExtra("timestamp");
        String akurasi = intent.getStringExtra("confidence");
        String lokasi  = intent.getStringExtra("location");

        Log.d(TAG, "Dashboard dibuka dari notifikasi — " +
                "Waktu: " + waktu + ", Akurasi: " + akurasi + "%, Lokasi: " + lokasi);

        // Tampilkan informasi deteksi dari notifikasi
        Toast.makeText(this,
                "🔔 Data Deteksi:\n" +
                "Lokasi: " + lokasi + "\n" +
                "Akurasi: CONF_" + akurasi + "%\n" +
                "Waktu: " + waktu,
                Toast.LENGTH_LONG).show();

        // Update alert card di dashboard dengan data dari notifikasi
        updateAlertCard(waktu, akurasi, lokasi);

        // Bersihkan flag agar tidak diproses ulang
        intent.removeExtra("from_notification");
    }

    /**
     * Memperbarui tampilan alert card pada dashboard dengan data deteksi baru.
     */
    private void updateAlertCard(String waktu, String akurasi, String lokasi) {
        TextView tvAlertLocation = findViewById(R.id.tv_alert_location_sensor);
        TextView tvAlertTimeAgo  = findViewById(R.id.tv_alert_time_ago);
        TextView tvConfidence    = findViewById(R.id.tv_confidence_score_value);

        if (tvAlertLocation != null && lokasi != null) {
            tvAlertLocation.setText(lokasi);
        }
        if (tvAlertTimeAgo != null) {
            tvAlertTimeAgo.setText("JUST NOW");
        }
        if (tvConfidence != null && akurasi != null) {
            tvConfidence.setText(akurasi + "%");
        }
    }

    // ── Telemetry RecyclerView ──────────────────────────────────

    /**
     * Menginisialisasi RecyclerView pada dashboard untuk menampilkan
     * daftar telemetri/heartbeat sensor terbaru.
     * Menggunakan data dummy untuk demonstrasi visual.
     */
    private void setupTelemetryRecyclerView() {
        rvLogSuara = findViewById(R.id.rv_log_suara);
        rvLogSuara.setLayoutManager(new LinearLayoutManager(this));
        // Disable nested scrolling karena sudah di dalam ScrollView
        rvLogSuara.setNestedScrollingEnabled(false);

        // Data dummy telemetri untuk demonstrasi (Sesuai request: Uptime, Batre, Signal)
        List<TelemetryEntry> dummyData = new ArrayList<>();
        dummyData.add(new TelemetryEntry(
                "SYSTEM UPTIME",
                "-",
                "-",
                android.R.drawable.ic_menu_recent_history));
        dummyData.add(new TelemetryEntry(
                "BATTERY STATUS",
                "-",
                "-",
                android.R.drawable.ic_lock_idle_low_battery)); // Atau ikon baterai lain jika ada
        dummyData.add(new TelemetryEntry(
                "SIGNAL STRENGTH",
                "-",
                "-",
                android.R.drawable.presence_online));

        rvLogSuara.setAdapter(new TelemetryAdapter(dummyData));
    }

    // ── Tombol Simulasi Notifikasi ──────────────────────────────

    /**
     * Mengonfigurasi tombol "SIMULASIKAN DETEKSI TEMBAKAN" untuk
     * memicu notifikasi lokal yang identik dengan notifikasi FCM asli.
     * Ini memungkinkan pengujian tampilan notifikasi, suara alarm,
     * getaran, dan navigasi tanpa backend server.
     */
    private void setupSimulationButton() {
        findViewById(R.id.btn_simulasi_notifikasi).setOnClickListener(v -> {
            simulasiCounter++;

            // Data simulasi yang realistis
            String waktuSimulasi = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(new Date());
            String lokasiSimulasi = "SECTOR ZERO • SENTRY";
            int akurasiSimulasi = 50 + (int)(Math.random() * 50); // 50-99%
            String tipeSimulasi = akurasiSimulasi >= 85 ? "CRITICAL_EVENT" : "NOISE_ANOMALY";

            Log.d(TAG, "🔴 Memicu simulasi notifikasi #" + simulasiCounter +
                    " — Lokasi: " + lokasiSimulasi +
                    ", Akurasi: " + akurasiSimulasi + "%");

            // Tampilkan notifikasi lokal yang identik dengan FCM
            tampilkanNotifikasiSimulasi(waktuSimulasi,
                    String.valueOf(akurasiSimulasi), lokasiSimulasi);

            // Simpan ke Firestore
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            com.project.sentry.model.DetectionLog logBaru = new com.project.sentry.model.DetectionLog(
                    null, waktuSimulasi, akurasiSimulasi, lokasiSimulasi, tipeSimulasi
            );

            db.collection("detection_logs").add(logBaru)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Log berhasil ditulis ke Firestore dengan ID: " + documentReference.getId());
                        Toast.makeText(this, "✅ Log tersimpan ke Firebase!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Gagal menulis log ke Firestore", e);
                    });
        });
    }

    /**
     * Membangun dan menampilkan notifikasi lokal yang identik dengan
     * notifikasi dari SentryFCMService. Digunakan untuk testing offline.
     *
     * @param waktu   waktu deteksi simulasi
     * @param akurasi tingkat akurasi simulasi (string, misal "94")
     * @param lokasi  zona/lokasi sensor simulasi
     */
    private void tampilkanNotifikasiSimulasi(String waktu, String akurasi, String lokasi) {

        // Buat Notification Channel (wajib Android 8.0+)
        buatNotificationChannel();

        // Siapkan intent untuk membuka Dashboard saat notifikasi ditekan
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("from_notification", true);
        intent.putExtra("timestamp", waktu);
        intent.putExtra("confidence", akurasi);
        intent.putExtra("location", lokasi);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                simulasiCounter, // Request code unik per simulasi
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Suara alarm (bukan ringtone biasa — agar lebih urgent)
        Uri suaraAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        String judulNotifikasi = "\u26A0 Peringatan: Suara Tembakan Terdeteksi!";
        String isiNotifikasi = "Lokasi: " + lokasi +
                "\nAkurasi: CONF_" + akurasi + "%" +
                "\nWaktu: " + waktu;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(judulNotifikasi)
                .setContentText("Deteksi tembakan di " + lokasi + " — CONF_" + akurasi + "%")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(isiNotifikasi))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(suaraAlarm)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setLights(0xFFD70028, 1000, 500)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            int notificationId = ("sim_" + waktu).hashCode();
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notifikasi simulasi ditampilkan (ID: " + notificationId + ")");
        }
    }

    /**
     * Membuat Notification Channel — identik dengan channel di SentryFCMService.
     * Channel hanya dibuat sekali; pemanggilan berulang diabaikan oleh sistem.
     */
    private void buatNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.enableLights(true);
            channel.setLightColor(0xFFD70028);

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ── Tombol Navigasi ke Log Deteksi ──────────────────────────

    /**
     * Mengonfigurasi tombol "BUKA LOG DETEKSI" untuk membuka
     * LogDeteksiActivity yang menampilkan riwayat deteksi lengkap.
     */
    private void setupNavigationButton() {
        findViewById(R.id.btn_buka_log_deteksi).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LogDeteksiActivity.class);
            startActivity(intent);
        });
    }

    // ── Inner Adapter untuk Telemetry List di Dashboard ─────────

    /**
     * Data class sederhana untuk item telemetri pada dashboard.
     */
    static class TelemetryEntry {
        final String title;
        final String subtitle;
        final String timeAgo;
        final int iconResId;

        TelemetryEntry(String title, String subtitle, String timeAgo, int iconResId) {
            this.title = title;
            this.subtitle = subtitle;
            this.timeAgo = timeAgo;
            this.iconResId = iconResId;
        }
    }

    /**
     * Adapter untuk RecyclerView telemetri di dashboard.
     * Menggunakan layout {@code item_log_suara.xml}.
     */
    static class TelemetryAdapter extends RecyclerView.Adapter<TelemetryAdapter.ViewHolder> {

        private final List<TelemetryEntry> entries;

        TelemetryAdapter(List<TelemetryEntry> entries) {
            this.entries = entries;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log_suara, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TelemetryEntry entry = entries.get(position);
            holder.tvTitle.setText(entry.title);
            holder.tvSubtitle.setText(entry.subtitle);
            holder.tvTimeAgo.setText(entry.timeAgo);
            holder.ivIcon.setImageResource(entry.iconResId);
            // Tint ikon sesuai design system
            holder.ivIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivIcon;
            final TextView tvTitle;
            final TextView tvSubtitle;
            final TextView tvTimeAgo;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon     = itemView.findViewById(R.id.iv_log_icon);
                tvTitle    = itemView.findViewById(R.id.tv_log_title);
                tvSubtitle = itemView.findViewById(R.id.tv_log_subtitle);
                tvTimeAgo  = itemView.findViewById(R.id.tv_log_time_ago);
            }
        }
    }
}
