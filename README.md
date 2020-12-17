# Auto-JSON 
Analyzes your Java/Scala object at runtime and serializes/deserializes to/from JSON. 
Without having to manually write serializer/deserialize boiler-plate

Core Feature: 
- Handles mixtures of Scala and Java objects by differentiating between language annotations
- Handles deserialization for Interface Types by preserving implementing classes labels in the JSON serialization.
- Builds on top of the Json4s library

Advantages over other JVM JSON libraries:

|                           Feature                              |     AutoJson  |    Jackson    |     GSON      |     Json4s    |  PlayJson   |   FlexJson  |    
| ---------------------------------------------------------------| ------------- | ------------- | ------------- | ------------- | ------------| ------------|
| De-serial/Serializing Scala Objects                            |      Yes      |      Yes      |      Yes      |       Yes     |      No     |      No     |
| De-serial/Serializing Java Objects                             |      Yes      |      Yes      |      Yes      |       No      |      No     |      No     |
| De-serial/Serializing Scala and Java mixed objects             |      Yes      |      Yes      |      No       |       No      |      No     |      No     |
| De-serializing For Classes with Abstract/Interface field Types |      Yes      |      Yes      |      No       |       No      |      No     |      No     |
| De-serializing Without Need for Manual Annotations             |      Yes      |      No       |      No       |       No      |      No     |      No     |


Upcoming features: 
- Support for Scala Case classes 
- Support for Tuples

## Java Example 
Consider the `ConstructionSite` Java class and its associated objects
```
import java.util.Set;

public class ConstructionSite {
    public Set<Person> workers;
    public Set<Building> buildings;
    public ConstructionSite(Set<Person> workers, Set<Building> buildings){
        this.workers = workers;
        this.buildings = buildings;
    }
}

public interface Person {
}


public abstract class Worker implements Person{
    public String name;
    public String id;
    public int age;
    public Worker(String name, String id, int age){
        this.name = name;
        this.id = id;
        this.age = age;
    }
}


public class Engineer extends Worker{
    public Engineer(String name, String id, int age) { super(name, id, age); }
}

public class Inspector extends Worker {
    public Inspector(String name, String id, int age) { super(name, id, age); }
}

public class BrickLayer extends Worker {
    public BrickLayer(String name, String id, int age) { super(name, id, age); }
}

```
If auto-json is on your classpath you can serialize `ConstructionSite` as follows:
```
import autojson.core
import example1._
import scala.jdk.CollectionConverters._

val bl: Person = new BrickLayer("Brandie", "asdf", 100)
val in: Person = new Inspector("Ivan", "qwer", 100)
val en: Person = new Engineer("Eugenia", "uiop", 100)
val b1r1 = new Room()
val b1r2 = new Room()
val build1 = new Building("b1", Set(b1r1, b1r2).asJava)
val b2r1 = new Room()
val b2r2 = new Room()
val build2 = new Building("b2", Set(b2r1, b2r2).asJava)
val buildings = Set(build1, build2).asJava
val workers = Set(bl, in, en).asJava
val cs = new ConstructionSite(workers, buildings)
val csJsonString = AutoSerializer.toJson(cs, prettyPrint = true)
```
csJsonString should have the following format:
```
{
  "workers":[
    {
      "name":"Brandie",
      "id":"asdf",
      "age":100,
      "className":"BrickLayer"
    },
    {
      "name":"Ivan",
      "id":"qwer",
      "age":100,
      "className":"Inspector"
    },
    {
      "name":"Eugenia",
      "id":"uiop",
      "age":100,
      "className":"Engineer"
    }
  ],
  "buildings":[
    {
      "id":"b1",
      "rooms":[
        {
          "className":"Room"
        },
        {
          "className":"Room"
        }
      ],
      "className":"Building"
    },
    {
      "id":"b2",
      "rooms":[
        {
          "className":"Room"
        },
        {
          "className":"Room"
        }
      ],
      "className":"Building"
    }
  ],
  "className":"ConstructionSite"
}
```
You can save the above string to a JSON file, then read from said file to recover the string.\
Finally, you can deserialize back to a `ConstructionSite` with the code below.
```
import autojson.core
import example1.ConstructionSite
val csFromJson: ConstructionSite = AutoSerializer.toObject(csJsonString, classOf[ConstructionSite])
``` 

## Scala Example
Serialization in Scala is identical. Consider the `SpaceShip` class along with its associated classes: 
```
class Spaceship(val weight: Double, val crewMembers: Set[CrewMember])

trait CrewMember{
  val name: String
  val age: Int
}
class Scientist(override val name: String, override val age: Int) extends CrewMember
class Pilot(override val name: String, override val age: Int) extends CrewMember
class Captain(override val name: String, override val age: Int) extends CrewMember
```
We then serialize the scala class `SpaceShip` as follows 
```
val crewMembers = Set(new Captain("luffy", 100), new Scientist("nami", 200), new Pilot("Usop", 300))
val spaceShip = new Spaceship(3.14, crewMembers)
val spJsonString = AutoSerializer.toJson(spaceShip, prettyPrint = true)
```

spJsonString should have the following format:   
```
{
  "crewMembers":[
    {
      "name":"luffy",
      "age":100,
      "className":"Captain"
    },
    {
      "name":"nami",
      "age":200,
      "className":"Scientist"
    },
    {
      "name":"Usop",
      "age":300,
      "className":"Pilot"
    }
  ],
  "weight":3.14,
  "className":"Spaceship"
}
```
Deserialization is also similar to the java example
```
import autojson.core
import example2.Spaceship
val spaceShipFromJson: SpaceShip = AutoSerializer.toObject(spaceShipJsonString, classOf[Spaceship])
```