package com.sriundee.preorder.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.sriundee.preorder.service.WebsiteQuickAddService;

/** A real reference row keeps offers without a source compatible with existing order joins. */
@Component
public class DefaultWebsiteInitializer implements ApplicationRunner {
    private final WebsiteQuickAddService websites;
    public DefaultWebsiteInitializer(WebsiteQuickAddService websites) { this.websites = websites; }
    @Override
    public void run(ApplicationArguments args) { websites.add(WebsiteQuickAddService.NO_WEBSITE); }
}
