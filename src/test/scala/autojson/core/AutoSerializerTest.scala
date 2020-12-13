package autojson.core

import java.util

import example1._
import example2.{Captain, Pilot, Scientist, Spaceship}
import example3.{Assemblyman, City, Councilor, Mayor}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._


class AutoSerializerTest extends AnyFlatSpec with Matchers{

  val bl: Person = new BrickLayer("Brandie", "asdf", 100)
  val in: Person = new Inspector("Ivan", "qwer", 100)
  val en: Person = new Engineer("Eugenia", "uiop", 100)
  val b1r1 = new Room()
  val b1r2 = new Room()
  val build1 = new Building("b1", Set(b1r1, b1r2).asJava)
  val b2r1 = new Room()
  val b2r2 = new Room()
  val build2 = new Building("b2", Set(b2r1, b2r2).asJava)
  val buildings: util.Set[Building] = Set(build1, build2).asJava
  val workers: util.Set[Person] = Set(bl, in, en).asJava
  val cs = new ConstructionSite(workers, buildings)
  "AutoSerializer" should "serialize and deserialize a java object while ignoring parameter-less methods" in {
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
    val csFromJson: ConstructionSite = AutoDeserializer.jsonToObject(csJsonString, classOf[ConstructionSite])
    csFromJson.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    cs.workers.asScala.toList.foreach{ w =>
      assert(List("BrickLayer", "Inspector", "Engineer").contains(w.getClass.getSimpleName))
    }
    compareConstructionSites(csFromJson, cs)
  }
  
  "AutoSerializer" should "serialize and deserialize a scala object" in {
    val crewMembers = Set(new Captain("luffy", 100), new Scientist("nami", 200), new Pilot("Usop", 300))
    val spaceShip = new Spaceship(3.14, crewMembers)
    val spaceShipJsonString = AutoSerializer.toJson(spaceShip)
    val spaceShipFromJson: Spaceship = AutoDeserializer.jsonToObject(spaceShipJsonString, classOf[Spaceship])
    spaceShip.weight shouldEqual spaceShipFromJson.weight
    spaceShip.crewMembers.map(crew => (crew.name, crew.age)) shouldEqual spaceShipFromJson.crewMembers.map(crew => (crew.name, crew.age))
  }

  "AutoSerializer" should "serialize and deserialize a scala object with an underlying java field in different package" in {
    val crewMembers = Set(new Mayor("Spike", 100), new Councilor("Faye", 200), new Assemblyman("Edward", 300))
    val city = new City("GrandLine", 1999, crewMembers, cs)
    val cityJsonString = AutoSerializer.toJson(city, prettyPrint = true)
    val cityFromJson: City = AutoDeserializer.jsonToObject(cityJsonString, classOf[City])
    city.name shouldEqual cityFromJson.name
    city.foundingYear shouldEqual cityFromJson.foundingYear
    compareConstructionSites(city.constructionSite, cs)
  }

  private def compareConstructionSites(cs1: ConstructionSite, cs2: ConstructionSite): Unit = {
    cs1.buildings.asScala.map(_.id) shouldEqual cs2.buildings.asScala.map(_.id)
    val result1 =  cs1.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName))
    val result2 = cs2.workers.asScala.map(_.asInstanceOf[Worker]).map(w => (w.age, w.id, w.name, w.getClass.getSimpleName))
    result1 shouldEqual result2
  }
}
