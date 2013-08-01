/*
 *  ControlMappings.scala
 *  (ScalaCollider)
 *
 *  Copyright (c) 2008-2013 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either
 *  version 2, june 1991 of the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License (gpl.txt) along with this software; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.synth

import collection.immutable.{IndexedSeq => Vec}
import language.implicitConversions

object ControlSetMap extends SingleControlSetMapImplicits with MultiControlSetMapImplicits {

  object Single extends SingleControlSetMapImplicits

  final case class Single(key: Any, value: Float)
    extends ControlSetMap {

    def toSetSeq : Vec[Any] = Vector(key,    value)
    def toSetnSeq: Vec[Any] = Vector(key, 1, value)
  }

  object Multi extends MultiControlSetMapImplicits

  final case class Multi(key: Any, values: Vec[Float])
    extends ControlSetMap {

    def toSetSeq : Vec[Any] = Vector(key, values)
    def toSetnSeq: Vec[Any] = key +: values.size +: values
  }
}

sealed trait ControlSetMap {
  def toSetSeq : Vec[Any]
  def toSetnSeq: Vec[Any]
}

private[synth] sealed trait SingleControlSetMapImplicits {
  implicit def intFloatControlSet    (tup: (Int   , Float )) = ControlSetMap.Single(tup._1, tup._2)
  implicit def intIntControlSet      (tup: (Int   , Int   )) = ControlSetMap.Single(tup._1, tup._2.toFloat)
  implicit def intDoubleControlSet   (tup: (Int   , Double)) = ControlSetMap.Single(tup._1, tup._2.toFloat)
  implicit def stringFloatControlSet (tup: (String, Float )) = ControlSetMap.Single(tup._1, tup._2)
  implicit def stringIntControlSet   (tup: (String, Int   )) = ControlSetMap.Single(tup._1, tup._2.toFloat)
  implicit def stringDoubleControlSet(tup: (String, Double)) = ControlSetMap.Single(tup._1, tup._2.toFloat)
}

private[synth] sealed trait MultiControlSetMapImplicits {
  implicit def intFloatsControlSet   (tup: (Int   , Seq[Float])) = ControlSetMap.Multi(tup._1, tup._2.toIndexedSeq)
  implicit def stringFloatsControlSet(tup: (String, Seq[Float])) = ControlSetMap.Multi(tup._1, tup._2.toIndexedSeq)
}

object ControlKBusMap {
  implicit def intIntControlKBus   (tup: (Int   , Int)) = Single(tup._1, tup._2)
  implicit def stringIntControlKBus(tup: (String, Int)) = Single(tup._1, tup._2)

  /** A mapping from an mono-channel control-rate bus to a synth control. */
  final case class Single(key: Any, index: Int)
    extends ControlKBusMap {

    def toMapSeq : Vec[Any] = Vector(key, index)
    def toMapnSeq: Vec[Any] = Vector(key, index, 1)
  }

  implicit def intKBusControlKBus   (tup: (Int   , ControlBus)) = Multi(tup._1, tup._2.index, tup._2.numChannels)
  implicit def stringKBusControlKBus(tup: (String, ControlBus)) = Multi(tup._1, tup._2.index, tup._2.numChannels)

  /** A mapping from an mono- or multi-channel control-rate bus to a synth control. */
  final case class Multi(key: Any, index: Int, numChannels: Int)
    extends ControlKBusMap {

    def toMapnSeq: Vec[Any] = Vector(key, index, numChannels)
  }
}

/** A mapping from a control-rate bus to a synth control. */
sealed trait ControlKBusMap {
  def toMapnSeq: Vec[Any]
}

object ControlABusMap {
  implicit def intIntControlABus   (tup: (Int   , Int)) = Single(tup._1, tup._2)
  implicit def stringIntControlABus(tup: (String, Int)) = Single(tup._1, tup._2)

  /** A mapping from an mono-channel audio bus to a synth control. */
  final case class Single(key: Any, index: Int)
    extends ControlABusMap {

    def toMapaSeq : Vec[Any] = Vector(key, index)
    def toMapanSeq: Vec[Any] = Vector(key, index, 1)
  }

  implicit def intABusControlABus   (tup: (Int   , AudioBus)) = Multi(tup._1, tup._2.index, tup._2.numChannels)
  implicit def stringABusControlABus(tup: (String, AudioBus)) = Multi(tup._1, tup._2.index, tup._2.numChannels)

  /** A mapping from an mono- or multi-channel audio bus to a synth control. */
  final case class Multi(key: Any, index: Int, numChannels: Int)
    extends ControlABusMap {

    def toMapanSeq: Vec[Any] = Vector(key, index, numChannels)
  }
}

/** A mapping from an audio bus to a synth control.
  *
  * Note that a mapped control acts similar to an `InFeedback` UGen in that it does not matter
  * whether the audio bus was written before the execution of the synth whose control is mapped or not.
  * If it was written before, no delay is introduced, otherwise a delay of one control block is introduced.
  *
  * @see  [[de.sciss.synth.ugen.InFeedback]]
  */
sealed trait ControlABusMap {
  def toMapanSeq: Vec[Any]
}
