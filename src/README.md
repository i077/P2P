# EECS 325 Project 1: Peer-to-Peer Network

Imran Hossain 

---

This project implements a UDP discovery protocol, so when running the P2P network,
connections are formed by typing `connect <IP> <port>` into the shell.

All peers are configured to accept connections at port 50320, so connect commands
should really just be `connect <IP> 50320`.

- To prevent broadcast storms, each peer maintains a HashMap of queries,
    mapping each query's ID to that query.
    If a peer happens to forward a query with the same ID,
    the receiving peer considers this to be the same query as in its map, so it simply discards the query it received.
- To prevent duplicate responses, the host removes the matching query once it receives the first response.
    Thus, when it gets a duplicate response, it has no query to match against, so it simply discards that response.
