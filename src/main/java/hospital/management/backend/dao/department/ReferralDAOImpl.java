package hospital.management.backend.dao.department;

import hospital.management.backend.dao.department.interfaces.ReferralDAO;
import hospital.management.backend.model.doctor.Referral;

import java.util.List;
import java.util.Optional;

public class ReferralDAOImpl implements ReferralDAO {

    @Override
    public Referral save(Referral referral) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<Referral> findById(String referralId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Referral> findByAppointmentId(String appointmentId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Referral> findByReferringDoctorId(String doctorId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Referral updateStatus(String referralId, String status) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void softDelete(String referralId) throws Exception {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}