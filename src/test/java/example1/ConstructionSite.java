package example1;


import java.util.List;
import java.util.Set;

public class ConstructionSite {
    public Set<Person> workers;
    public List<Building> buildings;
    public ConstructionSite(Set<Person> workers, List<Building> buildings){
        this.workers = workers;
        this.buildings = buildings;
    }

    public int getFoo(){
        return 1;
    }
}
