package amgn.amu.service;

import amgn.amu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OauthBridgeService {
    private final UserRepository userRepository;

}
