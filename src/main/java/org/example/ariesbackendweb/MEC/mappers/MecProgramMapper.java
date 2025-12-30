package org.example.ariesbackendweb.MEC.mappers;

import org.example.ariesbackendweb.MEC.DTOs.MecProgramRequestDto;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramResponseDto;
import org.example.ariesbackendweb.MEC.DTOs.MecProgramUpdateDto;
import org.example.ariesbackendweb.MEC.entities.MecProgram;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface MecProgramMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "retrievalHost", source = "retrievalHost")
    MecProgram toEntity(MecProgramRequestDto dto);

    @Mapping(target = "retrievalHost", source = "retrievalHost")
    MecProgramResponseDto toResponseDto(MecProgram entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "retrievalHost", source = "retrievalHost")
    void updateEntityFromDto(MecProgramUpdateDto dto, @MappingTarget MecProgram entity);
}