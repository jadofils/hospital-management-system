package hospital.management.backend.mongo.service;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import hospital.management.backend.config.AppLogger;
import hospital.management.backend.dto.patient.PatientNoteDTO;
import hospital.management.backend.mongo.config.MongoConfig;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB-backed service for free-text patient notes (collection: patient_notes).
 *
 * This is the true NoSQL implementation. The existing PatientNotesNoSqlService
 * (which writes to PostgreSQL) is left untouched — this service is the separate
 * MongoDB path required by the project NoSQL scope.
 *
 * All methods degrade gracefully: a null database or any exception produces an
 * empty result / null return. MongoDB unavailability never propagates to callers.
 */
public class PatientNotesMongoService {

    private static final AppLogger logger = AppLogger.getLogger(PatientNotesMongoService.class);
    private static final String COLLECTION = "patient_notes";

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Persists a new patient note and returns its generated note_id.
     * Returns null if the note text is blank or MongoDB is unavailable.
     */
    public String saveNote(String patientId, String appointmentId,
                           String authorUserId, String authorRole, String noteText) {
        if (noteText == null || noteText.isBlank()) return null;
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return null;

            String noteId = UUID.randomUUID().toString();
            Document doc = new Document()
                .append("note_id",        noteId)
                .append("patient_id",     patientId)
                .append("appointment_id", appointmentId)
                .append("author_user_id", authorUserId)
                .append("author_role",    authorRole)
                .append("note_text",      noteText.trim())
                .append("source",         "medical_records")
                .append("created_at",     new Date());
            db.getCollection(COLLECTION).insertOne(doc);
            return noteId;
        } catch (Exception e) {
            logger.warn("PatientNotesMongoService.saveNote failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Soft-deletes a note by setting deleted_at. The document remains in
     * MongoDB for audit purposes but is excluded from all find queries.
     */
    public void deleteNote(String noteId) {
        if (noteId == null || noteId.isBlank()) return;
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return;
            db.getCollection(COLLECTION).updateOne(
                Filters.eq("note_id", noteId),
                Updates.set("deleted_at", new Date())
            );
        } catch (Exception e) {
            logger.warn("PatientNotesMongoService.deleteNote failed: " + e.getMessage());
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Returns all active notes for a patient, newest first. */
    public List<PatientNoteDTO> findByPatientId(String patientId) {
        return query(Filters.and(
            Filters.eq("patient_id", patientId),
            Filters.exists("deleted_at", false)
        ));
    }

    /** Returns all active notes for an appointment, newest first. */
    public List<PatientNoteDTO> findByAppointmentId(String appointmentId) {
        return query(Filters.and(
            Filters.eq("appointment_id", appointmentId),
            Filters.exists("deleted_at", false)
        ));
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private List<PatientNoteDTO> query(Bson filter) {
        List<PatientNoteDTO> result = new ArrayList<>();
        try {
            MongoDatabase db = MongoConfig.getDatabase();
            if (db == null) return result;
            db.getCollection(COLLECTION)
              .find(filter)
              .sort(Sorts.descending("created_at"))
              .forEach(doc -> result.add(mapDoc(doc)));
        } catch (Exception e) {
            logger.warn("PatientNotesMongoService.query failed: " + e.getMessage());
        }
        return result;
    }

    private PatientNoteDTO mapDoc(Document doc) {
        PatientNoteDTO dto = new PatientNoteDTO();
        dto.setNoteId(doc.getString("note_id"));
        dto.setPatientId(doc.getString("patient_id"));
        dto.setAppointmentId(doc.getString("appointment_id"));
        dto.setAuthorUserId(doc.getString("author_user_id"));
        dto.setAuthorRole(doc.getString("author_role"));
        dto.setNoteText(doc.getString("note_text"));
        dto.setSource(doc.getString("source"));
        Date ca = doc.getDate("created_at");
        if (ca != null) {
            dto.setCreatedAt(ca.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        return dto;
    }
}