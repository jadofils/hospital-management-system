package hospital.management.backend.mapper.doctor;

import hospital.management.backend.dto.doctor.CreateReferralDTO;
import hospital.management.backend.dto.doctor.ReferralDTO;
import hospital.management.backend.model.doctor.Referral;

public class ReferralMapper {

    public static ReferralDTO toDTO(Referral r) {
        if (r == null) return null;
        return new ReferralDTO(
            r.getReferralId(),
            r.getAppointmentId(),
            r.getReferringDoctorId(),
            r.getReferredToDoctorId(),
            r.getReason(),
            r.getStatus(),
            r.getCreatedAt()
        );
    }

    public static Referral toEntity(CreateReferralDTO dto) {
        if (dto == null) return null;
        Referral r = new Referral();
        r.setAppointmentId(dto.getAppointmentId());
        r.setReferringDoctorId(dto.getReferringDoctorId());
        r.setReferredToDoctorId(dto.getReferredToDoctorId());
        r.setReason(dto.getReason());
        r.setStatus("pending");
        return r;
    }
}