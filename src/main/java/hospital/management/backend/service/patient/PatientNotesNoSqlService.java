package hospital.management.backend.service.patient;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import hospital.management.backend.config.EnvConfig;
import org.bson.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * NoSQL patient notes mirror used by doctor/admin note-taking flows.
 */
public class PatientNotesNoSqlService {

    private final MongoCollection<Document> notesCollection;

    public PatientNotesNoSqlService() {
        MongoClient client = MongoClients.create(EnvConfig.getMongoUri());
        MongoDatabase db = client.getDatabase("hospital");
        this.notesCollection = db.getCollection("patient_notes");
    }

    public String saveNote(String patientId, String appointmentId, String authorUserId, String authorRole, String noteText) {
        if (noteText == null || noteText.trim().isEmpty()) {
            return null;
        }

        String noteId = UUID.randomUUID().toString();
        Document doc = new Document()
                .append("note_id", noteId)
                .append("patient_id", patientId)
                .append("appointment_id", appointmentId)
                .append("author_user_id", authorUserId)
                .append("author_role", authorRole)
                .append("note_text", noteText)
                .append("source", "medical_records")
                .append("created_at", Instant.now().toString());

        notesCollection.insertOne(doc);
        return noteId;
    }
}
