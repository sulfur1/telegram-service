package com.iprody08.telegramservice.telegram.util;

import com.iprody08.telegramservice.customer.api.CountryControllerApi;
import com.iprody08.telegramservice.customer.invoker.ApiClient;
import com.iprody08.telegramservice.customer.invoker.ApiException;
import com.iprody08.telegramservice.customer.model.CountryDto;
import com.iprody08.telegramservice.util.PageableCustomer;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

import java.util.List;

public final class UtilityButton {

    private UtilityButton() {

    }

    public static InlineKeyboardMarkup cancelButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.addRow(new InlineKeyboardButton("Cancel inquiry")
                .callbackData("cancel=true"));
        return markup;
    }

    public static InlineKeyboardMarkup confirmButton() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.addRow(new InlineKeyboardButton("Confirm")
                .callbackData("confirm=true"));
        markup.addRow(new InlineKeyboardButton("Cancel inquiry")
                .callbackData("cancel=true"));
        return markup;
    }

    public static InlineKeyboardMarkup countryButtons() {
        List<CountryDto> countries;
        try {
            ApiClient apiClient = new ApiClient();
            apiClient.setVerifyingSsl(false);
            CountryControllerApi customerApi = new CountryControllerApi(apiClient);
            customerApi.setCustomBaseUrl("https://localhost/");
            countries = customerApi.getAllCountries(PageableCustomer.of(260));
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        for (CountryDto country : countries) {
            markup.addRow(new InlineKeyboardButton(country.getName())
                    .callbackData(String.format("country=%d&%s", country.getId(), country.getName())));
        }
        markup.addRow(new InlineKeyboardButton("Cancel inquiry")
                .callbackData("cancel=true"));

        return markup;
    }
}
