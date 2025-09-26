package example;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/rating")  // Prefix all endpoints with /rating
public class RatingController {

    @GetMapping("/")
    public String home() {
        return "Rating Service Home";
    }

    @GetMapping("/action")
    public String action() {
        return "Rating Action";
    }
}
