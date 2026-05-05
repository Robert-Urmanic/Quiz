package org.example.restController;

import org.example.repository.ChapterRepository;
import org.example.repository.SubchapterRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        // model.addAttribute("message", "Hello from backend!");
        ChapterRepository cr = new ChapterRepository();

        model.addAttribute("chapters", cr.findAll());

        return "/index"; // loads templates/index.html
    }

    @GetMapping("/subchapters")
    @ResponseBody
    public List<Map<String, Object>> getSubchapters(@RequestParam int chapterId) {
        SubchapterRepository subchapterRepository = new SubchapterRepository();

        return subchapterRepository.findByChapterId(chapterId)
                .stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "name", s.getName()
                ))
                .toList();
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