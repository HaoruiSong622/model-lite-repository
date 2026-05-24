package com.huawei.modellite.repository.weighttask.domain.aggregate.uploadtask;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public final class CifsCredentials {

    private final String username;
    private final String password;
}
