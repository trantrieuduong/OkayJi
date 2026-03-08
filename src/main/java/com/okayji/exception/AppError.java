package com.okayji.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
@Getter
public enum AppError {
    UNCATEGORIZED_EXCEPTION("Uncategorized Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT_DATA("Invalid input data", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED("Incorrect or expired token", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("You do not have access", HttpStatus.UNAUTHORIZED),

    USERNAME_EXISTED("Username already exists", HttpStatus.BAD_REQUEST),
    CHANGE_USERNAME_LIMIT("You can only change your username every 30 days", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED("Email already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    POST_NOT_FOUND("Post not found", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND("Comment not found", HttpStatus.NOT_FOUND),

    WRONG_PASSWORD("Wrong password", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH("Password does not match", HttpStatus.BAD_REQUEST),

    FRIEND_YOURSELF("Cannot friend yourself", HttpStatus.BAD_REQUEST),
    FRIEND_ALREADY("You are friend already", HttpStatus.BAD_REQUEST),
    NOT_FRIEND("You are not friends", HttpStatus.BAD_REQUEST),
    FRIEND_REQUEST_NOT_FOUND("Friend request not found", HttpStatus.NOT_FOUND),
    FRIEND_REQUEST_EXISTS("Friend request already exists", HttpStatus.BAD_REQUEST),

    CHAT_NOT_FOUND("Chat not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_IN_CHAT("User is already in chat", HttpStatus.BAD_REQUEST),

    MAX_FILE_SIZE("File size exceeds limit", HttpStatus.BAD_REQUEST),
    FILE_NOT_ALLOW("File type not allow", HttpStatus.BAD_REQUEST),

    NOTI_NOT_FOUND("Notification not found", HttpStatus.NOT_FOUND),
    ;

    private final String message;
    private final HttpStatusCode httpStatusCode;
}
