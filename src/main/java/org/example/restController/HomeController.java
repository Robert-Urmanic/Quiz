package org.example.restController;

import org.example.entity.Chapter;
import org.example.entity.Question;
import org.example.entity.Subchapter;
import org.example.repository.ChapterRepository;
import org.example.repository.QuestionRepository;
import org.example.repository.SubchapterRepository;
import org.example.util.HibernateUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    /** How many questions the panel on the index page lists. */
    private static final int LATEST_QUESTION_COUNT = 15;

    // The repositories hold no state, so one instance each is enough.
    private final ChapterRepository chapterRepository = new ChapterRepository();
    private final SubchapterRepository subchapterRepository = new SubchapterRepository();
    private final QuestionRepository questionRepository = new QuestionRepository();

    /* ----------------------------------------------------------------- page */

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("chapters", chapterRepository.findAll());

        return "index"; // loads templates/index.html
    }

    /* -------------------------------------------------------------- reading */

    @GetMapping("/api/subchapters")
    @ResponseBody
    public List<Map<String, Object>> subchapters(@RequestParam int chapterId) {
        return subchapterRepository.findByChapterId(chapterId)
                .stream()
                .map(HomeController::view)
                .toList();
    }

    /**
     * Feeds the question panel. Both parameters are optional: a missing
     * chapterId or subchapterId widens the selection instead of emptying it, so
     * the panel has something to show before a chapter is picked.
     */
    @GetMapping("/api/questions")
    @ResponseBody
    public Map<String, Object> questions(
            @RequestParam(required = false) Integer chapterId,
            @RequestParam(required = false) Integer subchapterId) {

        List<Map<String, Object>> items = questionRepository
                .findLatest(chapterId, subchapterId, LATEST_QUESTION_COUNT)
                .stream()
                .map(HomeController::view)
                .toList();

        return Map.of(
                "items", items,
                "total", questionRepository.count(chapterId, subchapterId)
        );
    }

    /* -------------------------------------------------------------- writing */

    @PostMapping("/api/chapters")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addChapter(@RequestBody NewChapter body) {
        String name = trimmed(body.name());

        if (name == null) {
            return badRequest("A chapter needs a name.");
        }

        Chapter existing = chapterRepository.findByName(name);
        if (existing != null) {
            return duplicate("Chapter " + quoted(existing.getName()) + " already exists.",
                    existing.getId());
        }

        Chapter chapter = new Chapter();
        chapter.setName(name);

        return created(view(HibernateUtil.save(chapter)));
    }

    @PostMapping("/api/chapters/{chapterId}/subchapters")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addSubchapter(
            @PathVariable int chapterId, @RequestBody NewSubchapter body) {

        String name = trimmed(body.name());

        if (name == null) {
            return badRequest("A subchapter needs a name.");
        }

        Chapter chapter = chapterRepository.findById(chapterId);
        if (chapter == null) {
            return gone("That chapter no longer exists. Reload the page.");
        }

        Subchapter existing = subchapterRepository.findByChapterIdAndName(chapterId, name);
        if (existing != null) {
            return duplicate(
                    quoted(existing.getName()) + " already exists under " + chapter.getName() + ".",
                    existing.getId());
        }

        Subchapter subchapter = new Subchapter();
        subchapter.setName(name);
        subchapter.setChapter(chapter);

        return created(view(HibernateUtil.save(subchapter)));
    }

    @PostMapping("/api/questions")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addQuestion(@RequestBody NewQuestion body) {
        if (body.subchapterId() == null) {
            return badRequest("Pick a chapter and subchapter first.");
        }

        String text = trimmed(body.text());
        if (text == null) {
            return badRequest("A question needs some text.");
        }

        Subchapter subchapter = subchapterRepository.findById(body.subchapterId());
        if (subchapter == null) {
            return gone("That subchapter no longer exists. Reload the page.");
        }

        Question question = new Question();
        question.setQuestion(text);
        question.setAnswer(trimmed(body.answer()));
        question.setSubchapter(subchapter);
        // The chapter comes from the subchapter, so the two can no longer
        // disagree the way they could when the client sent both ids.
        question.setChapter(subchapter.getChapter());

        return created(view(HibernateUtil.save(question)));
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

    /* ------------------------------------------------------- request bodies */

    public record NewChapter(String name) {}

    public record NewSubchapter(String name) {}

    public record NewQuestion(Integer subchapterId, String text, String answer) {}

    /* ---------------------------------------------------------------- views */

    private static Map<String, Object> view(Chapter chapter) {
        return Map.of("id", chapter.getId(), "name", chapter.getName());
    }

    private static Map<String, Object> view(Subchapter subchapter) {
        return Map.of("id", subchapter.getId(), "name", subchapter.getName());
    }

    private static Map<String, Object> view(Question question) {
        // Map.of() would reject the nulls that show up when a question has no
        // answer yet, or no chapter attached in older rows.
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", question.getId());
        row.put("question", question.getQuestion());
        row.put("answer", question.getAnswer());
        row.put("chapter", question.getChapter() == null ? null : question.getChapter().getName());
        row.put("subchapter", question.getSubchapter() == null ? null : question.getSubchapter().getName());
        return row;
    }

    /* -------------------------------------------------------------- replies */

    private static ResponseEntity<Map<String, Object>> created(Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private static ResponseEntity<Map<String, Object>> gone(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", message));
    }

    /**
     * A name that is already taken is not really a failure: the page reacts by
     * selecting what is already there, so the existing id travels along with
     * the message.
     */
    private static ResponseEntity<Map<String, Object>> duplicate(String message, int existingId) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", message, "existingId", existingId));
    }

    private static String quoted(String value) {
        return "\"" + value + "\"";
    }

    private static String trimmed(String value) {
        if (value == null) {
            return null;
        }

        String stripped = value.strip();

        return stripped.isEmpty() ? null : stripped;
    }
}
