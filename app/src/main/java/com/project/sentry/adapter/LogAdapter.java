package com.project.sentry.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.sentry.R;
import com.project.sentry.model.DetectionLog;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView Adapter untuk menampilkan daftar {@link DetectionLog}
 * di halaman Log Deteksi.
 * <p>
 * Adapter ini membaca layout {@code R.layout.item_log_deteksi} dan mengisi
 * komponen-komponen berikut:
 * <ul>
 *   <li>{@code tv_status_tembakan}  — label kategori event (CRITICAL / ANOMALY)</li>
 *   <li>{@code tv_waktu_deteksi}    — waktu deteksi suara</li>
 *   <li>{@code tv_lokasi_sensor}    — zona/lokasi sensor</li>
 *   <li>{@code tv_akurasi_tembakan} — badge tingkat akurasi (CONF_xx%)</li>
 *   <li>{@code iv_icon_deteksi}     — ikon indikator jenis deteksi</li>
 * </ul>
 * </p>
 */
public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    // ── Data Source ──────────────────────────────────────────────
    private final List<DetectionLog> logList;

    // ── Listener ────────────────────────────────────────────────
    private OnLogItemClickListener listener;

    /**
     * Interface callback ketika user menekan salah satu baris log.
     */
    public interface OnLogItemClickListener {
        void onLogItemClick(DetectionLog log, int position);
    }

    // ── Constructor ─────────────────────────────────────────────

    public LogAdapter() {
        this.logList = new ArrayList<>();
    }

    public LogAdapter(List<DetectionLog> initialList) {
        this.logList = (initialList != null) ? initialList : new ArrayList<>();
    }

    // ── Public API ──────────────────────────────────────────────

    /**
     * Mengganti seluruh dataset dan memperbarui tampilan.
     *
     * @param newList daftar DetectionLog terbaru
     */
    public void submitList(List<DetectionLog> newList) {
        logList.clear();
        if (newList != null) {
            logList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    /**
     * Menambahkan satu entri log baru di posisi paling atas (index 0).
     * Berguna saat menerima push notification baru dari FCM.
     *
     * @param log entri log baru
     */
    public void addLogToTop(DetectionLog log) {
        logList.add(0, log);
        notifyItemInserted(0);
    }

    /**
     * Memasang listener untuk event klik pada baris log.
     */
    public void setOnLogItemClickListener(OnLogItemClickListener listener) {
        this.listener = listener;
    }

    // ── Adapter Overrides ───────────────────────────────────────

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log_deteksi, parent, false);
        return new LogViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        DetectionLog currentLog = logList.get(position);
        holder.bind(currentLog);
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    // ── ViewHolder ──────────────────────────────────────────────

    class LogViewHolder extends RecyclerView.ViewHolder {

        // Referensi ke komponen XML di item_log_deteksi.xml
        private final ImageView ivIconDeteksi;
        private final TextView tvStatusTembakan;
        private final TextView tvWaktuDeteksi;
        private final TextView tvLokasiSensor;
        private final TextView tvAkurasiTembakan;

        LogViewHolder(@NonNull View itemView) {
            super(itemView);

            // Binding view dengan ID snake_case sesuai konvensi bioakustik
            ivIconDeteksi     = itemView.findViewById(R.id.iv_icon_deteksi);
            tvStatusTembakan  = itemView.findViewById(R.id.tv_status_tembakan);
            tvWaktuDeteksi    = itemView.findViewById(R.id.tv_waktu_deteksi);
            tvLokasiSensor    = itemView.findViewById(R.id.tv_lokasi_sensor);
            tvAkurasiTembakan = itemView.findViewById(R.id.tv_akurasi_tembakan);

            // Pasang click listener pada keseluruhan baris
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onLogItemClick(logList.get(pos), pos);
                    }
                }
            });
        }

        /**
         * Mengisi data dari objek DetectionLog ke dalam komponen UI.
         * Jika confidence ≥ 85%, item ditampilkan dalam gaya CRITICAL_EVENT
         * (ikon warning, teks merah). Jika tidak, ditampilkan sebagai ACOUSTIC_ANOMALY.
         */
        void bind(DetectionLog log) {
            // Tampilkan waktu deteksi
            tvWaktuDeteksi.setText(log.getTimestamp());

            // Tampilkan lokasi sensor
            tvLokasiSensor.setText(log.getLocation());

            // Tampilkan badge akurasi (misal "CONF_94%")
            tvAkurasiTembakan.setText(log.getConfidenceLabel());

            // Tentukan gaya tampilan berdasarkan tingkat kepercayaan
            if (log.isHighConfidence()) {
                // ── CRITICAL_EVENT ──
                tvStatusTembakan.setText("CRITICAL_EVENT");
                tvStatusTembakan.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.error));
                tvAkurasiTembakan.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.error));
                tvAkurasiTembakan.setBackgroundResource(R.drawable.bg_badge_error);

                // Ikon peringatan untuk deteksi kritis
                ivIconDeteksi.setImageResource(android.R.drawable.ic_dialog_alert);
                ivIconDeteksi.setColorFilter(
                        ContextCompat.getColor(itemView.getContext(), R.color.error));
            } else {
                // ── ACOUSTIC_ANOMALY ──
                tvStatusTembakan.setText("ACOUSTIC_ANOMALY");
                tvStatusTembakan.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.on_surface_variant));
                tvAkurasiTembakan.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.tertiary));
                tvAkurasiTembakan.setBackgroundResource(R.drawable.bg_badge_tertiary);

                // Ikon volume untuk anomali akustik biasa
                ivIconDeteksi.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                ivIconDeteksi.setColorFilter(
                        ContextCompat.getColor(itemView.getContext(), R.color.on_surface_variant));
            }
        }
    }
}
