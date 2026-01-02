package com.village.revive.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;

public class LoginUser extends User {

    private Long id; // ⭐ 自定义字段：用户ID

    public LoginUser(Long id, String username, String password,
                     Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    // 构造方法、getter、setter 省略
}

