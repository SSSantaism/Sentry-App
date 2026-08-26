package com.project.sentry.model;

/**
 * Model POJO untuk merepresentasikan satu entri log deteksi suara.
 * <p>
 * Setiap objek DetectionLog merekam satu kejadian akustik yang
 * ditangkap oleh jaringan sensor bioakustik SENTRY, termasuk
 * waktu kejadian, tingkat kepercayaan klasifikasi, dan lokasi
 * zona sensor yang menangkap sinyal.
 * </p>
 */
public class DetectionLog {

    // ── Field ────────────────────────────────────────────────────
    private String id;

    /** Waktu deteksi dalam format ISO-8601 atau epoch millis. */
    private String timestamp;

    /**
     * Tingkat akurasi/kepercayaan hasil klasifikasi suara (0–100%).
     * Contoh: 94 berarti sensor 94% yakin suara tersebut adalah tembakan.
     */
    private int confidenceLevel;

    /** Lokasi/zona sensor yang menangkap suara, misal "Sektor Utara - Blok B". */
    private String location;

    /** Tipe anomali suara, misal "CRITICAL_EVENT" atau "NOISE_ANOMALY". */
    private String type;

    // ── Constructor ──────────────────────────────────────────────

    /** Constructor kosong — diperlukan oleh Firebase Firestore deserialization. */
    public DetectionLog() {
    }

    /**
     * Constructor lengkap untuk membuat objek DetectionLog baru.
     *
     * @param id              ID unik entri log
     * @param timestamp       waktu deteksi
     * @param confidenceLevel tingkat akurasi (0–100)
     * @param location        lokasi/zona sensor
     * @param type            tipe deteksi (CRITICAL_EVENT atau NOISE_ANOMALY)
     */
    public DetectionLog(String id, String timestamp, int confidenceLevel, String location, String type) {
        this.id = id;
        this.timestamp = timestamp;
        this.confidenceLevel = confidenceLevel;
        this.location = location;
        this.type = type;
    }

    // ── Getter & Setter ──────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(int confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // ── Utility ──────────────────────────────────────────────────

    /**
     * Mengembalikan label confidence yang siap ditampilkan di UI.
     * Contoh output: "CONF_94%"
     */
    public String getConfidenceLabel() {
        return "CONF_" + confidenceLevel + "%";
    }

    /**
     * Menentukan apakah deteksi ini termasuk "high-confidence"
     * (≥ 85%) sehingga harus ditampilkan dengan gaya CRITICAL_EVENT.
     */
    public boolean isHighConfidence() {
        return confidenceLevel >= 85;
    }

    @Override
    public String toString() {
        return "DetectionLog{" +
                "id='" + id + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", confidenceLevel=" + confidenceLevel +
                ", location='" + location + '\'' +
                '}';
    }
}
