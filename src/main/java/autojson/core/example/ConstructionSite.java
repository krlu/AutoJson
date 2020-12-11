package autojson.core.example;


import java.util.Set;

public class ConstructionSite {
    public Set<Person> workers;
    public Set<Building> buildings;
    public ConstructionSite(Set<Person> workers, Set<Building> buildings){
        this.workers = workers;
        this.buildings = buildings;
    }
}
