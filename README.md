Use Case
-----

* Non Akka Client
* Akka Server

Both Async, Router/Dealer pattern with Kryo serialization

run with

   mvn exec:java -Dexec.mainClass="eu.fakod.zmqservice.TestServiceClient"

Credits:

* [Kryo](https://code.google.com/p/kryo/)
* [Akka Kryo](https://github.com/romix/akka-kryo-serialization)
* [ZeroMQ 2.2](http://www.zeromq.org/)
* [Akka](http://akka.io/)
* [Akka Zeromq Server](https://github.com/jiminoc/akka-zeromq-server)
* [ZeroMQ Async Example](https://github.com/imatix/zguide/blob/master/examples/Scala/asyncsrv.scala)
* [Homebrew (install 0MQ)](http://mxcl.github.com/homebrew/)
* [install specific version](http://timnew.github.com/blog/2012/06/02/install-specific-version-of-tool-with-HomeBrew/)
* [I like](http://www.scala-lang.org/)