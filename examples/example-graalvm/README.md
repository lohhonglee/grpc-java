gRPC Hostname Example with GraalVM
==================================

This example shows how to build the hostname example into a native application
using GraalVM's `native-image` tool.

The hostname example is a Hello World server whose response includes its
hostname. It also supports health and reflection services. This makes it a good
server to test infrastructure, like load balancing.

The example requires grpc-java to already be built. You are strongly encouraged
to check out a git release tag, since there will already be a build of grpc
available. Otherwise you must follow [COMPILING](../../COMPILING.md).

### Build the example

1. Build the hello-world example client. See [the examples README](../README.md)

2. Build this server as a native application. From the `grpc-java/examples/examples-hostname` directory:
```
$ ../gradlew installNativeImageDist
```

This creates the native application `build/install/native/hostname-server` that
runs the example.

To run the hostname example, run:

```
$ ./build/install/native/hostname-server
```

And in a different terminal window run the hello-world client:

```
$ ../build/install/examples/bin/hello-world-client
```

### Maven (not updated to support GraalVM compilation yet)

If you prefer to use Maven:
1. Build the hello-world example client. See [the examples README](../README.md)

2. Run in this directory:
```
$ mvn verify
$ # Run the server (from the examples-hostname directory)
$ mvn exec:java -Dexec.mainClass=io.grpc.examples.hostname.HostnameServer
$ # In another terminal run the client (from the examples directory)
$ cd ..
$ mvn exec:java -Dexec.mainClass=io.grpc.examples.helloworld.HelloWorldClient
```

### Bazel (not updated to support GraalVM compilation yet)

If you prefer to use Bazel, run from the `grpc-java/examples` directory:
```
$ bazel build :hello-world-client example-hostname:hostname-server
$ # Run the server
$ ./bazel-bin/example-hostname/hostname-server
$ # In another terminal run the client
$ ./bazel-bin/hello-world-client
```
