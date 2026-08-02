package hospital.management.backend.mapper.doctor;

import hospital.management.backend.dto.doctor.CreateDepartmentDTO;
import hospital.management.backend.dto.doctor.DepartmentDTO;
import hospital.management.backend.model.doctor.Department;

public class DepartmentMapper {

    public static DepartmentDTO toDTO(Department d) {
        if (d == null) return null;
        return new DepartmentDTO(
            d.getDepartmentId(),
            d.getName(),
            d.getLocation(),
            d.getPhone(),
            d.getCreatedAt()
        );
    }

    public static Department toEntity(CreateDepartmentDTO dto) {
        if (dto == null) return null;
        Department d = new Department();
        d.setName(dto.getName());
        d.setLocation(dto.getLocation());
        d.setPhone(dto.getPhone());
        return d;
    }
}