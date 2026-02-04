package com.okayji.feed.repository;

import com.okayji.feed.entity.Friend;
import com.okayji.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRepository extends JpaRepository<Friend,String> {
    boolean existsByUserLow_IdAndUserHigh_Id(String userLowId, String userHighId);

    Friend findByUserLowAndUserHigh(User userLow, User userHigh);
}
