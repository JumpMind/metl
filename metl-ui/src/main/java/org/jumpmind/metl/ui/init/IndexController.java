package org.jumpmind.metl.ui.init;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class IndexController {
    @GetMapping("/")
    public RedirectView redirectWithRedirectAttributes(RedirectAttributes attributes) {
        return new RedirectView("/metl/app/");
    }
}
