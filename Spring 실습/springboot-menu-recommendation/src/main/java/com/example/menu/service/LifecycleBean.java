import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class LifecycleBean {
    
    public LifecycleBean() {
        System.out.println("LifecycleBean 생성자 호출");
    }

    @PostConstruct
    public void init() {
        System.out.println("LifecycleBean 초기화 메서드 호출");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("LifecycleBean 소멸 메서드 호출");
    }
}
