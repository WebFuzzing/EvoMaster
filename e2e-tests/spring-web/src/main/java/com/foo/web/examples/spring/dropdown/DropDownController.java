package com.foo.web.examples.spring.dropdown;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class DropDownController {

    @GetMapping("/")
    public String index0() {
        return "/dropdown/index.html";
    }

    @GetMapping("/navigate/{page}")
    public String navigate(@PathVariable String page) {
        String filePath = "static/dropdown/page" + page + ".html";

        // Check if file exists
        Resource resource = new ClassPathResource(filePath);
        if (!resource.exists()) {
            return "redirect:/error.html";
        }

        return "forward:/dropdown/page" + page + ".html"; // Serve static HTML file
    }
}
