package com.blogplatform.service;

import com.blogplatform.dto.PagedResponse;
import com.blogplatform.dto.UserDto;
import com.blogplatform.exception.*;
import com.blogplatform.model.User;
import com.blogplatform.repository.PostRepository;
import com.blogplatform.repository.UserRepository;
import com.blogplatform.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final SecurityUtil securityUtil;

    public PagedResponse<UserDto.Profile> getAllUsers(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = (search != null && !search.isBlank())
                ? userRepository.searchUsers(search, pageable)
                : userRepository.findAll(pageable);
        return PagedResponse.of(users.map(this::mapToProfile));
    }

    public UserDto.Profile getUserById(Long id) {
        User user = findById(id);
        return mapToProfile(user);
    }

    public UserDto.Profile getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToProfile(user);
    }

    @Transactional
    public UserDto.Profile updateProfile(Long id, UserDto.UpdateRequest request) {
        User current = securityUtil.getCurrentUserOrThrow();
        if (!securityUtil.isOwnerOrAdmin(id)) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        User user = findById(id);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getBio()       != null) user.setBio(request.getBio());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public UserDto.Profile adminUpdateUser(Long id, UserDto.AdminUpdateRequest request) {
        User user = findById(id);
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()  != null) user.setLastName(request.getLastName());
        if (request.getBio()       != null) user.setBio(request.getBio());
        if (request.getRole()      != null) user.setRole(request.getRole());
        if (request.getEnabled()   != null) user.setEnabled(request.getEnabled());
        if (request.getLocked()    != null) user.setLocked(request.getLocked());
        return mapToProfile(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!securityUtil.isOwnerOrAdmin(id)) {
            throw new AccessDeniedException("You can only delete your own account");
        }
        userRepository.delete(findById(id));
        log.info("User deleted: id={}", id);
    }

    @Transactional
    public void toggleUserEnabled(Long id, boolean enabled) {
        User user = findById(id);
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("User id={} enabled={}", id, enabled);
    }

    @Transactional
    public void toggleUserLocked(Long id, boolean locked) {
        User user = findById(id);
        user.setLocked(locked);
        userRepository.save(user);
        log.info("User id={} locked={}", id, locked);
    }

    // ---- Mapping ----

    public UserDto.Profile mapToProfile(User user) {
        return UserDto.Profile.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .postCount(postRepository.countByAuthor(user))
                .build();
    }

    public UserDto.Summary mapToSummary(User user) {
        return UserDto.Summary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
