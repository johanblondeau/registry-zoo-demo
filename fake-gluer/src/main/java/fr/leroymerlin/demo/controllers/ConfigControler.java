package fr.leroymerlin.demo.controllers;

import fr.leroymerlin.demo.registry.RegistryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConfigControler {

    @Autowired
    private RegistryService registryService;

    @GetMapping("/config")
    public String getConfig(Model model) {

        model.addAttribute("applications", registryService.getRegisteredApplications());
        return "config";
    }

}
