package amgn.amu.repository;

import amgn.amu.entity.UserAddress;
import amgn.amu.entity.UserAddress.AddressStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    @Query("select a from UserAddress a where a.userId=:userId and a.status=:status order by a.isDefault desc, a.updatedAt desc")
    List<UserAddress> findAllActive(@Param("userId") Long userId, @Param("status") AddressStatus status);

    Optional<UserAddress> findByAddressIdAndUserId(Long addressId, Long userId);

    @Modifying
    @Query("update UserAddress a set a.isDefault=false where a.userId=:userId and a.isDefault=true and a.addressId<>:keepId")
    int unsetDefaultForUserExcept(@Param("userId") Long userId, @Param("keepId") Long keepId);

    @Modifying
    @Query("update UserAddress a set a.isDefault=false where a.userId=:userId and a.isDefault=true")
    int unsetDefaultForUser(@Param("userId") Long userId);

    Optional<UserAddress> findByUserIdAndAddrKey(Long userId, String addrKey);
}

