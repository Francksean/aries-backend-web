package org.example.ariesbackendweb.common.api;

import lombok.Data;

@Data
public class GenericResponse<T> {
    private T data;
}
