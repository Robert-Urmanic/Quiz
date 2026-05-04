package org.example.restController;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("message", "Hello from backend!");
        return "/index"; // loads templates/index.html
    }

    @GetMapping("/json")
    public Map<String, Object> home() {
        return Map.of(
                "message", "Hello from Spring Boot",
                "status", "working",
                "number", 123
        );
    }
}