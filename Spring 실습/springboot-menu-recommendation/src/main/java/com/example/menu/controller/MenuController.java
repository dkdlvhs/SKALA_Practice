package com.example.menu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.menu.dto.MenuResponse;
import com.example.menu.service.MenuService;

/**
 * 브라우저의 HTTP 요청을 받아 메뉴 추천 결과를 반환한다.
 */
@RestController
@RequestMapping("/api")
public class MenuController {

    private final MenuService menuService;

    /**
     * Spring Container가 MenuService Bean을 생성자에 주입합니다.
     */
    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable("name") String name) {
        return name + "님, 오늘도 맛있는 하루 보내세요!";
    }

    @GetMapping("/menu")
    public String menu() {
        return "오늘의 추천 메뉴는 " + menuService.recommend() + "입니다.";
    }

    @GetMapping("/menu/random")
    public String randomMenu() {
        return "오늘은 " + menuService.randomMenu() + " 어떠세요?";
    }

    @GetMapping("/menu/{category}")
    public String menuByCategory(@PathVariable("category") String category) {
        String menu = menuService.recommendByCategory(category);
        return category + " 추천 메뉴는 " + menu + "입니다.";
    }

    @GetMapping("/menu/json/{category}")
    public MenuResponse menuJson(@PathVariable("category") String category) {
        String menu = menuService.recommendByCategory(category);

        return new MenuResponse(
                category,
                menu,
                "오늘은 " + menu + " 어떠세요?"
        );
    }

    @GetMapping("/menu/weather/{weather}")
    public String menuByWeather(@PathVariable("weather") String weather) {
        String menu = menuService.recommendByWeather(weather);
        return weather + " 날씨에 어울리는 메뉴는 " + menu + "입니다.";
    }

    @GetMapping("/menu/mood/{mood}")
    public String menuByMood(@PathVariable("mood") String mood) {
        String menu = menuService.recommendByMood(mood);
        return mood + " 기분에 어울리는 메뉴는 " + menu + "입니다.";
    }

    @GetMapping("/menu/price/search")
    public String menuByPrice(@RequestParam("min") int min, @RequestParam("max") int max) {
        java.util.List<String> results = menuService.recommendByPriceRange(min, max);
        if (results.isEmpty()) {
            return "가격 범위 " + min + "원 ~ " + max + "원에 해당하는 추천 메뉴가 없습니다.";
        }

        String formatted = results.stream()
                .map(item -> "- " + item)
                .collect(java.util.stream.Collectors.joining("\n"));

        return "가격 범위 " + min + "원 ~ " + max + "원에 해당하는 추천 메뉴:\n" + formatted;
    }

    @GetMapping("/menu/combination")
    public String menuBySolo() {
        String combination = menuService.recommendCombination();
        return "추천 메뉴 조합은 " + combination + "입니다.";
    }
}
