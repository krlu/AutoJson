package autojson.core

import example1._
import example2.{Captain, Pilot, Scientist, Spaceship}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._


class AutoSerializerTest extends AnyFlatSpec with Matchers{
  "AutoSerializer" should "serialize and deserialize a java object" in {
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
    val csJsonString = AutoSerializer.toJson(cs)
    val gtString =
      "{\"workers\":[" +
        "{\"name\":\"Brandie\",\"id\":\"asdf\",\"age\":100,\"className\":\"BrickLayer\"}," +
        "{\"name\":\"Ivan\",\"id\":\"qwer\",\"age\":100,\"className\":\"Inspector\"}," +
        "{\"name\":\"Eugenia\",\"id\":\"uiop\",\"age\":100,\"className\":\"Engineer\"}]," +
      "\"buildings\":[" +
        "{\"id\":\"b1\",\"rooms\":[{\"className\":\"Room\"},{\"className\":\"Room\"}],\"className\":\"Building\"}," +
        "{\"id\":\"b2\",\"rooms\":[{\"className\":\"Room\"},{\"className\":\"Room\"}],\"className\":\"Building\"}]," +
      "\"className\":\"ConstructionSite\"}"
    csJsonString shouldEqual gtString
    val csFromJson: ConstructionSite = AutoSerializer.jsonToObject(gtString, classOf[ConstructionSite], packageName = "example1")
    csFromJson.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    cs.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    csFromJson.buildings.asScala.map(_.id) shouldEqual cs.buildings.asScala.map(_.id)
    val result1 =  csFromJson.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName))
    val result2 = cs.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName))
    result1 shouldEqual result2
  }
  
  "AutoSerializer" should "serialize and deserialize a scala object" in {
    val crewMembers = Set(new Captain("luffy", 100), new Scientist("nami", 200), new Pilot("Usop", 300))
    val spaceShip = new Spaceship(3.14, crewMembers)
    val spaceShipJsonString = AutoSerializer.toJson(spaceShip)
    val gtString = "{\"weight\":3.14,\"crewMembers\":[{\"name\":\"luffy\",\"age\":100,\"className\":\"Captain\"},{\"name\":\"nami\",\"age\":200,\"className\":\"Scientist\"},{\"name\":\"Usop\",\"age\":300,\"className\":\"Pilot\"}],\"className\":\"Spaceship\"}"
    spaceShipJsonString shouldEqual gtString
    val spaceShipFromJson: Spaceship = AutoSerializer.jsonToObject(spaceShipJsonString, classOf[Spaceship], packageName = "example2")
    spaceShip.weight shouldEqual spaceShipFromJson.weight
    spaceShip.crewMembers.map(crew => (crew.name, crew.age)) shouldEqual spaceShipFromJson.crewMembers.map(crew => (crew.name, crew.age))

  }
}
