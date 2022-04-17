# distributed-db-project

For my own education (and anyone that wants to follow) this project explores the creation of a
distributed system, specifically a simple database.

I plan to perform updates, providing the steps taken in each update here in the readme, as well as
tagging each version in git.

Oh, and by the way, don't use this anywhere for any reason.

## versions

### version 1

A simple client and server architecture for reading and writing key/value pairs. Data is stored only
in memory. Persistence will come in later version.

I've written a lot of web servers before but usually end up using some framework for it. In this
case,
just using some native java classes for server/http stuff.

This is hardly a database and certainly not distributed, but making it persistent and consistent is
next.

### version 2

I would like next to have the ability to have data distributed across more than one instance. So
that, though I am not making heavy usage of the database, I can write 2 or more key/value pairs, and
they will be balanced across multiple servers.

I am curious to know how this kind of balancing can be orchestrated. Is the client responsible for
knowing which of 2 or more servers to write to? Or is there something server-side that makese that
decision?

My instinct in this case is to keep the client as simple as possible. Perhaps a leader server is
responsible for knowing the space used on each server, in which case it decides which node to write
to, and passes client calls onward.

But before _that_ can happen, I think it is important for nodes in the cluster to be able to be
discovered. I would like to be able to have 2 or more nodes in my cluster without having to tell each
node the IP addresses of all other nodes.

To do this I will be using a service registry, a server that is currently only responsible for 
knowing the location and status of all nodes in a cluster. All nodes are aware of this registry, and
report themselves, maybe periodically, to the registry.