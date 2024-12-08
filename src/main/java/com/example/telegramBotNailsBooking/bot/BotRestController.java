package com.example.telegramBotNailsBooking.bot;

import com.example.telegramBotNailsBooking.bot.commands.CommandController;
import com.example.telegramBotNailsBooking.model.Users;
import com.example.telegramBotNailsBooking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot")
public class BotRestController {

    @Autowired
    private UserService userService;

    @Autowired
    private CommandController commandController;


}
