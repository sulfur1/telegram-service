package com.iprody08.telegramservice.telegram;

import com.iprody08.telegramservice.config.BotConfig;
import com.iprody08.telegramservice.customer.api.CustomerControllerApi;
import com.iprody08.telegramservice.customer.model.CustomerDto;
import com.iprody08.telegramservice.inquiry.api.InquiryControllerApi;
import com.iprody08.telegramservice.inquiry.api.SourceControllerApi;
import com.iprody08.telegramservice.inquiry.model.InquiryDto;
import com.iprody08.telegramservice.inquiry.model.SourceDto;
import com.iprody08.telegramservice.product.api.ProductControllerApi;
import com.iprody08.telegramservice.product.model.ProductDto;
import com.iprody08.telegramservice.telegram.dto.TelegramInquiryDto;
import com.iprody08.telegramservice.util.PageableProduct;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.iprody08.telegramservice.telegram.util.UtilityButton.cancelButton;
import static com.iprody08.telegramservice.telegram.util.UtilityButton.confirmButton;
import static com.iprody08.telegramservice.telegram.util.UtilityButton.countryButtons;

@Component
@Slf4j
public class InquiryTelegramBot extends TelegramBot {

    private static final com.iprody08.telegramservice.customer.invoker.ApiClient customerClient = new com.iprody08.telegramservice.customer.invoker.ApiClient();

    private static final com.iprody08.telegramservice.product.invoker.ApiClient productClient = new com.iprody08.telegramservice.product.invoker.ApiClient();

    private static final com.iprody08.telegramservice.inquiry.invoker.ApiClient inquiryClient = new com.iprody08.telegramservice.inquiry.invoker.ApiClient();

    private final Map<Long, TelegramInquiryDto> inquiryMap = new ConcurrentHashMap<>();

    private boolean startBot = true;

    private boolean startName = false;

    private boolean startEmail = false;

    private boolean startComment = false;

    @Autowired
    public InquiryTelegramBot(BotConfig botConfig) {
        super(botConfig.getBotToken());
    }

    @PostConstruct
    public void run() {
        log.info("Start telegram bot");
        this.setUpdatesListener(updateList -> {
            for (Update update : updateList) {
                if (update.message() != null) {
                    messageHandler(update);
                } else {
                    callbackQueryHandler(update);
                }
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void messageHandler(Update update) {
        Long id = update.message().from().id();
        String message = update.message().text();
        String telegramId = "@" + update.message().from().username();
        if (startBot) {
            startBot(id, telegramId);
        } else if (startName) {
            enterName(id, message);
        } else if (startEmail) {
            enterEmail(id, message);
        } else if (startComment){
            enterComment(id, message);
        }
    }


    private void callbackQueryHandler(Update update) {
        Long id = update.callbackQuery().from().id();
        String telegramId = "@" + update.callbackQuery().from().username();
        String[] data = update.callbackQuery().data().trim().split("=");
        String key = data[0].trim();
        String value = data[1].trim();

        switch (key) {
            case "productId" -> {
                inquiryMap.get(id).setProductRefId(Long.parseLong(value));
                SendMessage request = new SendMessage(
                        id, """
                        Please, enter your first and last name according to the template: 'Firstname Lastname'
                        """
                ).replyMarkup(cancelButton());
                startName = true;
                this.execute(request);
            }
            case "country" -> {
                String[] countryData = value.split("&");
                Long countryId = Long.parseLong(countryData[0]);
                String countryName = countryData[1];
                TelegramInquiryDto dto = inquiryMap.get(id);
                dto.setCountryId(countryId);
                dto.setCountryName(countryName);
                SendMessage request = new SendMessage(
                        id, """
                        Leave a comment on your inquiry (optional) or confirm it.
                        """
                ).replyMarkup(confirmButton());
                this.execute(request);
                startComment = true;
            }
            case "confirm" -> {
                createInquiry(id);
            }
            case "cancel" -> {
                cancel(id);
                startBot(id, telegramId);
            }
            default -> {

            }
        }

    }

    @SneakyThrows
    private void startBot(Long id, String telegramId) {
        productClient.setVerifyingSsl(false);
        ProductControllerApi productApi = new ProductControllerApi(productClient);
        productApi.setCustomBaseUrl("https://localhost/");
        List<ProductDto> listProduct = productApi.getAllProducts(PageableProduct.of(10));
        log.info(listProduct.toString());
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        for (ProductDto p : listProduct) {
            markup.addRow(new InlineKeyboardButton(p.getSummary())
                    .callbackData("productId=" + p.getId()));
        }
        SendMessage request = new SendMessage(
                id, "Welcome, please, choose product: "
        ).replyMarkup(markup);
        TelegramInquiryDto dto = new TelegramInquiryDto();
        dto.setTelegramId(telegramId);
        inquiryMap.put(id, dto);
        startBot = false;
        this.execute(request);
    }

    private void cancel(Long id) {
        SendMessage request = new SendMessage(
                id, "You cancel inquiry!"
        );
        inquiryMap.remove(id);
        startBot = true;
        startName = false;
        startEmail = false;
        startComment = false;
        this.execute(request);
    }

    @SneakyThrows
    private void createInquiry(Long id) {
        TelegramInquiryDto dto = inquiryMap.get(id);

        customerClient.setVerifyingSsl(false);
        CustomerControllerApi customerApi = new CustomerControllerApi(customerClient);
        customerApi.setCustomBaseUrl("https://localhost/");
        CustomerDto customer = new CustomerDto();
        customer.setName(dto.getFirstName());
        customer.setSurname(dto.getLastName());
        customer.setEmail(dto.getEmail());
        customer.setTelegramId(dto.getTelegramId());
        customer.countryId(dto.getCountryId());
        customer.setCountryName(dto.getCountryName());
        CustomerDto createdCustomer = customerApi.addCustomer(customer);
        dto.setCustomerRefId(createdCustomer.getId());

        inquiryClient.setVerifyingSsl(false);
        InquiryControllerApi inquiryApi = new InquiryControllerApi(inquiryClient);
        SourceControllerApi sourceApi = new SourceControllerApi(inquiryClient);
        sourceApi.setCustomBaseUrl("https://localhost/");
        inquiryApi.setCustomBaseUrl("https://localhost/");
        SourceDto sourceDto = new SourceDto();
        sourceDto.setName("Telegram bot");
        SourceDto createdSource = sourceApi.save(sourceDto);
        InquiryDto inquiryDto = new InquiryDto();
        inquiryDto.setComment(dto.getComment());
        inquiryDto.setSourceId(createdSource);
        inquiryDto.setProductRefId(dto.getProductRefId());
        inquiryDto.setCustomerRefId(dto.getCustomerRefId());

        InquiryDto createdInquiry = inquiryApi.save1(inquiryDto);
        SendMessage request = new SendMessage(
                id,
                String.format("Your inquiry is created! Id inquiry: %d", createdInquiry.getId())
        );
        this.execute(request);
    }

    private void enterComment(Long id, String comment) {
        inquiryMap.get(id).setComment(comment);
        SendMessage request = new SendMessage(
                id, """
                        Your comment is accepted. Confirm create inquiry.
                        """
        ).replyMarkup(confirmButton());
        this.execute(request);
        startComment = false;
    }
    private void enterEmail(Long id, String message) {
        SendMessage request;
        Pattern pattern = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
        String email = message.trim();
        Matcher m = pattern.matcher(email);
        if (m.find()) {
            TelegramInquiryDto dto = inquiryMap.get(id);
            dto.setEmail(email);

            log.info("Successful input!");
            request = new SendMessage(
                    id, """
                    Your email successfully save!
                    Please, input your country:
                    """
            ).replyMarkup(countryButtons());
            startEmail = false;
        } else {
            request = new SendMessage(
                    id, """
                    Invalid input. Please, retry again:
                    """
            ).replyMarkup(cancelButton());
        }
        this.execute(request);
    }

    private void enterName(Long id, String name) {
        SendMessage request;
        String[] parseName = name.trim().split(" ");
        if (parseName.length < 2 || parseName[0].isEmpty() || parseName[1].isEmpty()) {
            log.info("Invalid input!");
            request = new SendMessage(
                    id, """
                    Invalid input!
                    Please, enter your first and last name according to the template: 'Firstname Lastname'
                    """
            ).replyMarkup(cancelButton());
        } else  {
            TelegramInquiryDto dto = inquiryMap.get(id);
            dto.setFirstName(parseName[0]);
            dto.setLastName(parseName[1]);

            log.info("Successful input!");
            request = new SendMessage(
                    id, """
                    Your name successfully save!
                    Please, input your email:
                    """
            ).replyMarkup(cancelButton());
            startName = false;
            startEmail = true;
        }
        this.execute(request);
    }


}
