package com.epapers.epapers.controller;

import com.epapers.epapers.model.EpapersSubscription;
import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.SubscriptionService;
import com.epapers.epapers.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    SubscriptionService subscriptionService;

    @PostMapping("/register")
    public Map<String, String> registerUser(@RequestBody EpapersUser user) {
        return userService.saveNewUser(user);
    }

    @GetMapping("/subscribers")
    public List<EpapersSubscription> getSubscribers() {
        return subscriptionService.getAllSubscriptions();
    }

    @GetMapping("/users")
    public List<EpapersUser> getUsers() {
        return userService.getAllUsers();
    }

}
