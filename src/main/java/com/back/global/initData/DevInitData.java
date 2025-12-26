package com.back.global.initData;

import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.entity.CommentReaction;
import com.back.domain.comment.entity.CommentType;
import com.back.domain.comment.repository.CommentReactionRepository;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.feed.entity.*;
import com.back.domain.feed.repository.FeedBookmarkRepository;
import com.back.domain.feed.repository.FeedReactionRepository;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.MemberRole;
import com.back.domain.member.entity.SocialProvider;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 개발 환경 초기 데이터 설정 (JPA 테이블 생성 후 실행) */
@Slf4j
@Configuration
@Profile("default")
@RequiredArgsConstructor
public class DevInitData {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final FeedRepository feedRepository;
    private final FeedReactionRepository feedReactionRepository;
    private final FeedBookmarkRepository feedBookmarkRepository;
    private final CommentRepository commentRepository;
    private final CommentReactionRepository commentReactionRepository;

    private final Random random = new Random();

    /** 애플리케이션 준비 완료 후 샘플 데이터 생성 */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initDataOnApplicationReady() {
        // Member가 이미 존재하면 전체 초기화 스킵
        if (memberRepository.count() > 0) {
            log.info("데이터가 이미 존재합니다. 초기 데이터 생성을 건너뜁니다.");
            return;
        }

        log.info("========== 초기 데이터 생성 시작 ==========");

        // 1. Member 생성
        List<Member> members = initMembers();
        log.info(" Member {} 개 생성 완료", members.size());

        // 2. Feed 생성 (40개)
        List<Feed> feeds = initFeeds(members);
        log.info(" Feed {} 개 생성 완료", feeds.size());

        // 3. FeedReaction 생성
        int reactionCount = initFeedReactions(feeds, members);
        log.info(" FeedReaction {} 개 생성 완료", reactionCount);

        // 4. FeedBookmark 생성
        int bookmarkCount = initFeedBookmarks(feeds, members);
        log.info(" FeedBookmark {} 개 생성 완료", bookmarkCount);

        // 5. Comment 생성
        List<Comment> comments = initComments(feeds, members);
        log.info(" Comment {} 개 생성 완료", comments.size());

        // 6. CommentReaction 생성
        int commentReactionCount = initCommentReactions(comments, members);
        log.info(" CommentReaction {} 개 생성 완료", commentReactionCount);

        log.info("========== 초기 데이터 생성 완료 ==========");
        log.info("총 생성: Member {}, Feed {}, Comment {}", members.size(), feeds.size(), comments.size());
    }

    // ========== 1. Member 샘플 데이터 생성 ==========

    /** Member 샘플 데이터 생성 */
    public List<Member> initMembers() {
        List<Member> members = new ArrayList<>();

        log.info("Member 샘플 데이터 생성 시작...");

        // 일반 사용자 1
        Member member1 = Member.builder()
                .name("홍길동")
                .nickname("hong123")
                .email("hong@example.com")
                .memberCode(memberService.generateMemberCode())
                .profileImageUrl("https://picsum.photos/seed/hong/200/200")
                .role(MemberRole.USER)
                .lastLoginProvider(SocialProvider.GOOGLE)
                .build();
        members.add(memberRepository.save(member1));
        log.info("Member 생성: {} (이메일: {})", member1.getNickname(), member1.getEmail());

        // 일반 사용자 2
        Member member2 = Member.builder()
                .name("김철수")
                .nickname("kim456")
                .email("kim@example.com")
                .memberCode(memberService.generateMemberCode())
                .profileImageUrl("https://picsum.photos/seed/kim/200/200")
                .role(MemberRole.USER)
                .lastLoginProvider(SocialProvider.KAKAO)
                .build();
        members.add(memberRepository.save(member2));
        log.info("Member 생성: {} (이메일: {})", member2.getNickname(), member2.getEmail());

        // 일반 사용자 3
        Member member3 = Member.builder()
                .name("이영희")
                .nickname("lee789")
                .email("lee@example.com")
                .memberCode(memberService.generateMemberCode())
                .profileImageUrl("https://picsum.photos/seed/lee/200/200")
                .role(MemberRole.USER)
                .lastLoginProvider(SocialProvider.NAVER)
                .build();
        members.add(memberRepository.save(member3));
        log.info("Member 생성: {} (이메일: {})", member3.getNickname(), member3.getEmail());

        // 관리자
        Member admin = Member.builder()
                .name("관리자")
                .nickname("admin")
                .email("admin@example.com")
                .memberCode(memberService.generateMemberCode())
                .profileImageUrl("https://picsum.photos/seed/admin/200/200")
                .role(MemberRole.ADMIN)
                .lastLoginProvider(SocialProvider.GOOGLE)
                .build();
        members.add(memberRepository.save(admin));
        log.info("Member 생성: {} (이메일: {}, 역할: {})", admin.getNickname(), admin.getEmail(), admin.getRole());

        return members;
    }

    // ========== 2. Feed 샘플 데이터 생성 ==========

    private List<Feed> initFeeds(List<Member> members) {
        List<Feed> feeds = new ArrayList<>();

        String[][] contentTemplates = {
                {"오늘 아침 운동 완료! 💪", "여행", "운동", "일상"},
                {"맛있는 점심 식사 😋", "맛집", "음식", "일상"},
                {"새로운 프로젝트 시작!", "개발", "일", "성장"},
                {"주말 나들이 다녀왔어요 🌳", "여행", "휴식", "일상"},
                {"책 읽기 챌린지 5일차 📚", "독서", "자기계발", "성장"},
                {"강아지랑 산책 🐕", "반려동물", "일상", "힐링"},
                {"집에서 요리하기 🍳", "요리", "일상", "맛집"},
                {"넷플릭스 추천작 있나요?", "영화", "드라마", "추천"},
                {"오늘의 운동 루틴 공유", "운동", "헬스", "다이어트"},
                {"새로 산 카메라 테스트 📷", "사진", "취미", "일상"}
        };

        for (int i = 0; i < 40; i++) {
            int templateIndex = i % contentTemplates.length;
            String[] template = contentTemplates[templateIndex];

            String content = template[0] + "\n\n" + generateRandomContent();
            List<String> tags = List.of(template[1], template[2], template[3]);

            Member author = members.get(random.nextInt(members.size()));
            FeedType feedType = random.nextBoolean() ? FeedType.GENERAL : FeedType.TOGETHER_VERIFICATION;
            FeedVisibility visibility = random.nextInt(10) > 2 ? FeedVisibility.PUBLIC : FeedVisibility.FOLLOWERS;

            Feed feed = Feed.builder()
                    .feedType(feedType)
                    .content(content)
                    .visibility(visibility)
                    .tags(tags)
                    .member(author)
                    .images(new ArrayList<>())
                    .reactionCount(0)
                    .commentCount(0)
                    .bookmarkCount(0)
                    .build();

            // 이미지 추가 (50% 확률로 1-3개)
            if (random.nextBoolean()) {
                int imageCount = random.nextInt(3) + 1;
                for (int j = 0; j < imageCount; j++) {
                    FeedImage image = FeedImage.builder()
                            .feed(feed)
                            .imageUrl(String.format("https://picsum.photos/seed/feed%d-%d/800/600", i, j))
                            .width(800)
                            .height(600)
                            .displayOrder(j)
                            .fileSize((long) (random.nextInt(500) + 100) * 1024)
                            .originalFileName(String.format("image_%d_%d.jpg", i, j))
                            .build();
                    feed.addImage(image);
                }
            }

            feeds.add(feedRepository.save(feed));
        }

        return feeds;
    }

    // ========== 3. FeedReaction 생성 ==========

    private int initFeedReactions(List<Feed> feeds, List<Member> members) {
        int count = 0;

        for (Feed feed : feeds) {
            // 각 피드마다 랜덤으로 0-4명이 좋아요
            int reactionCount = random.nextInt(5);
            List<Member> shuffledMembers = new ArrayList<>(members);
            java.util.Collections.shuffle(shuffledMembers);

            for (int i = 0; i < reactionCount && i < shuffledMembers.size(); i++) {
                Member member = shuffledMembers.get(i);

                // 자기 자신 좋아요 방지
                if (member.getId().equals(feed.getMember().getId())) {
                    continue;
                }

                FeedReaction reaction = FeedReaction.builder()
                        .feed(feed)
                        .member(member)
                        .build();
                feedReactionRepository.save(reaction);
                feed.incrementReactionCount();
                count++;
            }
        }

        return count;
    }

    // ========== 4. FeedBookmark 생성 ==========

    private int initFeedBookmarks(List<Feed> feeds, List<Member> members) {
        int count = 0;

        for (Feed feed : feeds) {
            // 각 피드마다 랜덤으로 0-3명이 북마크
            int bookmarkCount = random.nextInt(4);
            List<Member> shuffledMembers = new ArrayList<>(members);
            java.util.Collections.shuffle(shuffledMembers);

            for (int i = 0; i < bookmarkCount && i < shuffledMembers.size(); i++) {
                Member member = shuffledMembers.get(i);

                FeedBookmark bookmark = FeedBookmark.builder()
                        .feed(feed)
                        .member(member)
                        .build();
                feedBookmarkRepository.save(bookmark);
                feed.incrementBookmarkCount();
                count++;
            }
        }

        return count;
    }

    // ========== 5. Comment 생성 ==========

    private List<Comment> initComments(List<Feed> feeds, List<Member> members) {
        List<Comment> comments = new ArrayList<>();

        String[] commentContents = {
                "정말 멋지네요! 👍",
                "저도 해보고 싶어요!",
                "공감합니다 ㅎㅎ",
                "어디서 구매하셨나요?",
                "좋은 정보 감사합니다!",
                "대단하세요! 응원합니다 💪",
                "저도 비슷한 경험이 있어요",
                "다음에 같이 가요!",
                "사진이 정말 예쁘네요 📷",
                "꿀팁 감사합니다!"
        };

        // 각 피드마다 0-3개의 최상위 댓글
        for (Feed feed : feeds) {
            int topLevelCount = random.nextInt(4);

            for (int i = 0; i < topLevelCount; i++) {
                Member commenter = members.get(random.nextInt(members.size()));
                String content = commentContents[random.nextInt(commentContents.length)];

                Comment comment = Comment.builder()
                        .commentType(CommentType.FEED)
                        .content(content)
                        .member(commenter)
                        .feed(feed)
                        .parent(null)
                        .reactionCount(0)
                        .build();

                Comment savedComment = commentRepository.save(comment);
                comments.add(savedComment);

                // Feed 댓글 개수 증가
                savedComment.notifyFeedCommentCreated();

                // 30% 확률로 대댓글 1-2개 추가
                if (random.nextInt(10) < 3) {
                    int replyCount = random.nextInt(2) + 1;
                    for (int j = 0; j < replyCount; j++) {
                        Member replier = members.get(random.nextInt(members.size()));
                        String replyContent = "좋은 의견이네요! 저도 동의합니다.";

                        Comment reply = Comment.builder()
                                .commentType(CommentType.FEED)
                                .content(replyContent)
                                .member(replier)
                                .feed(feed)
                                .parent(savedComment)
                                .reactionCount(0)
                                .build();

                        Comment savedReply = commentRepository.save(reply);
                        comments.add(savedReply);
                        savedReply.notifyFeedCommentCreated();
                    }
                }
            }
        }

        return comments;
    }

    // ========== 6. CommentReaction 생성 ==========

    private int initCommentReactions(List<Comment> comments, List<Member> members) {
        int count = 0;

        for (Comment comment : comments) {
            // 각 댓글마다 랜덤으로 0-3명이 좋아요
            int reactionCount = random.nextInt(4);
            List<Member> shuffledMembers = new ArrayList<>(members);
            java.util.Collections.shuffle(shuffledMembers);

            for (int i = 0; i < reactionCount && i < shuffledMembers.size(); i++) {
                Member member = shuffledMembers.get(i);

                CommentReaction reaction = CommentReaction.builder()
                        .comment(comment)
                        .member(member)
                        .build();
                commentReactionRepository.save(reaction);
                comment.incrementReactionCount();
                count++;
            }
        }

        return count;
    }

    // ========== 헬퍼 메서드 ==========

    private String generateRandomContent() {
        String[] sentences = {
                "정말 좋은 하루였습니다.",
                "다들 한번 시도해보세요!",
                "생각보다 쉬웠어요.",
                "앞으로도 계속 이어갈 예정입니다.",
                "여러분의 의견도 궁금해요!",
                "다음엔 더 잘할 수 있을 것 같아요.",
                "많은 분들이 즐기셨으면 좋겠습니다.",
                "새로운 도전을 시작했습니다!"
        };

        StringBuilder content = new StringBuilder();
        int sentenceCount = random.nextInt(3) + 2;  // 2-4문장

        for (int i = 0; i < sentenceCount; i++) {
            content.append(sentences[random.nextInt(sentences.length)]);
            if (i < sentenceCount - 1) {
                content.append(" ");
            }
        }

        return content.toString();
    }
}
