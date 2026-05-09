package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.entity.Role;
import linh.vn.cinegoticket.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }
}
