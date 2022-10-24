package com.epapers.epapers.schedulers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.EpapersSubscription;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.SubscriptionService;
import com.epapers.epapers.telegram.EpapersBot;

@SpringBootTest
@RunWith(SpringRunner.class)
public class AppSchedulersTest {
    @InjectMocks
    AppScheduler scheduler;

    @Mock
    EpaperService epaperService;

    @Mock
    SubscriptionService subscriptionService;

    @Autowired
    EpapersBot telegramBot;

    @Test
    public void EightAMDaily() throws Exception {
        List<EpapersSubscription> subscriptions = new ArrayList<>();
        Map<String, String> editions = new HashMap<>();
        editions.put("HT", "102");
        subscriptions.add(new EpapersSubscription("749434510", null, editions));

        Epaper epaper = new Epaper();
        File dummyfile = Mockito.mock(File.class);
        when(dummyfile.getPath()).thenReturn("/Users/gandalf/Documents/ebooks/kafka-using-spring-boot.pdf");
        epaper.setFile(dummyfile);
        Map<String, Object> response = new HashMap<>();
        response.put("epaper", epaper);

        when(subscriptionService.getAllSubscriptions()).thenReturn(subscriptions);
        when(epaperService.getHTpdf(any(), any())).thenReturn(response);
        when(telegramBot.sendSubscriptionMessage("749434510", "testing", dummyfile)).thenReturn(null);
        scheduler.telegramSubscriptions();
    }
    
}
