package com.forgeauth.server.api.oauth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DeviceController {

    @GetMapping("/device")
    public String deviceVerificationPage() {
        return "device";
    }
}
