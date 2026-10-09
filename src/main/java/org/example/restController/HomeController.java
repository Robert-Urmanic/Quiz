package org.example.restController;

import org.example.entity.Book;
import org.example.entity.Chapter;
import org.example.entity.Question;
import org.example.entity.Subchapter;
import org.example.repository.BookRepository;
import org.example.repository.ChapterRepository;
import org.example.repository.QuestionRepository;
import org.example.repository.SubchapterRepository;
import org.example.util.HibernateUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    /** How many questions the panel on the index page lists. */
    private static final int LATEST_QUESTION_COUNT = 15;

    // The repositories hold no state, so one instance each is enough.
    private final BookRepository bookRepository = new BookRepository();
    private final ChapterRepository chapterRepository = new ChapterRepository();
    private final SubchapterRepository subchapterRepository = new SubchapterRepository();
    private final QuestionRepository questionRepository = new QuestionRepository();

    /* ----------------------------------------------------------------- page */

    @GetMapping("/")
    public String index(Model model) {
        // Only the books travel with the page now. Chapters depend on which
        // book is picked, so they are fetched the same way subchapters are.
        model.addAttribute("books", bookRepository.findAll());

        return "index"; // loads templates/index.html
    }

    @GetMapping("/questions")
    public String browse(Model model) {
        // The book is picked on the page itself; everything below it arrives
        // through /api/books/{bookId}/outline.
        model.addAttribute("books", bookRepository.findAll());

        return "questions";
    }

    /* -------------------------------------------------------------- reading */

    @GetMapping("/api/chapters")
    @ResponseBody
    public List<Map<String, Object>> chapters(@RequestParam int bookId) {
        return chapterRepository.findByBookId(bookId)
                .stream()
                .map(HomeController::view)
                .toList();
    }

    @GetMapping("/api/subchapters")
    @ResponseBody
    public List<Map<String, Object>> subchapters(@RequestParam int chapterId) {
        return subchapterRepository.findByChapterId(chapterId)
                .stream()
                .map(HomeController::view)
                .toList();
    }

    /**
     * Feeds the question panel. All three parameters are optional: a missing
     * one widens the selection instead of emptying it, so the panel has
     * something to show before a book is picked.
     */
    @GetMapping("/api/questions")
    @ResponseBody
    public Map<String, Object> questions(
            @RequestParam(required = false) Integer bookId,
            @RequestParam(required = false) Integer chapterId,
            @RequestParam(required = false) Integer subchapterId) {

        List<Map<String, Object>> items = questionRepository
                .findLatest(bookId, chapterId, subchapterId, LATEST_QUESTION_COUNT)
                .stream()
                .map(HomeController::view)
                .toList();

        return Map.of(
                "items", items,
                "total", questionRepository.count(bookId, chapterId, subchapterId)
        );
    }

    /**
     * The whole book as the overview page draws it: chapters, their
     * subchapters, and each subchapter's questions in their numbered order.
     * Empty chapters and subchapters come along too, so the page shows where
     * questions are still missing. Three queries in total, however big the
     * book is.
     */
    @GetMapping("/api/books/{bookId}/outline")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> outline(@PathVariable int bookId) {
        Book book = bookRepository.findById(bookId);
        if (book == null) {
            return gone("That book no longer exists. Reload the page.");
        }

        Map<Integer, List<Subchapter>> subchaptersByChapter = new HashMap<>();
        for (Subchapter subchapter : subchapterRepository.findByBookId(bookId)) {
            subchaptersByChapter
                    .computeIfAbsent(subchapter.getChapter().getId(), id -> new ArrayList<>())
                    .add(subchapter);
        }

        // The query already returns the questions in reading order, and
        // appending keeps it within every group.
        Map<Integer, List<Map<String, Object>>> questionsBySubchapter = new HashMap<>();
        Map<Integer, List<Map<String, Object>>> unfiledByChapter = new HashMap<>();
        for (Question question : questionRepository.findByBookId(bookId)) {
            if (question.getSubchapter() != null) {
                questionsBySubchapter
                        .computeIfAbsent(question.getSubchapter().getId(), id -> new ArrayList<>())
                        .add(view(question));
            } else {
                // Left over from before every question had to have a
                // subchapter. Shown so they are not lost, but they cannot be
                // reordered, since the numbering lives inside a subchapter.
                unfiledByChapter
                        .computeIfAbsent(question.getChapter().getId(), id -> new ArrayList<>())
                        .add(view(question));
            }
        }

        List<Map<String, Object>> chapters = new ArrayList<>();
        for (Chapter chapter : chapterRepository.findByBookId(bookId)) {
            List<Map<String, Object>> subchapters = new ArrayList<>();
            for (Subchapter subchapter : subchaptersByChapter.getOrDefault(chapter.getId(), List.of())) {
                Map<String, Object> node = new LinkedHashMap<>(view(subchapter));
                node.put("questions", questionsBySubchapter.getOrDefault(subchapter.getId(), List.of()));
                subchapters.add(node);
            }

            Map<String, Object> node = new LinkedHashMap<>(view(chapter));
            node.put("subchapters", subchapters);
            node.put("unfiled", unfiledByChapter.getOrDefault(chapter.getId(), List.of()));
            chapters.add(node);
        }

        return ResponseEntity.ok(Map.of("book", view(book), "chapters", chapters));
    }

    /* -------------------------------------------------------------- writing */

    @PostMapping("/api/books")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addBook(@RequestBody NewBook body) {
        String title = trimmed(body.name());

        if (title == null) {
            return badRequest("A book needs a title.");
        }

        Book existing = bookRepository.findByTitle(title);
        if (existing != null) {
            return duplicate("Book " + quoted(existing.getTitle()) + " already exists.",
                    existing.getId());
        }

        Book book = new Book();
        book.setTitle(title);

        return created(view(HibernateUtil.save(book)));
    }

    @PostMapping("/api/books/{bookId}/chapters")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addChapter(
            @PathVariable int bookId, @RequestBody NewChapter body) {

        String name = trimmed(body.name());

        if (name == null) {
            return badRequest("A chapter needs a name.");
        }

        Book book = bookRepository.findById(bookId);
        if (book == null) {
            return gone("That book no longer exists. Reload the page.");
        }

        // Only unique within the book: two books may each have a "Generics".
        Chapter existing = chapterRepository.findByBookIdAndName(bookId, name);
        if (existing != null) {
            return duplicate(
                    quoted(existing.getName()) + " already exists in " + book.getTitle() + ".",
                    existing.getId());
        }

        Chapter chapter = new Chapter();
        chapter.setName(name);
        chapter.setBook(book);

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
            return badRequest("Pick a book, chapter and subchapter first.");
        }

        String text = trimmed(body.text());
        Integer page = body.page();

        String problem = problemWith(text, page);
        if (problem != null) {
            return badRequest(problem);
        }

        Subchapter subchapter = subchapterRepository.findById(body.subchapterId());
        if (subchapter == null) {
            return gone("That subchapter no longer exists. Reload the page.");
        }

        Question question = new Question();
        question.setQuestion(text);
        question.setAnswer(trimmed(body.answer()));
        question.setPage(page);
        question.setSubchapter(subchapter);
        // The chapter comes from the subchapter, so the two can no longer
        // disagree the way they could when the client sent both ids.
        question.setChapter(subchapter.getChapter());

        return created(view(HibernateUtil.save(question)));
    }

    @PutMapping("/api/questions/{questionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editQuestion(
            @PathVariable int questionId, @RequestBody EditedQuestion body) {

        String text = trimmed(body.text());
        Integer page = body.page();

        String problem = problemWith(text, page);
        if (problem != null) {
            return badRequest(problem);
        }

        // Asked first, apart from the update, so the reply can say which of
        // the two went missing.
        if (body.subchapterId() != null && subchapterRepository.findById(body.subchapterId()) == null) {
            return gone("That subchapter no longer exists. Reload the page.");
        }

        Question question = questionRepository.update(
                questionId, text, trimmed(body.answer()), page, body.subchapterId());
        if (question == null) {
            return gone("That question no longer exists. Reload the page.");
        }

        return ResponseEntity.ok(view(question));
    }

    /**
     * Stores a new order for one subchapter. The page sends every question id
     * of the subchapter, first to last, and the server numbers them from 1.
     */
    @PutMapping("/api/subchapters/{subchapterId}/order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> reorder(
            @PathVariable int subchapterId, @RequestBody NewOrder body) {

        if (body.questionIds() == null) {
            return badRequest("The new order is missing.");
        }

        if (subchapterRepository.findById(subchapterId) == null) {
            return gone("That subchapter no longer exists. Reload the page.");
        }

        if (!questionRepository.reorder(subchapterId, body.questionIds())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message",
                    "Questions were added or moved since this page loaded. Reload it and try again."));
        }

        return ResponseEntity.ok(Map.of("questionIds", body.questionIds()));
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

    public record NewBook(String name) {}

    public record NewChapter(String name) {}

    public record NewSubchapter(String name) {}

    public record NewQuestion(Integer subchapterId, String text, String answer, Integer page) {}

    /** A null subchapterId leaves the question where it is. */
    public record EditedQuestion(String text, String answer, Integer page, Integer subchapterId) {}

    public record NewOrder(List<Integer> questionIds) {}

    /* ---------------------------------------------------------------- views */

    /**
     * A book travels as "name" like every other level does, so one dropdown
     * helper on the page can fill all three selects.
     */
    private static Map<String, Object> view(Book book) {
        return Map.of("id", book.getId(), "name", book.getTitle());
    }

    private static Map<String, Object> view(Chapter chapter) {
        return Map.of("id", chapter.getId(), "name", chapter.getName());
    }

    private static Map<String, Object> view(Subchapter subchapter) {
        return Map.of("id", subchapter.getId(), "name", subchapter.getName());
    }

    private static Map<String, Object> view(Question question) {
        // Map.of() would reject the nulls that show up when a question has no
        // answer or no page yet.
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", question.getId());
        row.put("question", question.getQuestion());
        row.put("answer", question.getAnswer());
        row.put("page", question.getPage());
        row.put("book", bookTitleOf(question));
        row.put("chapter", question.getChapter() == null ? null : question.getChapter().getName());
        row.put("subchapter", question.getSubchapter() == null ? null : question.getSubchapter().getName());
        return row;
    }

    private static String bookTitleOf(Question question) {
        Chapter chapter = question.getChapter();

        return chapter == null || chapter.getBook() == null ? null : chapter.getBook().getTitle();
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

    /**
     * The rules a question has to pass whether it is new or edited, or null
     * when it passes them. The page is optional, but a nonsensical one is
     * refused rather than stored, because nothing downstream would ever
     * question it again.
     */
    private static String problemWith(String text, Integer page) {
        if (text == null) {
            return "A question needs some text.";
        }

        if (page != null && page < 1) {
            return "Page numbers start at 1.";
        }

        return null;
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
