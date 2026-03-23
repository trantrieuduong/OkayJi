package com.okayji.chat.repository;

import com.okayji.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MessageRepository extends JpaRepository<Message,Long> {
    Page<Message> findByChatId(String chatId, Pageable pageable);
    @Query("""
        select m
        from Message m
        where m.chat.id = :chatId
        order by m.seq desc
    """)
    Slice<Message> findMessagesFirstPage(String chatId, Pageable pageable);

    @Query("""
        select m
        from Message m
        where m.chat.id = :chatId
        and m.seq < :cursorSeq
        order by m.seq desc
    """)
    Slice<Message> findMessagesAfterCursor(String chatId,
                                           Long cursorSeq,
                                           Pageable pageable);
}
