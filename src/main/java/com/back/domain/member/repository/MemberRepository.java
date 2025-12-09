package com.back.domain.member.repository;

import com.back.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Member 엔티티를 위한 Repository 인터페이스
 * 
 * JpaRepository<Member, Long>를 상속받아 기본 CRUD 메서드를 자동으로 제공받습니다.
 * - Member: 엔티티 타입
 * - Long: ID 타입
 * 
 * 자동으로 제공되는 메서드들:
 * - save(Member member): 저장/수정
 * - findById(Long id): ID로 조회
 * - findAll(): 전체 조회
 * - delete(Member member): 삭제
 * - count(): 전체 개수
 * 등등...
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    // ========== 기본 조회 (Soft Delete 고려) ==========

    /**
     * ID로 활성 회원 조회
     * 
     * @param id 회원 ID
     * @return Optional<Member> (탈퇴한 회원이거나 없으면 empty)
     * 
     * 사용 예시:
     * Member member = memberRepository.findByIdAndDeletedAtIsNull(1L)
     *     .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
     */
    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 이메일로 활성 회원 조회
     * 
     * @param email 이메일
     * @return Optional<Member> (탈퇴한 회원이거나 없으면 empty)
     * 
     * 사용 예시 (로그인):
     * Member member = memberRepository.findByEmailAndDeletedAtIsNull("hong@example.com")
     *     .orElseThrow(() -> new IllegalArgumentException("이메일이 없습니다."));
     */
    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    /**
     * 닉네임으로 활성 회원 조회
     * 
     * @param nickname 닉네임
     * @return Optional<Member> (탈퇴한 회원이거나 없으면 empty)
     * 
     * 사용 예시 (닉네임 중복 체크, 프로필 조회):
     * boolean exists = memberRepository.findByNicknameAndDeletedAtIsNull("홍길동")
     *     .isPresent();  // true면 이미 사용 중인 닉네임
     */
    Optional<Member> findByNicknameAndDeletedAtIsNull(String nickname);

    // ========== 존재 여부 확인 (중복 체크용) ==========

    /**
     * 이메일 중복 체크 (활성 회원만)
     * 
     * @param email 이메일
     * @return true: 이미 사용 중인 이메일, false: 사용 가능한 이메일
     * 
     * 사용 예시 (회원가입 시):
     * if (memberRepository.existsByEmailAndDeletedAtIsNull("hong@example.com")) {
     *     throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
     * }
     */
    boolean existsByEmailAndDeletedAtIsNull(String email);

    /**
     * 닉네임 중복 체크 (활성 회원만)
     * 
     * @param nickname 닉네임
     * @return true: 이미 사용 중인 닉네임, false: 사용 가능한 닉네임
     * 
     * 사용 예시 (회원가입, 닉네임 변경 시):
     * if (memberRepository.existsByNicknameAndDeletedAtIsNull("홍길동")) {
     *     throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
     * }
     */
    boolean existsByNicknameAndDeletedAtIsNull(String nickname);

    // ========== 관리자용 메서드 (탈퇴 여부 무관) ==========

    /**
     * 이메일로 회원 조회 (탈퇴한 회원도 조회 가능)
     * 
     * 주의: 일반적인 로그인/조회에는 사용하지 말고, 관리자 기능에서만 사용하세요.
     * 일반 조회는 findByEmailAndDeletedAtIsNull()을 사용하세요.
     * 
     * @param email 이메일
     * @return Optional<Member> (탈퇴한 회원도 포함)
     * 
     * 사용 예시 (관리자가 탈퇴한 회원 정보도 확인):
     * Optional<Member> member = memberRepository.findByEmail("hong@example.com");
     */
    Optional<Member> findByEmail(String email);

    /**
     * 활성 회원 수 조회
     * 
     * @return 활성 회원 수 (deletedAt이 null인 회원 수)
     * 
     * 사용 예시 (대시보드 통계):
     * long activeMemberCount = memberRepository.countByDeletedAtIsNull();
     * System.out.println("현재 활성 회원 수: " + activeMemberCount);
     */
    long countByDeletedAtIsNull();
}

