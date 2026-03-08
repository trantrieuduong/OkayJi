package com.okayji.identity.service.impl;

import com.okayji.identity.dto.request.UserChangePasswordRequest;
import com.okayji.identity.dto.request.UserChangeUsernameRequest;
import com.okayji.identity.dto.request.UserCreationRequest;
import com.okayji.identity.dto.response.UserResponse;
import com.okayji.identity.entity.Profile;
import com.okayji.identity.entity.Role;
import com.okayji.identity.entity.User;
import com.okayji.identity.entity.UserRole;
import com.okayji.exception.AppError;
import com.okayji.exception.AppException;
import com.okayji.mapper.UserMapper;
import com.okayji.identity.repository.RoleRepository;
import com.okayji.identity.repository.UserRepository;
import com.okayji.identity.service.UserService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;

@Service
@AllArgsConstructor
@Slf4j(topic = "USER-SERVICE")
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;

    @Override
    public UserResponse findById(String id) {
        return userMapper.toUserResponse(userRepository.findById(id)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND)));
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public UserResponse create(UserCreationRequest userCreationRequest) {
        log.info("User creation request: username={}", userCreationRequest.getUsername());

        User user = userMapper.toUser(userCreationRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(UserRole.USER).ifPresent(roles::add);
        user.setRoles(roles);

        Profile profile = Profile.builder()
                .user(user)
                .birthday(userCreationRequest.getBirthday())
                .fullName(userCreationRequest.getFullName())
                .gender(userCreationRequest.getGender())
                .build();
        user.setProfile(profile);

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public void changePassword(String userId, UserChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));
        log.info("User change password request: username={}", user.getUsername());

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword()))
            throw new AppException(AppError.WRONG_PASSWORD);

        if (!request.getNewPassword().equals(request.getNewPasswordConfirm()))
            throw new AppException(AppError.PASSWORD_NOT_MATCH);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(rollbackOn = Exception.class)
    public void changeUsername(String userId, UserChangeUsernameRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AppError.USER_NOT_FOUND));

        if (!user.canChangeUsername())
            throw new AppException(AppError.CHANGE_USERNAME_LIMIT);

        log.info("User change username request: oldUsername={}; newUsername={}",
                user.getUsername(), request.getNewUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new AppException(AppError.WRONG_PASSWORD);

        user.setUsername(request.getNewUsername());
        user.setLastChangeUsername(Instant.now());
        userRepository.save(user);
    }

    @Override
    public void delete(String userId) {

    }
}
