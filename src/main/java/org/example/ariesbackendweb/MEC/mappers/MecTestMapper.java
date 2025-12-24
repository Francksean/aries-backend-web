package org.example.ariesbackendweb.MEC.mappers;

import org.example.ariesbackendweb.MEC.DTOs.MecTestResponseDto;
import org.example.ariesbackendweb.MEC.MecTest;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "spring")
public interface MecTestMapper {


    @Mapping(target = "programName", source = "program.code")
    @Mapping(target = "programId", source = "program.id")
    MecTestResponseDto toResponseDto(MecTest entity);

}