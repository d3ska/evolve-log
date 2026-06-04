package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.dto.request.UpdateUserPreferencesRequest;
import com.deska.evolvelog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User updatePreferences(User user, UpdateUserPreferencesRequest request) {
        user.updatePreferences(request.unitSystem());
        return userRepository.save(user);
    }

    @Transactional
    public User updateLocale(User user, String locale) {
        user.updateLocale(locale);
        return userRepository.save(user);
    }
}
