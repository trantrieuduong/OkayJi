package com.okayji.feed.repository;

import com.okayji.feed.entity.FriendRequest;
import com.okayji.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest,String> {
    boolean existsBySender_IdAndReceiver_Id(String senderId, String receiverId);

    FriendRequest findBySenderAndReceiver(User sender, User receiver);
}
