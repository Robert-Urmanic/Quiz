package org.example.restController;

import org.example.entity.Chapter;
import org.example.entity.Question;
import org.example.entity.Subchapter;
import org.example.repository.ChapterRepository;
import org.example.repository.SubchapterRepository;
import org.example.util.HibernateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        // model.addAttribute("message", "Hello from backend!");
        ChapterRepository cr = new ChapterRepository();

        model.addAttribute("chapters", cr.findAll());

        return "index"; // loads templates/index.html
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
    @ResponseBody
    public Map<String, Object> home() {
        return Map.of(
                "message", "Hello from Spring Boot",
                "status", "working",
                "number", 123
        );
    }

    @PostMapping("/addChapter")
    @ResponseBody
    public String addChapter(@RequestBody Map<String, String> data) {
        String name = data.get("name");

        Chapter chapter = new Chapter();
        chapter.setName(name);

        HibernateUtil.save(chapter);

        return "ok";
    }

    @PostMapping("/addSubchapter")
    @ResponseBody
    public String addSubchapter(@RequestBody Map<String, String> data) {
        String name = data.get("name");
        Integer chapterId = Integer.parseInt(data.get("chapterId"));

        ChapterRepository chapterRepository = new ChapterRepository();

        // Get the Chapter entity from the database
        Chapter chapter = chapterRepository.findById(chapterId);

        Subchapter subchapter = new Subchapter();
        subchapter.setName(name);
        subchapter.setChapter(chapter);

        HibernateUtil.save(subchapter);

        return "ok";
    }

    @PostMapping("/addQuestion")
    @ResponseBody
    public String addQuestion(@RequestBody Map<String, String> data) {
        String text = data.get("text");
        Integer chapterId = Integer.parseInt(data.get("chapterId"));
        Integer subchapterId = Integer.parseInt(data.get("subchapterId"));

        ChapterRepository chapterRepository = new ChapterRepository();

        // Get the Chapter entity from the database
        Chapter chapter = chapterRepository.findById(chapterId);

        SubchapterRepository subchapterRepository = new SubchapterRepository();

        Subchapter subchapter = subchapterRepository.findById(subchapterId);

        Question question = new Question();
        question.setQuestion(text);
        question.setChapter(chapter);
        question.setSubchapter(subchapter);

//        Subchapter subchapter = new Subchapter();
//        subchapter.setName(text);
//        subchapter.setChapter(chapter);

        HibernateUtil.save(question);

        return "ok";
    }
}