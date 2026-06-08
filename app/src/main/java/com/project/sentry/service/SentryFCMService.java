package com.project.sentry.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.project.sentry.MainActivity;
import com.project.sentry.R;

import java.util.Map;

/**
 * Service untuk menerima pesan push notification dari Firebase Cloud Messaging (FCM).
 * <p>
 * Service ini akan aktif secara otomatis saat Firebase mengirimkan pesan
 * data payload ke perangkat. Payload yang diharapkan memiliki key:
 * <ul>
 *   <li>{@code timestamp}  — waktu deteksi suara tembakan</li>
 *   <li>{@code confidence} — tingkat akurasi klasifikasi (0–100)</li>
 *   <li>{@code location}   — zona/lokasi sensor yang menangkap suara</li>
 * </ul>
 * </p>
 *
 * <p>Harus didaftarkan di AndroidManifest.xml dengan intent-filter
 * {@code com.google.firebase.MESSAGING_EVENT}.</p>
 */
public class SentryFCMService extends FirebaseMessagingService {

    private static final String TAG = "SentryFCMService";

    /**
     * ID Notification Channel — wajib untuk Android 8.0 (Oreo) ke atas.
     * Channel ini dikonfigurasi dengan prioritas HIGH agar notifikasi
     * muncul sebagai heads-up notification.
     */
    private static final String CHANNEL_ID = "sentry_gunshot_alert";
    private static final String CHANNEL_NAME = "Peringatan Tembakan";
    private static final String CHANNEL_DESC =
            "Notifikasi untuk deteksi suara tembakan ilegal dari jaringan sensor SENTRY.";

    // ── Lifecycle FCM ───────────────────────────────────────────

    /**
     * Dipanggil saat perangkat menerima token FCM baru.
     * Token ini perlu dikirim ke backend server agar server bisa
     * menargetkan notifikasi ke perangkat ini.
     *
     * @param token token FCM baru yang diberikan oleh Firebase
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Token FCM baru diterima: " + token);

        // TODO: Kirim token ke backend server untuk disimpan
        // Contoh: ApiClient.sendTokenToServer(token);
        sendTokenToServer(token);
    }

    /**
     * Dipanggil setiap kali perangkat menerima pesan dari FCM.
     * <p>
     * Alur logika:
     * 1. Ambil data payload dari pesan (timestamp, confidence, location).
     * 2. Validasi bahwa payload tidak kosong.
     * 3. Bangun notifikasi push dengan informasi deteksi.
     * 4. Tampilkan notifikasi ke pengguna melalui NotificationManager.
     * </p>
     *
     * @param remoteMessage pesan yang diterima dari Firebase
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Pesan FCM diterima dari: " + remoteMessage.getFrom());

        // ── Langkah 1: Ambil data payload dari Firebase ──
        Map<String, String> dataPayload = remoteMessage.getData();

        if (dataPayload.isEmpty()) {
            Log.w(TAG, "Payload data kosong, notifikasi tidak ditampilkan.");
            return;
        }

        // Ekstrak informasi deteksi dari payload
        String waktuDeteksi    = dataPayload.getOrDefault("timestamp", "Waktu tidak diketahui");
        String akurasiTembakan = dataPayload.getOrDefault("confidence", "0");
        String lokasiSensor    = dataPayload.getOrDefault("location", "Lokasi tidak diketahui");

        Log.d(TAG, "Data deteksi diterima — " +
                "Waktu: " + waktuDeteksi +
                ", Akurasi: " + akurasiTembakan + "%" +
                ", Lokasi: " + lokasiSensor);

        // ── Langkah 2: Bangun dan tampilkan notifikasi ──
        tampilkanNotifikasiTembakan(waktuDeteksi, akurasiTembakan, lokasiSensor);
    }

    // ── Notifikasi ──────────────────────────────────────────────

    /**
     * Membangun dan menampilkan notifikasi push peringatan tembakan.
     * <p>
     * Notifikasi akan muncul sebagai heads-up notification (prioritas HIGH)
     * dengan suara default ringtone. Saat ditekan, aplikasi akan terbuka
     * ke halaman utama (MainActivity).
     * </p>
     *
     * @param waktu   waktu deteksi suara tembakan
     * @param akurasi tingkat akurasi klasifikasi (string, misal "94")
     * @param lokasi  zona/lokasi sensor yang menangkap suara
     */
    private void tampilkanNotifikasiTembakan(String waktu, String akurasi, String lokasi) {

        // ── Buat Notification Channel (wajib untuk Android 8.0+) ──
        buatNotificationChannel();

        // ── Siapkan intent untuk membuka aplikasi saat notifikasi ditekan ──
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // Kirimkan data deteksi sebagai extras agar Activity bisa menampilkannya
        intent.putExtra("from_notification", true);
        intent.putExtra("timestamp", waktu);
        intent.putExtra("confidence", akurasi);
        intent.putExtra("location", lokasi);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // ── Suara notifikasi default ──
        Uri suaraDefault = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        // ── Bangun notifikasi ──
        String judulNotifikasi = "\u26A0 Peringatan: Suara Tembakan Terdeteksi!";
        String isiNotifikasi = "Lokasi: " + lokasi +
                "\nAkurasi: CONF_" + akurasi + "%" +
                "\nWaktu: " + waktu;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                // Ikon kecil di status bar — gunakan ikon bawaan Android
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                // Judul notifikasi
                .setContentTitle(judulNotifikasi)
                // Isi singkat (satu baris)
                .setContentText("Deteksi tembakan di " + lokasi + " — CONF_" + akurasi + "%")
                // Tampilan panjang (expanded) — multi-baris
                .setStyle(new NotificationCompat.BigTextStyle().bigText(isiNotifikasi))
                // Prioritas tinggi agar muncul sebagai heads-up notification
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Kategori: alarm keamanan
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                // Suara notifikasi
                .setSound(suaraDefault)
                // Getaran (pola: delay, vibrate, sleep, vibrate)
                .setVibrate(new long[]{0, 500, 200, 500})
                // Warna LED indikator (merah untuk bahaya)
                .setLights(0xFFD70028, 1000, 500)
                // Notifikasi otomatis hilang setelah ditekan
                .setAutoCancel(true)
                // Aksi saat notifikasi ditekan
                .setContentIntent(pendingIntent);

        // ── Tampilkan notifikasi melalui NotificationManager ──
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // Gunakan hashCode dari waktu sebagai notificationId agar tiap
            // deteksi menghasilkan notifikasi terpisah (tidak saling menimpa)
            int notificationId = waktu.hashCode();
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notifikasi tembakan ditampilkan (ID: " + notificationId + ")");
        }
    }

    /**
     * Membuat Notification Channel yang diperlukan oleh Android 8.0 (Oreo) ke atas.
     * <p>
     * Channel hanya perlu dibuat sekali — pemanggilan berulang tidak akan
     * membuat duplikat karena Android mengabaikan channel dengan ID yang sama.
     * Prioritas diset ke IMPORTANCE_HIGH agar notifikasi tampil sebagai
     * heads-up (pop-up di layar atas).
     * </p>
     */
    private void buatNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            // Aktifkan getaran untuk channel ini
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            // Aktifkan LED indikator
            channel.enableLights(true);
            channel.setLightColor(0xFFD70028);

            NotificationManager manager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ── Helper ──────────────────────────────────────────────────

    /**
     * Placeholder untuk mengirim token FCM ke backend server.
     * Implementasikan sesuai arsitektur backend Anda (REST API / Firestore).
     *
     * @param token token FCM terbaru
     */
    private void sendTokenToServer(String token) {
        // TODO: Implementasi pengiriman token ke backend
        // Contoh menggunakan Retrofit:
        // ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        // api.registerFcmToken(new TokenRequest(token)).enqueue(...);
        Log.d(TAG, "TODO: Kirim token ke backend server — " + token);
    }
}
