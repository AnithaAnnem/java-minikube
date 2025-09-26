package example;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
public class StudentController {

    @GetMapping("/")   // root path for Ingress
    public String home() {
        return "Student Service Home";
    }

    @GetMapping("/info")
    public String info() {
        return "Student Info";
    }
}
