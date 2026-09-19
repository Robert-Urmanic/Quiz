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
        if (text == null) {
            return badRequest("A question needs some text.");
        }

        // The page is optional, but a nonsensical one is refused rather than
        // stored, because nothing downstream would ever question it again.
        Integer page = body.page();
        if (page != null && page < 1) {
            return badRequest("Page numbers start at 1.");
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
