package autojson.core.example2;

import java.util.Set;

public class Building {
    public String id;
    public Set<Room> rooms;
    public Building(String id, Set<Room> rooms){
        this.id = id;
        this.rooms = rooms;
    }
}
