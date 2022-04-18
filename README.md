# distributed-db-project

For my own education (and anyone that wants to follow) this project explores the creation of a
distributed system, specifically a simple database.

I plan to perform updates, providing the steps taken in each update here in the readme, as well as
tagging each version in git.

Oh, and by the way, don't use this anywhere for any reason.

## versions

### version 1.0.0

A simple client and server architecture for reading and writing key/value pairs. Data is stored only
in memory. Persistence will come in later version.

I've written a lot of web servers before but usually end up using some framework for it. In this
case,
just using some native java classes for server/http stuff.

This is hardly a database and certainly not distributed, but making it persistent and consistent is
next.

### version 2.0.0

Ok so this was a big one. I got a little carried away with reorganizing the project.

One of my goals is to be able to spin up a 2 or more nodes that are aware of eachother and able to
communicate with one another. But I do not want to have to explicitly tell each node about the other
nodes. This means that nodes need a way of "discovering" one another. I did a little research. There
are a few ways to do this. The one I have gone with is a simple registry-service. The registry is
a server that hangs out and receives requests to register nodes. Each node is given the address of
the registry service on startup. Nodes must register themselves with the registry. Eventually,
nodes will use the registry service to list all nodes in the cluster to facilitate communication.

One of many changes I included here is use of the fabric8 docker maven plugin to build docker
images. Now I can start a registry container

```shell
docker run steve000/distributed-db-project/registry:2.0.0
```

Then start a node container, passing the registry address as an argument

```shell
docker run steve000/distributed-db-project/node:2.0.0 -ra http://172.17.0.2:8080
```

And the node registers itself with the registry

```shell
[pool-1-thread-1] INFO io.steve000.distributed.db.registry.server.InMemoryRegistry - Registered node 7a735fec-2a39-471c-9f49-f719f6c5b36b at IP 172.17.0.3:8050
```