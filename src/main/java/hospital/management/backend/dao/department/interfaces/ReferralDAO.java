package hospital.management.backend.dao.department.interfaces;

import hospital.management.backend.model.doctor.Referral;

import java.util.List;
import java.util.Optional;

public interface ReferralDAO {
    Referral save(Referral referral) throws Exception;
    Optional<Referral> findById(String referralId) throws Exception;
    List<Referral> findByAppointmentId(String appointmentId) throws Exception;
    List<Referral> findByReferringDoctorId(String doctorId) throws Exception;
    List<Referral> findByReferredToDoctorId(String doctorId) throws Exception;
    Referral updateStatus(String referralId, String status) throws Exception;
    void softDelete(String referralId) throws Exception;
}