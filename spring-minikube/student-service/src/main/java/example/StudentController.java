package example;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/student")  // prefix all endpoints
public class StudentController {

    @GetMapping("/")
    public String home() {
        return "Student Service Home";
    }

    @GetMapping("/info")
    public String info() {
        return "Student Info";
    }
}
