package amgn.amu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import amgn.amu.entity.Block;

public interface BlockRepository extends JpaRepository<Block, Block.PK> {
    boolean existsByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);
    void deleteByIdBlockerIdAndIdBlockedId(Long blockerId, Long blockedId);
}

