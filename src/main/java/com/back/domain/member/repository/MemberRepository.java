package com.back.domain.member.repository;

import com.back.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Member 엔티티를 위한 Repository 인터페이스 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    // ========== 기본 조회 (Soft Delete 고려) ==========

    /** ID로 활성 회원 조회 (탈퇴한 회원 제외) */
    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    /** 이메일로 활성 회원 조회 (탈퇴한 회원 제외) */
    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    /** 닉네임으로 활성 회원 조회 (탈퇴한 회원 제외) */
    Optional<Member> findByNicknameAndDeletedAtIsNull(String nickname);

    /** 고유번호로 활성 회원 조회 (탈퇴한 회원 제외) */
    Optional<Member> findByMemberCodeAndDeletedAtIsNull(String memberCode);

    // ========== 존재 여부 확인 (중복 체크용) ==========

    /** 이메일 중복 체크 (활성 회원만) */
    boolean existsByEmailAndDeletedAtIsNull(String email);

    /** 닉네임 중복 체크 (활성 회원만) */
    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    /** 고유번호 중복 체크 (활성 회원만) */
    boolean existsByMemberCodeAndDeletedAtIsNull(String memberCode);

    // ========== 관리자용 메서드 (탈퇴 여부 무관) ==========

    /** 이메일로 회원 조회 (탈퇴한 회원도 포함) - 관리자용 */
    Optional<Member> findByEmail(String email);

    /** 활성 회원 수 조회 */
    long countByDeletedAtIsNull();
}

