package amgn.amu.service;

import amgn.amu.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckService {

    private final UserMapper userMapper;

    public boolean existById(String id) {
        return userMapper.existsByLoginId(id);
    }
}
