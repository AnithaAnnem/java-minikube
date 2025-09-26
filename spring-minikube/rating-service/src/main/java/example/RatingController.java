package example;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class RatingController {

    @GetMapping("/")   // root path
    public String home() {
        return "Rating Service Home";
    }

    @GetMapping("/action")
    public String action() {
        return "Rating Action";
    }
}
