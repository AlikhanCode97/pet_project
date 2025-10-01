package com.example.Games.user.role;

import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "roles")
public class Role {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleType name;
}
