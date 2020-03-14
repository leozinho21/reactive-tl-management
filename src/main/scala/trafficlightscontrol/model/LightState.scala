package trafficlightscontrol.model

sealed abstract class LightState(val colour: String, val id: String) {
  override val toString: String = s"$colour"
}

case object RedLight extends LightState("RedLight", "R")
case object GreenLight extends LightState("GreenLight", "G")
case object OrangeLight extends LightState("OrangeLight", "Y")
case object ChangingToGreenLight extends LightState("ChangingToGreenLight", "Y")
