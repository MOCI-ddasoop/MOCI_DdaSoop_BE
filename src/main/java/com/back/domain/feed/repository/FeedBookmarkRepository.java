package com.back.domain.feed.repository;

import com.back.domain.feed.entity.FeedBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedBookmarkRepository extends JpaRepository<FeedBookmark, Long> {

}
