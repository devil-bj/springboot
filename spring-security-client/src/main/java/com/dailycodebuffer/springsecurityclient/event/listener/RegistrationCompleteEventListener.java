package com.dailycodebuffer.springsecurityclient.event.listener;


import com.dailycodebuffer.springsecurityclient.entity.User;
import com.dailycodebuffer.springsecurityclient.event.RegistrationCompleteEvent;
import com.dailycodebuffer.springsecurityclient.service.UserService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component

@Slf4j

public class RegistrationCompleteEventListener implements
        ApplicationListener <RegistrationCompleteEvent> {
    @Autowired
    private UserService userService;

    @Override
    public void onApplicationEvent(RegistrationCompleteEvent event) {
        //create type verification token for the user with link.
        User user = event.getUser();
        String token = UUID.randomUUID().toString();
        userService.saveVerificationTokenForUser(token,user);
        //send mail to user
        String url = event.getApplicationUrl() + "/verifyRegistration?token=" + token;

        //send verification email () here
        log.info("click the link to verify the account:{}",url);
    }
}
