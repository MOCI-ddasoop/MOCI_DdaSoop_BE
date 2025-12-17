package com.back.domain.feed.repository;

import com.back.domain.feed.entity.FeedImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {


}
