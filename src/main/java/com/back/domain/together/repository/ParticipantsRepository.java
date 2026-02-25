package com.back.domain.together.repository;

import com.back.domain.together.entity.Participants;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantsRepository extends JpaRepository<Participants, Long> {

    Optional<Participants> findByTogetherIdAndMemberId(Long togetherId, Long memberId);

    boolean existsByTogetherIdAndMemberIdAndParticipantsStatus(
            Long togetherId, Long memberId, com.back.domain.together.entity.ParticipantsStatus status);
}
