package com.iprody08.telegramservice.util;

import com.iprody08.telegramservice.customer.model.Pageable;

import java.util.List;

public interface PageableCustomer {

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
