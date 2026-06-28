package com.pipeforge.auth.mapper;

import com.pipeforge.auth.dto.UserResponse;
import com.pipeforge.auth.entity.User;
import org.mapstruct.Mapper;

/** Maps {@link User} entities to their public {@link UserResponse} projection. */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
