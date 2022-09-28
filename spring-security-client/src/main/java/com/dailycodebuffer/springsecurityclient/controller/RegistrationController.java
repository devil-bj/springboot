package com.dailycodebuffer.springsecurityclient.controller;

import com.dailycodebuffer.springsecurityclient.entity.User;
import com.dailycodebuffer.springsecurityclient.entity.VerificationToken;
import com.dailycodebuffer.springsecurityclient.event.RegistrationCompleteEvent;
import com.dailycodebuffer.springsecurityclient.model.PasswordModel;
import com.dailycodebuffer.springsecurityclient.model.UserModel;
import com.dailycodebuffer.springsecurityclient.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;


@RestController
@Slf4j
public class RegistrationController {

    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private UserService userService;
    @PostMapping("/register")
    public String registerUser(@RequestBody UserModel userModel, final HttpServletRequest request){
        User user = userService.registerUser(userModel);
        publisher.publishEvent(new RegistrationCompleteEvent(
                user,
                applicationUrl(request)
        ));
        return "success";

    }

    @GetMapping("/verifyRegistration")
    public String verifyRegistration(@RequestParam("token") String token){
        String result = userService.validateVerificationToken(token);
        if(result.equalsIgnoreCase("valid")){
            return "user verified successfully.";
        }
        return "Bad User";

    }

    @GetMapping("/resendVerifyToken")
    public String resendVerificationToken(@RequestParam("token") String oldToken, HttpServletRequest request){
        VerificationToken verificationToken = userService.generateNewVerificationToken(oldToken);
        User user =verificationToken.getUser();
        resendVerificationTokenMail(user,applicationUrl(request),verificationToken);
        return "Verification link sent";
    }

    @PostMapping("/resetPassword")
    public String resetPassword(@RequestBody PasswordModel passwordModel, HttpServletRequest request){
        User user = userService.findUserByEmail(passwordModel.getEmail());
        String url  ="";
        if(user!=null){
            String token = UUID.randomUUID().toString();
           userService.createPasswordResetTokenForUser(user,token);
            url = passwordResetTokenMail(user,applicationUrl(request),token);

        }
        return url;

    }

    @PostMapping("/savePassword")
    public String savePassword(@RequestParam("token") String token,@RequestBody PasswordModel passwordModel){
        String result = userService.validatePasswordResetToken(token);
        if (!result.equalsIgnoreCase("valid")){
            return  "invalid token";
        }
        Optional<User> user = userService.getUserByPasswordResetToken(token);
        if(user.isPresent()){
            userService.changePassword(user.get(),passwordModel.getNewPassword());
            return"Password Reset Successfully";
        }
        else {
            return "invalid token";
        }

    }

    private String passwordResetTokenMail(User user, String applicationUrl, String token) {
        String url =
                applicationUrl
                        + "/savePassword?token="
                        +token;
        log.info("Click the link to reset your password: {}",url);
        return url;

    }

    @PostMapping("/changePassword")
    private String changePassword(@RequestBody PasswordModel passwordModel){
        User user = userService.findUserByEmail(passwordModel.getEmail());
        if(!userService.checkIfValidOldPassword(user,passwordModel.getOldPassword())){
            return "invalid Old Password";
        }
        userService.changePassword(user,passwordModel.getNewPassword());
        return "Password Changed Successfully";
    }

    private void resendVerificationTokenMail(User user, String applicationUrl, VerificationToken verificationToken) {
        String url =
                applicationUrl
                + "/verifyRegistration?token="
                +verificationToken.getToken();
        log.info("Click the link to verify your account: {}",url);
    }

    private String applicationUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() +
                ":" +
                request.getServerPort() +
                request.getContextPath();
    }
}
