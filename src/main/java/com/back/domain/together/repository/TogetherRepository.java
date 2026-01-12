package com.back.domain.together.repository;

import com.back.domain.together.entity.Together;
import com.back.domain.together.entity.TogetherCategory;
import com.back.domain.together.entity.TogetherMode;
import com.back.domain.together.entity.TogetherStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TogetherRepository extends JpaRepository<Together,Long> {

    Optional<Together> findByMember_Id(Long memberId);

    Page<Together> findByCategory(TogetherCategory category, Pageable pageable);

    Page<Together> findByMode(TogetherMode mode, Pageable pageable);

    Page<Together> findByStatus(TogetherStatus status, Pageable pageable);

    Page<Together> findByCategoryAndMode(TogetherCategory category, TogetherMode mode, Pageable pageable);

    Page<Together> findByCategoryAndStatus(TogetherCategory category, TogetherStatus status, Pageable pageable);

    Page<Together> findByModeAndStatus(TogetherMode mode, TogetherStatus status, Pageable pageable);

    Page<Together> findByCategoryAndModeAndStatus(TogetherCategory category, TogetherMode mode, TogetherStatus status, Pageable pageable);
}
