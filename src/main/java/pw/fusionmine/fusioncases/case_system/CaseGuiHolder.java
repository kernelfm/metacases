package pw.fusionmine.fusioncases.case_system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@AllArgsConstructor
@Getter
public class CaseGuiHolder implements InventoryHolder {

    private final CaseModel caseModel;
    private final Location caseLocation;

    public Inventory getInventory() {
        return null;
    }

}