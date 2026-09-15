package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.FriendConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendConnectionRepository extends JpaRepository<FriendConnection, Long> {

    List<FriendConnection> findByOwnerTieyohIdOrderByDisplayNameAsc(String ownerTieyohId);

    Optional<FriendConnection> findByOwnerTieyohIdAndContactUserId(String ownerTieyohId, Long contactUserId);

    // Used to auto-resolve a phone/email invite to ACTIVE immediately, when
    // the invitee turns out to already have a Tieyoh account.
    List<FriendConnection> findByStatusAndPhone(String status, String phone);

    List<FriendConnection> findByStatusAndEmailIgnoreCase(String status, String email);

    // Dedup lookups for addFriend: is this phone/email already a circle
    // entry for this owner (any status)? Used to update that row in place
    // instead of inserting a duplicate when the same contact is invited or
    // re-shared more than once.
    Optional<FriendConnection> findByOwnerTieyohIdAndPhone(String ownerTieyohId, String phone);

    Optional<FriendConnection> findByOwnerTieyohIdAndEmailIgnoreCase(String ownerTieyohId, String email);
}
