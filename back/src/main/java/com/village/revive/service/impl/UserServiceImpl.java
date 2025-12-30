package com.village.revive.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.village.revive.dto.*;
import com.village.revive.entity.User;
import com.village.revive.exception.ServiceException;
import com.village.revive.mapper.UserMapper;
import com.village.revive.service.UserService;
import com.village.revive.utils.EmailUtils;
import com.village.revive.utils.JwtUtils;
import com.village.revive.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmailUtils emailUtils;
    private final PasswordEncoder passwordEncoder;
    @Override
    public boolean usernameExists(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        return userMapper.checkUsernameExists(username) > 0;
    }

    @Override
    public boolean emailExists(String email) {
        return false;
    }

    @Override
    public Map<String, Long> countUsersByRole() {
        return null;
    }

    @Override
    public UserDTO getCurrentUserInfo() {
        return null;
    }

    @Override
    public Map<String, Object> getUserStats() {
        return null;
    }

    @Override
    public User getByUsername(String username) {
        return null;
    }

    @Override
    public Result<String> sendRegisterCode(String email) {
        return null;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication =authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        //保存信息到上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);
        //查询用户
        User user = userMapper.findByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        //生成token
        String token = jwtUtils.generateToken(user.getUsername(), user.getRole());
        //返回结果
        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .token(token)
                .role(user.getRole())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .realName(user.getRealName())
                .build();
    }

    @Override
    public UserInfoDto getUserInfo(String username) {
        return null;
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        return null;
    }

    @Override
    public UserDTO getUserById(Long id) {
        return null;
    }

    @Override
    public Long register(UserRegisterDTO registerDTO) {
        // 验证两次密码是否一致
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new ServiceException("两次密码输入不一致");
        }

        // 检查用户名是否已存在
        if (usernameExists(registerDTO.getUsername())) {
            throw new ServiceException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (emailExists(registerDTO.getEmail())) {
            throw new ServiceException("邮箱已被注册");
        }
        //验证码
        if (!emailUtils.validateCode(registerDTO.getEmail(), registerDTO.getVerifyCode())) {
            throw new ServiceException("验证码错误");
        }
        // 创建用户对象
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setPhoneNumber(registerDTO.getPhoneNumber());
        user.setRealName(registerDTO.getRealName());
        user.setRole("USER"); // 默认注册为普通用户
        user.setEnabled(true);
        user.setLocked(false);
        user.setDeleted(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 保存用户
        save(user);

        return user.getId();
    }

    @Override
    public boolean updateUserInfo(UserUpdateDTO updateDTO, String currentUsername) {
        return false;
    }

    @Override
    public boolean updateUser(UserDTO userDTO) {
        return false;
    }

    @Override
    public boolean updateProfile(UserDTO userDTO) {
        return false;
    }

    @Override
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        return false;
    }

    @Override
    public IPage<UserDTO> pageUsers(UserQueryDTO queryDTO) {
        return null;
    }

    @Override
    public Object getUserPage(Integer current, Integer size, String username, String email, String phoneNumber, String role, Boolean enabled) {
        return null;
    }

    @Override
    public boolean addUser(UserDTO userDTO) {
        return false;
    }

    @Override
    public boolean updateUserStatus(Long id, Boolean enabled) {
        return false;
    }

    @Override
    public boolean resetPassword(Long id) {
        return false;
    }

    @Override
    public boolean deleteUser(Long id) {
        return false;
    }
}
