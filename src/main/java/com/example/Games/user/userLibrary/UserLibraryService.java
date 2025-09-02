package com.example.Games.user.userLibrary;

import com.example.Games.config.exception.ResourceNotFoundException;
import com.example.Games.game.GameMapStruct;
import com.example.Games.game.dto.Response;
import com.example.Games.user.User;
import com.example.Games.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserLibraryService {

    private final UserLibraryRepository userLibraryRepository;
    private final UserRepository userRepository;
    private final GameMapStruct gameMapStruct;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<Response> getUserLibrary() {
        User user = getCurrentUser();
        return userLibraryRepository.findByUser(user)
                .stream()
                .map(lib -> gameMapStruct.toDto(lib.getGame()))
                .toList();
    }

    @Transactional
    public void removeGameFromLibrary(Long gameId) {
        User user = getCurrentUser();
        var userLibrary = userLibraryRepository.findByUserIdAndGameId(user.getId(), gameId)
                .orElseThrow(() -> new ResourceNotFoundException("Game not found in user library"));

        userLibraryRepository.delete(userLibrary);
    }

    public boolean hasGame(Long gameId) {
        User user = getCurrentUser();
        return userLibraryRepository.existsByUserIdAndGameId(user.getId(), gameId);
    }
}
