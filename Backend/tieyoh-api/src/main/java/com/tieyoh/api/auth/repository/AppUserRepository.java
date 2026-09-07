package com.tieyoh.api.auth.repository;

import com.tieyoh.api.auth.entity.AppUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<AppUser> findByTieyohIdIgnoreCase(String tieyohId);

    boolean existsByTieyohIdIgnoreCase(String tieyohId);

    // Backs the "invite" search bar. The @t handle match is a STARTS_WITH
    // against the part of the id after the fixed "@t-" label -- the label
    // is implicit/default, so typing "test" alone matches "@t-Test", the
    // same as typing "@t-test" or "t-test" would (the service strips any
    // of those prefixes off before calling this, passing the bare body as
    // handleQuery). Phone/email stay a plain CONTAINS on the raw query.
    // Excludes the searching user themselves and is capped via the
    // Pageable (no COUNT query needed since the client only ever shows a
    // short top-N list, not full pagination).
    @Query("SELECT u FROM AppUser u WHERE u.id <> :excludeId AND u.status = 'ACTIVE' AND (" +
            "LOWER(u.tieyohId) LIKE LOWER(CONCAT('@t-', :handleQuery, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :rawQuery, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :rawQuery, '%')))")
    List<AppUser> searchByHandlePhoneOrEmail(
            @Param("handleQuery") String handleQuery,
            @Param("rawQuery") String rawQuery,
            @Param("excludeId") Long excludeId,
            Pageable pageable);
}
