package eu.fakod.zmqservice

import java.util.concurrent.LinkedBlockingQueue
import org.zeromq.ZMQ
import org.zeromq.ZMQ.Socket
import annotation.tailrec


/**
 *
 */
trait ServiceClient {

  private val queue = new LinkedBlockingQueue[ZMQMsg]()

  private var _addr: String = _
  private var _callback: (ZMQMsg) => Unit = _
  private var _socket: Socket = _
  private var _timeout: Long = _
  private var _pollTimeoutDuration: Long = _
  private var _worker: Thread = _


  private val worker = new Runnable {
    def run() {
      val context = ZMQ.context(1)
      _socket = context.socket(ZMQ.DEALER)
      _socket.connect(_addr)
      val poller = context.poller(1)
      poller.register(_socket, ZMQ.Poller.POLLIN)

      while (true) {
        poller.poll(_timeout)
        while (poller.pollin(0)) {
          val msg = ZMQMsg(_socket)
          if (msg.frames.size > 0)
            _callback(msg)
        }
        Thread.sleep(_pollTimeoutDuration)
        var p = queue.poll()
        if (p != null) {
          do {
            sendMessage(p.frames)
            p = queue.poll()
          } while (p != null)
        }

      }
    }
  }

  def startClient(addr: String, timeout: Long, pollTimeoutDuration: Long)(callback: (ZMQMsg) => Unit) = {
    _addr = addr
    _callback = callback
    _timeout = timeout
    _pollTimeoutDuration = pollTimeoutDuration
    _worker = new Thread(worker)
    _worker.start
  }

  def sendMessage(message: ZMQMsg) = queue.put(message)

  private def sendMessage(frames: Seq[MsgFrame]) {
    def sendBytes(bytes: Seq[Byte], flags: Int) = _socket.send(bytes.toArray, flags)
    val iter = frames.iterator
    while (iter.hasNext) {
      val payload = iter.next.payload
      val flags = if (iter.hasNext) ZMQ.SNDMORE else 0
      sendBytes(payload, flags)
    }
  }
}


object MsgFrame {
  def apply(text: String): MsgFrame = new MsgFrame(text)
}


/**
 *
 * @param payload
 */
case class MsgFrame(payload: Seq[Byte]) {
  def this(bytes: Array[Byte]) = this(bytes.toSeq)

  def this(text: String) = this(text.getBytes("UTF-8"))
}


/**
 *
 * @param frames
 */
case class ZMQMsg(frames: Seq[MsgFrame]) {

  def this(frame: MsgFrame) = this(Seq(frame))

  def this(frame1: MsgFrame, frame2: MsgFrame) = this(Seq(frame1, frame2))

  def this(frameArray: Array[MsgFrame]) = this(frameArray.toSeq)

  /**
   * Convert the bytes in the first frame to a String, using specified charset.
   */
  def firstFrameAsString(charsetName: String): String = new String(frames.head.payload.toArray, charsetName)

  /**
   * Convert the bytes in the first frame to a String, using "UTF-8" charset.
   */
  def firstFrameAsString: String = firstFrameAsString("UTF-8")

  def payload(frameIndex: Int): Array[Byte] = frames(frameIndex).payload.toArray
}


/**
 *
 */
object ZMQMsg {
  def apply(bytes: Array[Byte]): ZMQMsg = ZMQMsg(Seq(MsgFrame(bytes)))

  def apply(socket: Socket): ZMQMsg = ZMQMsg(receiveMessage(socket))

  @tailrec
  private def receiveMessage(socket: Socket, currentFrames: Vector[MsgFrame] = Vector.empty): Seq[MsgFrame] = {
    socket.recv(ZMQ.NOBLOCK) match {
      case null ⇒
        if (currentFrames.isEmpty) currentFrames
        else throw new IllegalStateException("no more frames available while socket.hasReceivedMore==true")
      case bytes ⇒
        val frames = currentFrames :+ MsgFrame(if (bytes.length == 0) Array[Byte]() else bytes)
        if (socket.hasReceiveMore) receiveMessage(socket, frames) else frames
    }
  }
}

