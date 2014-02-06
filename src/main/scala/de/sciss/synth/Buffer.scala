/*
 *  Buffer.scala
 *  (ScalaCollider)
 *
 *  Copyright (c) 2008-2014 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU General Public License v2+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.synth

import de.sciss.synth.{Completion => Comp}
import de.sciss.osc.{Bundle, Packet}
import collection.immutable.{IndexedSeq => Vec}
import de.sciss.model.Model
import de.sciss.model.impl.ModelImpl

object Buffer {
  type Listener     = Model.Listener[BufferManager.BufferInfo]

  type Completion   = Comp[Buffer]
  val NoCompletion  = Comp[Buffer](None, None)

  def apply(server: Server = Server.default): Buffer = apply(server, allocID(server))

  private def allocID(server: Server): Int = {
    val id = server.allocBuffer(1)
    if (id == -1) {
      throw AllocatorExhausted(s"Buffer: failed to get a buffer allocated on ${server.name}")
    }
    id
  }
}
final case class Buffer(server: Server, id: Int) extends ModelImpl[BufferManager.BufferInfo] {

  //  def this(server: Server = Server.default) = this(server, Buffer.allocID(server))

  import Buffer._

  private var released        = false
  private var numFramesVar    = -1
  private var numChannelsVar  = -1
  private var sampleRateVar   = 0f

  private val sync            = new AnyRef

  override def toString = s"Buffer($server,$id" +
    (if (numFramesVar >= 0) s") : <$numFramesVar,$numChannelsVar,$sampleRateVar>" else ")")

  def numFrames   = numFramesVar
  def numChannels = numChannelsVar
  def sampleRate  = sampleRateVar

  def register(): Unit = server.bufManager.register(this)

  private[synth] def updated(change: BufferManager.BufferInfo): Unit =
    sync.synchronized {
      val info        = change.info
      numFramesVar    = info.numFrames
      numChannelsVar  = info.numChannels
      sampleRateVar   = info.sampleRate
      dispatch(change)
    }

  def queryMsg = message.BufferQuery(id)

  def freeMsg: message.BufferFree = freeMsg(None, release = true)

  /**
   * @param   release  whether the buffer id should be immediately returned to the id-allocator or not.
   *                   if you build a system that monitors when bundles are really sent to the server,
   *                   and you need to deal with transaction abortion, you might want to pass in
   *                   <code>false</code> here, and manually release the id, using the <code>release</code>
   *                   method
   */
  def freeMsg(completion: Optional[Packet] = None, release: Boolean = true): message.BufferFree =
    sync.synchronized {
      if (release) this.release()
      message.BufferFree(id, completion)
    }

  /** Releases the buffer id to the id-allocator pool, without sending any
    * OSCMessage. Use with great care.
    */
  def release(): Unit =
    sync.synchronized {
      if (released) sys.error(this.toString + " : has already been freed")
      server.freeBuffer(id)
      released = true
    }

  def closeMsg: message.BufferClose = closeMsg(None)

  def closeMsg(completion: Optional[Packet] = None) = message.BufferClose(id, completion)

  def allocMsg(numFrames: Int, numChannels: Int = 1, completion: Optional[Packet] = None): message.BufferAlloc =
    sync.synchronized {
      numFramesVar    = numFrames
      numChannelsVar  = numChannels
      sampleRateVar   = server.sampleRate.toFloat
      message.BufferAlloc(id, numFrames, numChannels, completion)
    }

  def allocReadMsg(path: String, startFrame: Int = 0, numFrames: Int = -1,
                   completion: Optional[Packet] = None) =
    message.BufferAllocRead(id, path, startFrame, numFrames, completion)

  def allocReadChannelMsg(path: String, startFrame: Int = 0, numFrames: Int = -1, channels: Seq[Int],
                          completion: Optional[Packet] = None) =
    message.BufferAllocReadChannel(id, path, startFrame, numFrames, channels.toList, completion)

  def cueMsg(path: String, startFrame: Int = 0, completion: Completion = NoCompletion) =
    message.BufferRead(id, path, startFrame, numFrames, 0, leaveOpen = true, completion = makePacket(completion))

  def readMsg(path: String, fileStartFrame: Int = 0, numFrames: Int = -1, bufStartFrame: Int = 0,
              leaveOpen: Boolean = false, completion: Optional[Packet] = None) =
    message.BufferRead(id, path, fileStartFrame, numFrames, bufStartFrame, leaveOpen, completion)

  def readChannelMsg(path: String, fileStartFrame: Int = 0, numFrames: Int = -1, bufStartFrame: Int = 0,
                     leaveOpen: Boolean = false, channels: Seq[Int],
                     completion: Optional[Packet] = None) =
    message.BufferReadChannel(id, path, fileStartFrame, numFrames, bufStartFrame, leaveOpen, channels.toList,
      completion)

  def setMsg(pairs: (Int, Float)*) = message.BufferSet(id, pairs: _*)

  def setnMsg(v: Vec[Float]) = message.BufferSetn(id, (0, v.toIndexedSeq))

  def setnMsg(pairs: (Int, Vec[Float])*) = {
    //    val numSmp = numChannels * numFrames
    //    require(pairs.forall(tup => (tup._1 >= 0 && (tup._1 + tup._2.size) <= numSmp)))
    val ipairs = pairs.map(tup => (tup._1, tup._2.toIndexedSeq))
    message.BufferSetn(id, ipairs: _*)
  }

  /** Convenience method for creating a fill message for one given range */
  def fillMsg(index: Int, num: Int, value: Float) = message.BufferFill(id, message.BufferFill.Info(index, num, value))

  def fillMsg(infos: message.BufferFill.Info*) = message.BufferFill(id, infos: _*)

  def zeroMsg: message.BufferZero = zeroMsg(None)

  def zeroMsg(completion: Optional[Packet]) = message.BufferZero(id, completion)

  def writeMsg(path: String, fileType: io.AudioFileType = io.AudioFileType.AIFF,
               sampleFormat: io.SampleFormat = io.SampleFormat.Float, numFrames: Int = -1, startFrame: Int = 0,
               leaveOpen: Boolean = false, completion: Optional[Packet] = None) =
    message.BufferWrite(id, path, fileType, sampleFormat, numFrames, startFrame, leaveOpen, completion)

  def makePacket(completion: Completion, forceQuery: Boolean = false): Option[Packet] = {
    val a = completion.action
    if (forceQuery || a.isDefined) {
      register()
      a.foreach { action =>
        lazy val l: Buffer.Listener = {
          case BufferManager.BufferInfo(_, _) =>
            removeListener(l)
            action(this)
        }
        addListener(l)
      }
    }
    (completion.message, a) match {
      case (None, None)           => if (forceQuery) Some(queryMsg) else None
      case (Some(msg), None)      => Some(if (forceQuery) Bundle.now(msg(this), queryMsg) else msg(this))
      case (None, Some(act))      => Some(queryMsg)
      case (Some(msg), Some(act)) => Some(Bundle.now(msg(this), queryMsg))
    }
  }
}