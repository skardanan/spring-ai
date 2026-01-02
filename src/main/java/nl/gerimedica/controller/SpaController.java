package nl.gerimedica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {"/", "/chat", "/dashboard"})
    public String forward() {
        return "forward:/index.html";
    }
}