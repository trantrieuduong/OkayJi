package com.okayji.feed.repository;

import com.okayji.feed.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;

public interface PostRepository extends JpaRepository<Post,String> {
    Page<Post> findByUser_Id(String userId, Pageable pageable);
    @Query("""
        select p
        from Post p
        where p.user.id in :authorIds
        order by p.createdAt desc, p.id desc
    """)
    Slice<Post> findFeedFirstPage(@Param("authorIds") Collection<String> authorIds, Pageable pageable);

    @Query("""
        select p
        from Post p
        where p.user.id in :authorIds
        and (p.createdAt < :cursorTime
        or (p.createdAt = :cursorTime and p.id < :cursorId))
        order by p.createdAt desc, p.id desc
    """)
    Slice<Post> findFeedAfterCursor(@Param("authorIds") Collection<String> authorIds,
                                    @Param("cursorTime") Instant cursorTime,
                                    @Param("cursorId") String cursorId,
                                    Pageable pageable);
}
