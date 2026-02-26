package com.back.domain.donation.service;

import com.back.domain.notification.entity.NotificationTargetType;
import com.back.domain.notification.entity.NotificationType;
import com.back.domain.notification.service.NotificationService;
import com.back.domain.donation.client.TossPaymentsClient;
import com.back.domain.donation.dto.*;
import com.back.domain.donation.entity.*;
import com.back.domain.donation.repository.DonationNoticeRepository;
import com.back.domain.donation.repository.DonationPaymentsRepository;
import com.back.domain.donation.repository.DonationRepository;
import com.back.domain.donation.repository.TossPaymentRepository;
import com.back.domain.member.entity.Member;
import com.back.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DonationService {
    private final DonationRepository donationRepository;
    private final DonationNoticeRepository donationNoticeRepository;
    private final DonationPaymentsRepository donationPaymentsRepository;
    private final TossPaymentRepository tossPaymentRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    
    public DonationDto.PageResponse<DonationDto.ListResponse> getAllDonations(
            List<DonationCategory> categories,
            DonationSortType sortType,
            Pageable pageable
    ) {
        final int page = pageable.getPageNumber();
        final int size = pageable.getPageSize();
        final int firstPageRealSize = Math.max(0, size - 1); // 1페이지는 11개

        boolean hasCategory = categories != null && !categories.isEmpty();

        // 우리가 원하는 slice 범위
        final int start = (page == 0) ? 0 : (firstPageRealSize + (page - 1) * size);
        final int limit = (page == 0) ? firstPageRealSize : size;
        final int endExclusive = start + limit;

        PageRequest fetchPageable = PageRequest.of(0, Math.max(endExclusive, firstPageRealSize), pageable.getSort());

        Page<Donations> fetched;
        if (!hasCategory) {
            fetched = switch (sortType) {
                case POPULAR -> donationRepository.findPopularWithoutCategory(fetchPageable);
                case DEADLINE -> donationRepository.findDeadlineWithoutCategory(fetchPageable);
                default -> donationRepository.findLatestWithoutCategory(fetchPageable);
            };
        } else {
            fetched = switch (sortType) {
                case POPULAR -> donationRepository.findPopularWithCategory(categories, fetchPageable);
                case DEADLINE -> donationRepository.findDeadlineWithCategory(categories, fetchPageable);
                default -> donationRepository.findLatestWithCategory(categories, fetchPageable);
            };
        }

        long totalElements = fetched.getTotalElements();
        int totalPages = calcTotalPagesUi(totalElements, size);

        List<Donations> all = fetched.getContent();
        int safeFrom = Math.min(start, all.size());
        int safeTo = Math.min(endExclusive, all.size());

        List<DonationDto.ListResponse> content = all.subList(safeFrom, safeTo)
                .stream()
                .map(DonationDto.ListResponse::from)
                .toList();

        return new DonationDto.PageResponse<>(content, page, size, totalElements, totalPages);
    }

    private int calcTotalPagesUi(long total, int size) {
        int firstPageRealSize = Math.max(0, size - 1);
        if (total <= firstPageRealSize) return 1;
        long remain = total - firstPageRealSize;
        long pagesAfter = (remain + size - 1) / size; // ceil
        return (int) (1 + pagesAfter);
    }

    public DonationDto.DetailResponse getDonation(Long id) {
        var donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "번 후원하기 없음"));
        return DonationDto.DetailResponse.from(donation);
    }

    public DonationDto.DescriptionResponse getDonationDescription(Long id) {
        Donations donations = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(id + "번 후원하기 설명 없음"));
        String description = donations.getDescription();
        return new DonationDto.DescriptionResponse(
                description == null ? "" : description
        );
    }

    public Boolean isDonationCreator(Long donationId, Long memberId) {
        Donations donations = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException(donationId + "번 후원하기 없음"));
        return donations.getMember().getId().equals(memberId);
    }

    public List<DonationDto.MyDonationListResponse> getMyDonationList(Long memberId) {
        List<Donations> donations = donationRepository.findByMember_Id(memberId);

        return donations.stream().map(DonationDto.MyDonationListResponse::from).toList();
    }

    public List<DonationPaymentDto.DonationPaymentListResponse> getDonationPaymentList(Long memberId){
        List<DonationPayments> paymentList = donationPaymentsRepository.findAllMyDonationPaymentByMember_Id(memberId);

        return paymentList.stream().map(DonationPaymentDto.DonationPaymentListResponse::from).toList();
    }

    //최근 후원 내역 2개 조회
    public List<DonationPaymentDto.RecentDonationPaymentListResponse> getRecentDonationPayments(){
        List<DonationPayments> paymentList = donationPaymentsRepository.findTop2ByOrderByCreatedAtDesc();

        return paymentList.stream().map(DonationPaymentDto.RecentDonationPaymentListResponse::from).toList();
    }

    public List<DonationNoticeDto.ListResponse> getAllDonationNotices() {
        List<DonationNotice> notices = donationNoticeRepository.findAll();

        return notices.stream().map(DonationNoticeDto.ListResponse::from).toList();
    }

    public DonationNoticeDto.ListResponse getDonationNoticesByDonationId(Long donationId) {

        return donationNoticeRepository.findByDonations_Id(donationId)
                .map(DonationNoticeDto.ListResponse::from)
                .orElse(new DonationNoticeDto.ListResponse(
                        null,"","","","",donationId));
    }

    public List<DonorDto.ListResponse> getAllDonorList(Long id){
        List<DonationPayments> payments = donationPaymentsRepository.findAllDonorList(id);

        return payments.stream().map(DonorDto.ListResponse::from).toList();
    }

    // 후원하기 게시글 등록
    @Transactional
    public DonationDto.CreateResponse createDonation(
            DonationDto.CreateRequest request, Long memberId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원(memberId)을 찾을 수 없습니다."));

        String thumbnail = null;

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            thumbnail = request.imageUrls().getFirst();
        }

        Donations donation = Donations.builder()
                .title(request.title())
                .description(request.description())
                .goalAmount(request.goalAmount())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .imageUrls(request.imageUrls())
                .status(request.status())
                .donationCategory(request.category())
                .member(member)
                .build();

        donationRepository.save(donation);

        return DonationDto.CreateResponse.from(donation);
    }

    // 후원하기 공지 게시글 등록
    @Transactional
    public DonationNoticeDto.CreateResponse createDonationNotice(
            DonationNoticeDto.CreateRequest request, Long donationId, Long memberId
    ) {
        Donations donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("후원페이지(donationId)를 찾을 수 없습니다."));

        DonationNotice notice = DonationNotice.builder()
                .title(request.title())
                .description(request.description())
                .progressNews(request.progressNews())
                .reviews(request.reviews())
                .donations(donation)
                .build();

        donationNoticeRepository.save(notice);

        // 후원 참여자 전체에게 공지 알림 → "후원 공지가 등록되었습니다"
        List<DonationPayments> participants = donationPaymentsRepository.findAllDonorList(donationId);
        for (DonationPayments participant : participants) {
            notificationService.createNotification(
                    participant.getMember().getId(),
                    memberId,
                    NotificationType.DONATION_NOTICE,
                    NotificationTargetType.DONATION,
                    donationId
            );
        }

        return DonationNoticeDto.CreateResponse.from(notice);
    }

    //Toss 결제 승인 및 후원 결제 내역 저장
    @Transactional
    public DonationPaymentDto.DonationPaymentResponse donationTossPayment(
            Long donationId, Long memberId, DonationTossDto.DonationTossRequest request
    ) {
        // 후원 대상 조회
        Donations donation = donationRepository.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("후원페이지(donationId)를 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원(memberId)을 찾을 수 없습니다."));

        // 토스 결제 승인
        DonationTossDto.DonationTossResponse tossResponse =
                tossPaymentsClient.confirm(request);

        TossPayments tossPayments = TossPayments.builder()
                .paymentKey(tossResponse.paymentKey())
                .orderId(tossResponse.orderId())
                .amount(tossResponse.totalAmount())
                .status(TossPaymentStatus.DONE)
                .approvedAt(tossResponse.approvedAt())
                .member(member)
                .build();

        tossPaymentRepository.save(tossPayments);

        // 결제 내역 저장
        DonationPayments payment = DonationPayments.builder()
                .donations(donation)
                .member(member)
                .amount(tossResponse.totalAmount())
                .paymentMethod("TOSS")
//                .approvedAt(tossResponse.getApprovedAt()) //TODO: 일단 최소한으로 조건 사용
//                .tossPayments(tossPayments)
                .build();

        donationPaymentsRepository.save(payment);

        if (TossPaymentStatus.DONE.name().equals(tossResponse.status())) {
            donation.increaseAmount(tossResponse.totalAmount());

            // 후원 개설자에게 알림 → "님이 후원해주셨습니다" (본인이 개설자이면 NotificationService에서 자동 차단)
            notificationService.createNotification(
                    donation.getMember().getId(),
                    memberId,
                    NotificationType.DONATION_RECEIVED,
                    NotificationTargetType.DONATION,
                    donationId
            );

            // 후원자 본인에게 알림 → "후원이 완료되었습니다"
            notificationService.createNotification(
                    memberId,
                    null,
                    NotificationType.DONATION_COMPLETE,
                    NotificationTargetType.DONATION,
                    donationId
            );
        }

        // 프론트로 응답
        return DonationPaymentDto.DonationPaymentResponse.from(payment);
    }


}
