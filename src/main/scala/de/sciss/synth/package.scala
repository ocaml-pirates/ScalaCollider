/*
 *  package.scala
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

package de.sciss

import language.implicitConversions

/** The `synth` package provides some type enrichments. In particular converting numbers to
  * constant graph elements, operators on graph elements and allowing succinct creation of named controls.
  * Furthermore, it contains the `play` function to quickly test graph functions.
  */
package object synth {
  /** Positive `Float` infinity. Useful for sequence based demand UGens.
    * `-inf` gives you negative infinity.
    */
  final val inf = Float.PositiveInfinity

  // ---- implicit ----

  /* This conversion is particularly important to balance priorities,
   * as the plain pair of `intToGE` and `enrichFloat` have equal
   * priorities for an Int despite being in sub/superclass relationship,
   * probably due to the numeric widening which would be needed.
   *
   * Note that we use the same name as scala.Predef.intWrapper. That
   * way the original conversion is hidden!
   */
  implicit def intGEWrapper       (i: Int   ): synth  .RichInt    = new synth  .RichInt   (i)
  implicit def floatGEWrapper     (f: Float ): synth  .RichFloat  = new synth  .RichFloat (f)
  implicit def doubleGEWrapper    (d: Double): synth  .RichDouble = new synth  .RichDouble(d)
  implicit def intNumberWrapper   (i: Int   ): numbers.RichInt    = new numbers.RichInt   (i)
  implicit def floatNumberWrapper (f: Float ): numbers.RichFloat  = new numbers.RichFloat (f)
  implicit def doubleNumberWrapper(d: Double): numbers.RichDouble = new numbers.RichDouble(d)

  /** Provides operators for graph elements, such as `.abs`, `.linlin` or `.poll`. */
  implicit def geOps(g: GE): GEOps = new GEOps(g)

  // XXX TODO: ControlProxyFactory could be implicit class?
  /** Allows the construction or named controls, for example via `"freq".kr`. */
  implicit def stringToControlProxyFactory(name: String): ugen.ControlProxyFactory = new ugen.ControlProxyFactory(name)

  // ---- explicit ----

  /** Wraps the body of the thunk argument in a `SynthGraph`, adds an output UGen, and plays the graph
    * on the default group of the default server.
    *
    * @param  thunk   the thunk which produces the UGens to play
    * @return         a reference to the spawned Synth
    */
  def play[T: GraphFunction.Result](thunk: => T): Synth = play()(thunk)

  /** Wraps the body of the thunk argument in a `SynthGraph`, adds an output UGen, and plays the graph
    * in a synth attached to a given target.
    *
    * @param  target      the target with respect to which to place the synth
    * @param  addAction   the relation between the new synth and the target
    * @param  outBus      audio bus index which is used for the synthetically generated `Out` UGen.
    * @param  fadeTime    if defined, specifies the fade-in time for a synthetically added amplitude envelope.
    * @param  thunk       the thunk which produces the UGens to play
    * @return             a reference to the spawned Synth
    */
  def play[T: GraphFunction.Result](target: Node = Server.default, outBus: Int = 0,
                                    fadeTime: Optional[Float] = Some(0.02f),
                                    addAction: AddAction = addToHead)(thunk: => T): Synth = {
    val fun = new GraphFunction[T](thunk)
    fun.play(target, outBus, fadeTime, addAction)
  }
}