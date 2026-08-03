package hospital.management.backend.dao.patient;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import hospital.management.backend.config.EnvConfig;
import hospital.management.backend.dao.patient.interfaces.PatientFeedbackDAO;
import hospital.management.backend.exceptions.DatabaseException;
import hospital.management.backend.exceptions.ResourceNotFoundException;
import hospital.management.backend.model.patient.PatientFeedback;
import org.bson.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** MongoDB implementation for patient_feedback collection */
public class PatientFeedbackDAOImpl implements PatientFeedbackDAO {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> feedbackCollection;

    public PatientFeedbackDAOImpl() {
        this.mongoClient = MongoClients.create(EnvConfig.getMongoUri());
        MongoDatabase db = mongoClient.getDatabase("hospital");
        this.feedbackCollection = db.getCollection("patient_feedback");
    }

    @Override
    public PatientFeedback save(PatientFeedback feedback) throws Exception {
        try {
            String id = feedback.getFeedbackId() != null ? feedback.getFeedbackId() : UUID.randomUUID().toString();
            
            Document doc = new Document()
                .append("feedback_id", id)
                .append("submitted_by", feedback.getSubmittedBy())
                .append("patient_id", feedback.getPatientId())
                .append("appointment_id", feedback.getAppointmentId())
                .append("rating", feedback.getRating())
                .append("comments", feedback.getComments())
                .append("date_submitted", feedback.getDateSubmitted() != null 
                    ? Date.from(feedback.getDateSubmitted().atStartOfDay(ZoneId.systemDefault()).toInstant())
                    : new Date())
                .append("created_at", Instant.now().toString())
                .append("updated_at", Instant.now().toString())
                .append("deleted_at", null);

            feedbackCollection.insertOne(doc);
            
            feedback.setFeedbackId(id);
            if (feedback.getDateSubmitted() == null) {
                feedback.setDateSubmitted(LocalDate.now());
            }
            
            return feedback;
        } catch (Exception e) {
            throw new DatabaseException("Failed to save patient feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<PatientFeedback> findById(String feedbackId) throws Exception {
        try {
            Document doc = feedbackCollection.find(
                Filters.and(
                    Filters.eq("feedback_id", feedbackId),
                    Filters.eq("deleted_at", null)
                )
            ).first();
            
            return doc != null ? Optional.of(documentToFeedback(doc)) : Optional.empty();
        } catch (Exception e) {
            throw new DatabaseException("Failed to find patient feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PatientFeedback> findAll() throws Exception {
        try {
            List<PatientFeedback> feedbacks = new ArrayList<>();
            feedbackCollection.find(Filters.eq("deleted_at", null))
                .sort(new Document("date_submitted", -1).append("created_at", -1))
                .forEach(doc -> feedbacks.add(documentToFeedback(doc)));
            return feedbacks;
        } catch (Exception e) {
            throw new DatabaseException("Failed to list patient feedback: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PatientFeedback> findAll() throws Exception {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM patient_feedback WHERE deleted_at IS NULL ORDER BY date_submitted DESC, created_at DESC";
        List<PatientFeedback> feedbacks = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) feedbacks.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to list patient feedback: " + e.getMessage(), e);
        }
        return feedbacks;
    }

    @Override
    public List<PatientFeedback> findByPatientId(String patientId) throws Exception {
        try {
            List<PatientFeedback> feedbacks = new ArrayList<>();
            feedbackCollection.find(
                Filters.and(
                    Filters.eq("patient_id", patientId),
                    Filters.eq("deleted_at", null)
                )
            ).sort(new Document("date_submitted", -1).append("created_at", -1))
             .forEach(doc -> feedbacks.add(documentToFeedback(doc)));
            return feedbacks;
        } catch (Exception e) {
            throw new DatabaseException("Failed to find feedback by patient: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PatientFeedback> findByAppointmentId(String appointmentId) throws Exception {
        try {
            List<PatientFeedback> feedbacks = new ArrayList<>();
            feedbackCollection.find(
                Filters.and(
                    Filters.eq("appointment_id", appointmentId),
                    Filters.eq("deleted_at", null)
                )
            ).sort(new Document("date_submitted", -1).append("created_at", -1))
             .forEach(doc -> feedbacks.add(documentToFeedback(doc)));
            return feedbacks;
        } catch (Exception e) {
            throw new DatabaseException("Failed to find feedback by appointment: " + e.getMessage(), e);
        }
    }

    @Override
    public void softDelete(String feedbackId) throws Exception {
        try {
            Document update = new Document("$set", new Document("deleted_at", Instant.now().toString()));
            var result = feedbackCollection.updateOne(
                Filters.and(
                    Filters.eq("feedback_id", feedbackId),
                    Filters.eq("deleted_at", null)
                ),
                update
            );
            
            if (result.getModifiedCount() == 0) {
                throw new ResourceNotFoundException("PatientFeedback", feedbackId);
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new DatabaseException("Failed to delete patient feedback: " + e.getMessage(), e);
        }
    }

    private PatientFeedback documentToFeedback(Document doc) {
        PatientFeedback feedback = new PatientFeedback();
        feedback.setFeedbackId(doc.getString("feedback_id"));
        feedback.setSubmittedBy(doc.getString("submitted_by"));
        feedback.setPatientId(doc.getString("patient_id"));
        feedback.setAppointmentId(doc.getString("appointment_id"));
        feedback.setRating(doc.getInteger("rating"));
        feedback.setComments(doc.getString("comments"));
        
        Date dateSubmitted = doc.getDate("date_submitted");
        if (dateSubmitted != null) {
            feedback.setDateSubmitted(dateSubmitted.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        
        // MongoDB stores ISO strings for timestamps
        String createdAt = doc.getString("created_at");
        if (createdAt != null) {
            feedback.setCreatedAt(Instant.parse(createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        String updatedAt = doc.getString("updated_at");
        if (updatedAt != null) {
            feedback.setUpdatedAt(Instant.parse(updatedAt).atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        String deletedAt = doc.getString("deleted_at");
        if (deletedAt != null) {
            feedback.setDeletedAt(Instant.parse(deletedAt).atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return feedback;
    }
}
