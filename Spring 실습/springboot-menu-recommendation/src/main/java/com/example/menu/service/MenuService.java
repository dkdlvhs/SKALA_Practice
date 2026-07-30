package com.example.menu.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

/**
 * 메뉴 추천 비즈니스 로직을 담당하는 Spring Bean입니다.
 *
 * @Service를 사용하면 Component Scan을 통해 Spring Container에
 * 자동으로 Bean으로 등록됩니다.
 */
@Service
public class MenuService {

    private final List<String> menus = List.of(
            "김치찌개",
            "불고기",
            "짜장면",
            "돈가스",
            "떡볶이",
            "치킨",
            "피자",
            "햄버거",
            "아이스크림",
            "붕어빵"
    );

    private final Map<String, Integer> menuPrices = Map.of(

            "김치찌개", 8000,
            "불고기", 12000,
            "짜장면", 7000,
            "돈가스", 10000,
            "떡볶이", 6000,
            "치킨", 15000,
            "피자", 20000,
            "햄버거", 9000,
            "아이스크림", 1500,
            "붕어빵", 3000
    );

    public String recommend() {
        return "김치찌개";
    }

    public String recommendByCategory(String category) {
        return switch (category) {
            case "korean" -> "불고기";
            case "chinese" -> "짜장면";
            case "japanese" -> "돈가스";
            case "snack" -> "떡볶이";
            default -> "추천 가능한 메뉴가 없습니다";
        };
    }

    public String randomMenu() {
        int index = ThreadLocalRandom.current().nextInt(menus.size());
        return menus.get(index);
    }

    public String recommendByWeather(String weather) {
        return switch (weather) {
            case "sunny" -> "아이스크림";
            case "rainy" -> "돈까스";
            case "snowy" -> "붕어빵";
            default -> "추천 가능한 메뉴가 없습니다";
        };
    }

    public String recommendByMood (String moode) {
        return switch (moode) {
            case "happy" -> "치킨";
            case "sad" -> "짜장면";
            case "angry" -> "햄버거";
            default -> "추천 가능한 메뉴가 없습니다";
        };
    }

    public List<String> recommendByPriceRange(int min, int max) {
        List<String> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : menuPrices.entrySet()) {
            String menu = entry.getKey();
            int price = entry.getValue();
            if (price >= min && price <= max) {
                result.add(menu + " (" + price + "원)");
            }
        }

        return result;
    }

    public String recommendCombination() {
        return randomMenu() + "+" + randomMenu();
    }
}
