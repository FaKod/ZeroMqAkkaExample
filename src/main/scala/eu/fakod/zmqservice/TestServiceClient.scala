package eu.fakod.zmqservice

import com.esotericsoftware.kryo.Kryo
import com.romix.scala.serialization.kryo.ScalaProductSerializer
import org.objenesis.strategy.StdInstantiatorStrategy
import java.io.{ByteArrayOutputStream, ByteArrayInputStream}
import com.esotericsoftware.kryo.io.{Output, Input}


object TestServiceClient extends App with ServiceClient {

  Server.start
  Thread.sleep(500)

  val k = new Kryo()
  k.setRegistrationRequired(false)
  k.register(classOf[ServiceResult], 1055470509)
  k.addDefaultSerializer(classOf[ServiceResult], classOf[ScalaProductSerializer])
  k.setInstantiatorStrategy(new StdInstantiatorStrategy())


  startClient(forConnect, 0, 10) {
    msg =>
      val input = new ByteArrayInputStream(msg.payload(1))
      val c = k.readClassAndObject(new Input(input)).asInstanceOf[ServiceResult]
      println((System.currentTimeMillis - c.time))
  }

  for (request_nbr <- 1 to 200) {
    val outStream = new ByteArrayOutputStream()
    val output = new Output(outStream, 256)
    k.writeClassAndObject(output, ServiceCall(System.currentTimeMillis))
    output.flush()
    sendMessage(ZMQMsg(Seq(MsgFrame("ServiceCall"), MsgFrame(output.getBuffer))))
  }


}
