package com.okayji.utils;

import com.okayji.identity.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PairUser {
    User low;
    User high;

    public static PairUser canonical(User a, User b) throws IllegalArgumentException {
        if (a.getId().equals(b.getId()))
            throw new IllegalArgumentException();
        return (a.getId().compareTo(b.getId()) < 0) ? new PairUser(a, b) : new PairUser(b, a);
    }
}
