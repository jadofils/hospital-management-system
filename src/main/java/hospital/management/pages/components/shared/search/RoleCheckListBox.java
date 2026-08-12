package hospital.management.pages.components.shared.search;

import hospital.management.backend.dto.auth.RoleDTO;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A checkbox per role, so a user can be assigned more than one role at once —
 * the database (`user_roles`) has always been many-to-many; this is the UI
 * finally exposing that instead of collapsing to a single-select dropdown.
 */
public class RoleCheckListBox extends VBox {

    private final Map<String, CheckBox> checkboxByRoleId = new LinkedHashMap<>();

    public RoleCheckListBox() {
        super(4);
    }

    /** Replaces the full set of selectable roles, clearing any prior selection state. */
    public void setOptions(List<RoleDTO> roles) {
        checkboxByRoleId.clear();
        getChildren().clear();
        for (RoleDTO role : roles) {
            CheckBox box = new CheckBox(role.getRoleName());
            checkboxByRoleId.put(role.getRoleId(), box);
            getChildren().add(box);
        }
    }

    /** Checks exactly the given role ids, leaving the rest unchecked. */
    public void setSelectedIds(Set<String> roleIds) {
        checkboxByRoleId.forEach((roleId, box) -> box.setSelected(roleIds.contains(roleId)));
    }

    /** The ids of every currently-checked role. */
    public Set<String> getSelectedIds() {
        return checkboxByRoleId.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
