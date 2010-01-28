/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.tint.sc

import scala.collection.immutable.{ Queue }

trait Model {
  private var listeners: Queue[ AnyRef => Unit ] = Queue.Empty
//  private val sync = new AnyRef

//  def name: String

  protected def dispatch( change: AnyRef ) {
      listeners.foreach( l => try {
        l.apply( change )
      } catch {
        case e: MatchError => // ignored
        case e => e.printStackTrace() // catch, but print
      })
  }

  def addListener( l: AnyRef => Unit ) {
//    sync.synchronized {
      listeners = listeners.enqueue( l )
//    }
  }

  def removeListener( l: AnyRef => Unit ) {
    var filtered: Queue[ AnyRef => Unit ] = Queue.Empty
//    sync.synchronized {
      listeners.foreach( x => if( x != l )
        filtered = filtered.enqueue( x )) // ugly; no easier way??
      listeners = filtered
//    }
  }
}