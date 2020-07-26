package com.zarucki.recruitment

import shapeless.tag
import shapeless.tag.@@

package object sensing {
 trait SensorIdTag

 type SensorId = String @@ SensorIdTag

 object SensorId {
  def apply(id: String): SensorId = tag[SensorIdTag][String](id)
 }
}
