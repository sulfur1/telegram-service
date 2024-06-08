package com.iprody08.telegramservice.telegram.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TelegramInquiryDto {

    private String firstName;

    private String lastName;

    private String email;

    private String telegramId;

    private String country;

    private String comment;

    private Long productRefId;

    private Long customerRefId;

    private Long managerRefId;
}
