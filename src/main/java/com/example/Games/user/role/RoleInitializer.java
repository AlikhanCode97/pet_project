package com.example.Games.user.role;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        Set<RoleType> existing = roleRepository.findAll()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        List<RoleType> roles = List.of(RoleType.values());

        roles.stream().filter(roleType -> !existing.contains(roleType))
                .forEach(roleType -> roleRepository.save(Role.builder()
                        .name(roleType)
                        .build()));
    }
}
