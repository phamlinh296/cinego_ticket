package linh.vn.cinegoticket.mapper;

import linh.vn.cinegoticket.dto.request.UserUpdateRequest;
import linh.vn.cinegoticket.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapperInterface {
    @Mapping(target = "roles", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
