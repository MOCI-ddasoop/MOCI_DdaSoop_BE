package com.back.domain.together.repository;

import com.back.domain.together.entity.Together;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TogetherRepository extends JpaRepository<Together,Long> {

    Optional<Together> findByMemberId(Long memberId);
}
