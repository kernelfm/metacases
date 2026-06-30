package pw.fusionmine.fusioncases.case_system;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CaseGuiHolder implements InventoryHolder {

    private final CaseModel caseModel;
    private final Location caseLoc;

    public CaseGuiHolder(CaseModel caseModel, Location caseLoc) {
        this.caseModel = caseModel;
        this.caseLoc = caseLoc;
    }

    public Inventory getInventory() {
        return null;
    }

    public CaseModel getCaseModel() {
        return this.caseModel;
    }

    public Location getCaseLocation() {
        return this.caseLoc;
    }

}