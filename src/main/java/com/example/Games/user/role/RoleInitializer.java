package com.example.Games.user.role;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        List<Role> missingRoles = roles.stream()
                .filter(roleType -> !existing.contains(roleType))
                .map(roleType -> Role.builder().name(roleType).build())
                .toList();

        roleRepository.saveAll(missingRoles);

    }
}
