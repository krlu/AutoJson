package autojson.core.example2;


import java.util.Set;

public class ConstructionSite {
    public Set<Worker> workers;
    public Set<Building> buildings;
    public ConstructionSite(Set<Worker> workers, Set<Building> buildings){
        this.workers = workers;
        this.buildings = buildings;
    }
}
