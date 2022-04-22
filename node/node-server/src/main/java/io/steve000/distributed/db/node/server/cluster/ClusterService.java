package io.steve000.distributed.db.node.server.cluster;

/**
 * I LIKE INTERFACES EVEN IF THEY ONLY HAVE ONE IMPLEMENTATION BECAUSE THEY ENFORCE A LAYER OF SEPARATION, OK?
 */
public interface ClusterService {

    Leader getLeader();

}
