package com.back.global.initData;

import com.back.domain.comment.entity.Comment;
import com.back.domain.comment.entity.CommentReaction;
import com.back.domain.comment.entity.CommentType;
import com.back.domain.comment.repository.CommentReactionRepository;
import com.back.domain.comment.repository.CommentRepository;
import com.back.domain.donation.dto.DonationNoticeDto;
import com.back.domain.donation.entity.DonationCategory;
import com.back.domain.donation.entity.DonationNotice;
import com.back.domain.donation.entity.Donations;
import com.back.domain.donation.repository.DonationNoticeRepository;
import com.back.domain.donation.repository.DonationRepository;
import com.back.domain.donation.service.DonationService;
import com.back.domain.feed.entity.*;
import com.back.domain.feed.repository.FeedBookmarkRepository;
import com.back.domain.feed.repository.FeedReactionRepository;
import com.back.domain.feed.repository.FeedRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.entity.MemberRole;
import com.back.domain.member.entity.SocialProvider;
import com.back.domain.member.repository.MemberRepository;
import com.back.domain.member.service.MemberService;
import com.back.domain.together.entity.*;
import com.back.domain.together.repository.ParticipantsRepository;
import com.back.domain.together.repository.TogetherRepository;
import com.back.domain.together.service.TogetherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final TogetherRepository togetherRepository;
    private final TogetherService togetherService;
    private final DonationRepository donationRepository;
    private final DonationNoticeRepository donationNoticeRepository;
    private final DonationService donationService;
    private final ParticipantsRepository participantsRepository;

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

        // 20. Together 생성
        List<Together> togethers = initTogethers(members);
        log.info(" Together {} 개 생성 완료", togethers.size());

        // 21. Participants 생성
        List<Participants> participantsList = initParticipants(togethers, members);
        log.info(" Participants {} 개 생성 완료", participantsList.size());

        // 22. Donation 생성
        List<Donations> donationsList = initDonations(members);
        log.info(" Donation {} 개 생성 완료", donationsList.size());

        // 23. DonationNotice 생성
        List<DonationNotice> donationNotices = initDonationNotices(donationsList);
        log.info(" DonationNotice {} 개 생성 완료", donationNotices.size());

        log.info("========== 초기 데이터 생성 완료 ==========");
        log.info("총 생성: Member {}, Feed {}, Comment {}, Together {}, Participants {}, Donation {}, DonationNotice {}",
                members.size(), feeds.size(), comments.size(),
                togethers.size(), participantsList.size(), donationsList.size(), donationNotices.size());
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


    // ========== 20. Together 샘플 데이터 생성 ==========
    private Together createTogether(
            String[] template,
            Member organizer,
            int startDayRange
    ) {
        TogetherMode mode = random.nextBoolean()
                ? TogetherMode.ONLINE
                : TogetherMode.OFFLINE;

        return Together.builder()
                .title(template[0])
                .description(template[1])
                .category(TogetherCategory.valueOf(template[2]))
                .mode(mode)
                .capacity((long) (random.nextInt(15) + 5)) // 5 ~ 20명
                .startDate(LocalDate.now().plusDays(random.nextInt(startDayRange) + 1))
                .endDate(LocalDate.now().plusDays(100)) // 100일 후 종료
                .togetherStatus(TogetherStatus.RECRUITING)
                .member(organizer)
                .build();
    }
    private List<Together> initTogethers(List<Member> members) {
        List<Together> togethers = new ArrayList<>();

        String[][] templates = {
                {"같이 플로깅해요", "주말 플로깅 참여자 모집", "PLOGGING"},
                {"동네 환경 정화", "공원 쓰레기 줍기", "CLEANUP"},
                {"분리수거 캠페인", "올바른 분리배출 실천", "RECYCLING"},
                {"아침 플로깅", "출근 전 30분 플로깅", "PLOGGING"},
                {"환경 보호 챌린지", "일주일간 친환경 실천", "CLEANUP"},
                {"제로웨이스트 실천", "플라스틱 줄이기", "RECYCLING"}
        };

        // 처음 4개는 1~4번 멤버가 각각 주최
        for (int i = 0; i < 4; i++){
            String[] template = templates[i%templates.length];
            togethers.add(togetherRepository.save(createTogether(template, members.get(i), 5)));
        }

        // 나머지 26개는 랜덤 멤버가 주최
        for (int i = 4; i < 30; i++) {
            String[] template = templates[i%templates.length];
            Member organizer = members.get(random.nextInt(members.size()));
            togethers.add(togetherRepository.save(createTogether(template, organizer, 10)));
        }

        return togethers;

    }

    // ========== 21. Participants 샘플 데이터 생성 ==========
    private List<Participants> initParticipants(List<Together> togethers, List<Member> members) {
        List<Participants> participantsList = new ArrayList<>();

        // Together 1~10만 사용
        togethers.stream().filter(together -> together.getId() !=null
                && together.getId() >= 1 && together.getId() <= 10).forEach(together -> {
                    // 참가하는 아이디는 1~5번만 사용
            List<Member> candidateMember = members.stream()
                    .filter(member -> member.getId() != null
                            && member.getId() >= 1 && member.getId() <= 4).toList();

            // 5명선택
            List<Member> shuffled = new ArrayList<>(candidateMember);
            java.util.Collections.shuffle(shuffled);

            int limit = Math.min(5, candidateMember.size());

            for (int i=0; i<limit; i++){
                Member member = shuffled.get(i);

                ParticipantRole role = together.getMember().getId().equals(member.getId())
                        ? ParticipantRole.LEADER
                        : ParticipantRole.MEMBER;

                Participants participants = Participants.builder()
                        .together(together)
                        .member(member)
                        .participantsStatus(ParticipantsStatus.PARTICIPATING)
                        .participantRole(role)
                        .build();

                participantsRepository.save(participants);
                participantsList.add(participants);
            }
        });

        return participantsList;
    }

    // ========== 22. Donation 샘플 데이터 생성 ==========
    private Donations createDonation(
            String[] template,
            Member organizer,
            int startDayRange
    ){
        return Donations.builder()
                .title(template[0])
                .description(template[1])
                .goalAmount(100000L)
                .currentAmount(0L)
                .startDate(LocalDate.now().plusDays(random.nextInt(startDayRange) + 1))
                .endDate(LocalDate.now().plusDays(random.nextInt(60, 121)))
                .status("ONGOING")
                .member(organizer)
                .donationCategory(DonationCategory.values()[random.nextInt(DonationCategory.values().length)])
                .build();
    }
    private List<Donations> initDonations(List<Member> members) {
        List<Donations> donations = new ArrayList<>();

        String[][] templates = {
                {"환경 보호를 위한 후원", "우리 지구를 지키기 위한 작은 실천에 동참해주세요!"},
                {"해양 생태계 보존", "바다의 소중한 생명들을 위해 후원해주세요!"},
                {"산림 복원 프로젝트", "숲을 다시 푸르게 만드는 일에 함께해요!"},
                {"멸종 위기 동물 보호", "소중한 동물들을 지키는 일에 동참해주세요!"},
                {"깨끗한 물 공급", "모든 이에게 깨끗한 물을 제공하기 위한 후원입니다."}
        };

        // 처음 5개는 1~5번 멤버가 각각 주최
        for (int i = 0; i < 4; i++){
            String[] template = templates[i%templates.length];
            donations.add(donationRepository.save(createDonation(template, members.get(i), 5)));
        }

        for (int i = 4; i < 30; i++){
            String[] template = templates[i%templates.length];
            Member organizer = members.get(random.nextInt(members.size()));
            donations.add(donationRepository.save(createDonation(template, organizer, 10)));
        }

        return donations;
    }

    // ========== 23. DonationNotice 샘플 데이터 생성 ==========
    private DonationNotice createDonationNotice(
            String[] template,
            Donations donations
    ) {
        return DonationNotice.builder()
                .title(template[0])
                .description(template[1])
                .progressNews(template[2])
                .reviews(template[3])
                .donations(donations)
                .build();
    }
    private List<DonationNotice> initDonationNotices(List<Donations> donationsList) {
        List<DonationNotice> notices = new ArrayList<>();

        String[][] templates = {
                {"첫 번째 소식", "후원해주셔서 감사합니다!", "현재 목표 금액의 30% 달성!", "후원자 여러분께 감사드립니다."},
                {"두 번째 소식", "더 많은 참여 부탁드려요!", "새로운 후원자가 늘고 있습니다.", "함께 해주셔서 감사합니다."},
                {"세 번째 소식", "목표 금액에 가까워지고 있어요!", "지금까지의 성과를 공유합니다.", "여러분의 후원이 큰 힘이 됩니다."}
        };

        for (int i = 0; i < 11; i++){
            String[] template = templates[i%templates.length];
            notices.add(donationNoticeRepository.save(createDonationNotice(template, donationsList.get(i))));
        }

        return notices;
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
