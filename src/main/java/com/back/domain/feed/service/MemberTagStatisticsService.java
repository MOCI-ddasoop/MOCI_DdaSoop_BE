package com.back.domain.feed.service;

import com.back.domain.feed.entity.MemberTagStatistics;
import com.back.domain.feed.repository.FeedReactionRepository;
import com.back.domain.feed.repository.MemberTagStatisticsRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MemberTagStatistics Service
 * 회원별 자주 사용하는 태그 통계 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberTagStatisticsService {

    private final MemberTagStatisticsRepository memberTagStatisticsRepository;
    private final FeedReactionRepository feedReactionRepository;
    private final MemberRepository memberRepository;
    @Autowired
    @Lazy
    private MemberTagStatisticsService self;

    // ========== 조회 ==========

    /**
     * 특정 회원의 자주 사용하는 태그 조회
     * 
     * @param memberId 회원 ID
     * @return 자주 사용하는 태그 목록 (최대 10개)
     */
    public List<String> getFrequentTags(Long memberId) {
        Optional<MemberTagStatistics> statsOptional = memberTagStatisticsRepository.findByMemberId(memberId);
        
        if (statsOptional.isEmpty()) {
            // 통계가 없으면 즉시 생성
            log.info("통계 없음 - 즉시 생성: 회원 ID {}", memberId);
            self.updateStatisticsSync(memberId);
            statsOptional = memberTagStatisticsRepository.findByMemberId(memberId);
        }
        
        return statsOptional
                .map(MemberTagStatistics::getFrequentTags)
                .orElse(List.of());
    }

    /**
     * 특정 회원의 태그 통계 조회
     * 
     * @param memberId 회원 ID
     * @return 태그 통계 (없으면 Optional.empty())
     */
    public Optional<MemberTagStatistics> getStatistics(Long memberId) {
        return memberTagStatisticsRepository.findByMemberId(memberId);
    }

    // ========== 통계 업데이트 (비동기) ==========

    /**
     * 특정 회원의 태그 통계를 비동기로 업데이트
     * 좋아요 추가/취소 시 호출됨
     * 
     * @param memberId 회원 ID
     */
    @Async
    @Transactional
    public void updateStatisticsAsync(Long memberId) {
        try {
            updateStatisticsSync(memberId);
        } catch (Exception e) {
            log.error("태그 통계 비동기 업데이트 실패 - 회원 ID: {}", memberId, e);
        }
    }

    /**
     * 특정 회원의 태그 통계를 동기적으로 업데이트
     * 
     * @param memberId 회원 ID
     */
    @Transactional
    public void updateStatisticsSync(Long memberId) {
        // 1. 현재 좋아요 개수 확인
        Long currentReactionCount = feedReactionRepository.countByMemberId(memberId);
        
        // 2. 기존 통계 조회
        Optional<MemberTagStatistics> statsOptional = memberTagStatisticsRepository.findByMemberId(memberId);
        
        // 3. 통계가 최신이면 스킵
        if (statsOptional.isPresent()) {
            MemberTagStatistics stats = statsOptional.get();
            if (stats.isUpToDate(currentReactionCount.intValue())) {
                log.debug("통계 최신 상태 - 재계산 스킵: 회원 ID {}", memberId);
                return;
            }
        }
        
        // 4. 자주 사용하는 태그 재계산
        List<String> frequentTags = feedReactionRepository.findFrequentTagsByMemberId(memberId);
        
        // 최대 10개로 제한
        if (frequentTags.size() > 10) {
            frequentTags = frequentTags.subList(0, 10);
        }
        
        // 5. 통계 업데이트 또는 생성
        if (statsOptional.isPresent()) {
            // 기존 통계 업데이트
            MemberTagStatistics stats = statsOptional.get();
            stats.updateStatistics(frequentTags, currentReactionCount.intValue());
            log.info("태그 통계 업데이트 완료 - 회원 ID: {}, 태그: {}", memberId, frequentTags);
        } else {
            // 새 통계 생성
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
            
            MemberTagStatistics newStats = MemberTagStatistics.builder()
                    .member(member)
                    .frequentTags(frequentTags)
                    .reactionCount(currentReactionCount.intValue())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            memberTagStatisticsRepository.save(newStats);
            log.info("태그 통계 생성 완료 - 회원 ID: {}, 태그: {}", memberId, frequentTags);
        }
    }

    // ========== 통계 삭제 ==========

    /**
     * 특정 회원의 태그 통계 삭제
     * 
     * @param memberId 회원 ID
     */
    @Transactional
    public void deleteStatistics(Long memberId) {
        memberTagStatisticsRepository.findByMemberId(memberId)
                .ifPresent(stats -> {
                    memberTagStatisticsRepository.delete(stats);
                    log.info("태그 통계 삭제 완료 - 회원 ID: {}", memberId);
                });
    }

    /**
     * 모든 회원의 태그 통계 초기화
     * 관리자용
     */
    @Transactional
    public void deleteAllStatistics() {
        memberTagStatisticsRepository.deleteAll();
        log.info("전체 태그 통계 삭제 완료");
    }

    // ========== 배치 작업 ==========

    /**
     * 오래된 통계 재계산 (7일 이상)
     * 스케줄러에서 호출
     * 
     * @return 재계산된 통계 개수
     */
    @Transactional
    public int refreshStaleStatistics() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<MemberTagStatistics> staleStats = memberTagStatisticsRepository.findStaleStatistics(sevenDaysAgo);
        
        int count = 0;
        for (MemberTagStatistics stats : staleStats) {
            try {
                updateStatisticsSync(stats.getMember().getId());
                count++;
            } catch (Exception e) {
                log.error("통계 재계산 실패 - 회원 ID: {}", stats.getMember().getId(), e);
            }
        }
        
        log.info("오래된 통계 재계산 완료 - {}개", count);
        return count;
    }

    /**
     * 통계가 없는 회원의 통계 생성
     * 초기 데이터 구축용
     * 
     * @return 생성된 통계 개수
     */
    @Transactional
    public int createMissingStatistics() {
        List<Long> memberIdsWithoutStats = memberTagStatisticsRepository.findMembersWithoutStatistics();
        
        int count = 0;
        for (Long memberId : memberIdsWithoutStats) {
            try {
                updateStatisticsSync(memberId);
                count++;
            } catch (Exception e) {
                log.error("통계 생성 실패 - 회원 ID: {}", memberId, e);
            }
        }
        
        log.info("누락된 통계 생성 완료 - {}개", count);
        return count;
    }
}
