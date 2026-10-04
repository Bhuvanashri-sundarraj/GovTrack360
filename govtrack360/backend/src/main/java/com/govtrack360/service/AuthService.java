package com.govtrack360.service;

import com.govtrack360.dto.auth.AuthUserResponse;
import com.govtrack360.dto.auth.LoginRequest;
import com.govtrack360.dto.auth.LoginResponse;
import com.govtrack360.entity.Employee;
import com.govtrack360.entity.User;
import com.govtrack360.repository.EmployeeRepository;
import com.govtrack360.repository.UserRepository;
import com.govtrack360.security.CustomUserDetails;
import com.govtrack360.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().name());

        auditLogService.log(user, "LOGIN", "AUTH", user.getEmail() + " signed in.");

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresInMs(expirationMs)
                .build();
    }

    public AuthUserResponse getCurrentUser(User user) {
        Optional<Employee> employee = employeeRepository.findByUser(user);

        return AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .employeeId(employee.map(Employee::getId).orElse(null))
                .employeeCode(employee.map(Employee::getEmployeeId).orElse(null))
                .fullName(employee.map(e -> e.getFirstName() + " " + e.getLastName()).orElse(null))
                .build();
    }
}
