package com.huawei.modellite.repository.modelweight.domain.vo;

import java.util.List;

import lombok.Data;

@Data
public class PageResult<T> {
    private List<T> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;
}