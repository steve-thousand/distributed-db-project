# distributed-db-project

For my own education (and anyone that wants to follow) this project explores the creation of a
distributed system, specifically a simple database.

I plan to perform updates, providing the steps taken in each update here in the readme, as well as
tagging each version in git.

Oh, and by the way, don't use this anywhere for any reason. To clarify, I mean that I don't
recommend that you rely on this code for anything you may be building. This is an educational
project. You are free to use it in any way you want, but I make no guarantees.

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

Sorry that this ended up being such a big one. This includes adding a new "cluster" module that
includes the code that will be used by each node to discover and communicate. Making it its own
module hopefully will allow it to be used despite what the cluster is used for (data replication,
distribution, workflows etc...).

Ultimate goal is to be able to communicate automatically between nodes so that we can distribute
data. Someone needs to coordinate that work, so I am thinking of
a [leader and follower pattern](https://martinfowler.com/articles/patterns-of-distributed-systems/leader-follower.html)
. For that to work, I will implement an election process. Soooooo let's do that!

And since writing that last paragraph I have done some reading and it turns out that elections are
complicated. I have implemented a [bully algorithm](https://en.wikipedia.org/wiki/Bully_algorithm)
for choosing a leader. On startup, every node waits some period of time to receive heartbeat from a
leader node. Any node that receives a heartbeat identifies the sending node as the new leader. If it
receives none in a configured window of time (most likely to happen if it is the first node in the
cluster), it starts an election process.

The election process works like this:

1. the node that begins the election considers all registered nodes.
2. if there are any higher-id nodes (currently using alphabetic order of names) they are all
   pinged for live-ness. if any of those returns a timely response, the node further waits to
   receive a victory response. if a victory response is received in that time, we have found our
   leader.
3. if there are no higher-id live-nodes or we do not hear a victory message from them soon enough,
   then this node is the leader. all lower-id nodes are sent victory messages.

Of course this requires some changes to other parts of the platform as well. Now that I've entered
the servers-being-lost territory, the registry and the servers that use it for service discovery
need some changes to be able to forget nodes, otherwise I'll end up with error logs endlessly.

It wasn't easy and I'm sure there are race conditions lurking there somewhere but I've got it set
up so that multiple nodes can be started with nothing but a registry parameter, and at run time
nodes will discover each-other, and a leader will be chosen. If the leader fails, a new leader
will be chosen.

### version 2.2.0

Still working towards the goal of data replication.

It is my understanding of the leader/follower pattern that write requests should go through the
leader, which replicates data to the followers. In that way, read requests can be done against any
node.

I have set up a simple replication layer. It is not fault-tolerant, and it is not transactional. But
I've tested it locally with a cluster of 3 nodes (plus 1 register node). Writes go through the
leader and then are successfully replicated to the followers.

I am also not sure what to do about replicating existing data to new nodes. It's one of my goals to
be able to add new nodes to a cluster and have it replicate the data.

Future improvements will include making it durable and transactional.

### version 3.0.0

To enable replication and ensure consensus across nodes, I am using [the raft
algorithm](https://en.wikipedia.org/wiki/Raft_(algorithm)), or at least some
half-baked form of it. Each node has a replication log, and the leader sends
client actions to each follower to be appended to their logs. Only once all
(or ideally a majority) of followers have successfully appended to their logs
does the leader commit to its log, performing the entry action and responding to
the client. At that point followers are told to commit as well.

[This is a pretty awesome visual demonstration of the raft algorithm](http://thesecretlivesofdata.com/raft/)

I've also set it up so that new nodes can request a replication log sync with
the leader. This sync action could be used to repair disagreements between
nodes as well.