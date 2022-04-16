# distributed-db-project

For my own education (and anyone that wants to follow) this project explores the creation of a
distributed system, specifically a simple database.

I plan to perform updates, providing the steps taken in each update here in the readme, as well as
tagging each version in git.

## versions

### version 1

A simple client and server architecture for reading and writing key/value pairs. Data is stored only
in memory. Persistence will come in later version.

I've written a lot of web servers before but usually end up using some framework for it. In this
case,
just using some native java classes for server/http stuff.

This is hardly a database and certainly not distributed, but making it persistent and consistent is
next.