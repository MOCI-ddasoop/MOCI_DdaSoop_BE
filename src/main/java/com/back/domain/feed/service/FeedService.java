package com.back.domain.feed.service;

import com.back.domain.feed.dto.FeedCreateRequest;
import com.back.domain.feed.entity.Feed;
import com.back.domain.feed.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final TagService tagService;


}
