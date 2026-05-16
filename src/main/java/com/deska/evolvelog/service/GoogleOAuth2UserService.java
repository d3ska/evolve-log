package com.deska.evolvelog.service;

import com.deska.evolvelog.domain.User;
import com.deska.evolvelog.repository.UserRepository;
import com.deska.evolvelog.security.CustomOidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class GoogleOAuth2UserService extends OidcUserService {

    private final UserRepository userRepository;

    private final Set<String> ALLOWED_USERS = Set.of(
            "mateuszdeska00@gmail.com",
            "natalia.walek32@gmail.com"
    );

    public GoogleOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        if (!ALLOWED_USERS.contains(oidcUser.getEmail())) {
            throw new OAuth2AuthenticationException("Email not authorized: " + oidcUser.getEmail());
        }

        String googleSub = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        User user = userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .orElseGet(() -> User.builder()
                                .email(email)
                                .build()));

        user.updateOidcInfo(googleSub, name);
        User savedUser = userRepository.save(user);

        return new CustomOidcUser(savedUser, oidcUser);
    }
}
