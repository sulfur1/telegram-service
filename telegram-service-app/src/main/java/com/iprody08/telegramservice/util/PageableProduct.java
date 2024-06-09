package com.iprody08.telegramservice.util;

import com.iprody08.telegramservice.product.model.Pageable;

import java.util.List;

public interface PageableProduct {
    static Pageable of(Integer size) {
        Pageable pageable = new Pageable();
        pageable.setSize(size);
        return pageable;
    }

    static Pageable of(Integer size, List<String> sort) {
        Pageable pageable = new Pageable();
        pageable.setSize(size);
        pageable.setSort(sort);
        return pageable;
    }
}
