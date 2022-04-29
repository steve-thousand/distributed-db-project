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

```text
[pool-1-thread-1] INFO io.steve000.distributed.db.registry.server.InMemoryRegistry - Registered node 7a735fec-2a39-471c-9f49-f719f6c5b36b at IP 172.17.0.3:8050
```

### version 2.1.0

Uhhhhh what am I doing this time.

Ultimate goal is to be able to communicate automatically between nodes so that we can distribute
data. Someone needs to coordinate that work, so I am thinking of
a [leader and follower pattern](https://martinfowler.com/articles/patterns-of-distributed-systems/leader-follower.html)
. For that to work, I will implement an election process. Soooooo let's do that!

And since writing that last paragraph I have done some reading and it turns out that elections are
complicated. I have read a bit
about [the bully algorithm](https://en.wikipedia.org/wiki/Bully_algorithm) and I think it makes
sense, but it only seems to describe what to do when a leader fails or no leader has yet been
determined across the whole cluster. What about when a new node is
added to a cluster, how does it know who the leader is? I am considering putting the leader
information into the registry so that any node can easily retrieve that information, but that makes
me wonder why election is needed if the registry can handle leader information. Can't the registry
handle appointment of new leaders rather than having nodes elect one?

I am further concerned by my inability to find information on:
* distributed system registries
  * https://jack-vanlightly.com/blog/2019/1/27/building-a-simple-distributed-system-the-protocol
  * https://microservices.io/patterns/service-registry.html
  * http://javaonfly.blogspot.com/2017/09/deep-dive-to-distributed-service.html
* how to add nodes to an existing cluster

i should really ask around about that. how does a cluster become aware of itself? how are its boundaries
technically defined?