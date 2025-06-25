package com.adepuu.blog.domain.entity.converter;

import com.adepuu.blog.domain.entity.User;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserRoleConverter implements AttributeConverter<User.UserRole, String> {

    @Override
    public String convertToDatabaseColumn(User.UserRole role) {
        if (role == null) {
            return null;
        }
        return role.name();
    }

    @Override
    public User.UserRole convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        return User.UserRole.valueOf(dbData);
    }
    
}
