package example2

import example1.ConstructionSite

class Spaceship(val weight: Double, val crewMembers: Set[CrewMember])

trait CrewMember{
  val name: String
  val age: Int
}
class Scientist(override val name: String, override val age: Int) extends CrewMember
class Pilot(override val name: String, override val age: Int) extends CrewMember
class Captain(override val name: String, override val age: Int) extends CrewMember