package kr.co.demo;

import kr.co.demo.client.jpa.adapter.AutoJpaAdapter;
import kr.co.demo.domain.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AutoJpaAdapter Bean 조회 테스트
 */
@SpringBootTest
class AutoJpaAdapterTest {

    @Autowired
    ApplicationContext context;

    @Test
    void printAllBeanNames() {
        System.out.println("========== All Bean Names ==========");
        String[] beanNames = context.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String name : beanNames) {
            System.out.println(name);
        }
    }

    @Test
    void printOrderRelatedBeans() {
        System.out.println("========== Order Related Beans ==========");
        String[] beanNames = context.getBeanDefinitionNames();
        Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("order"))
                .forEach(name -> {
                    Object bean = context.getBean(name);
                    System.out.println(name + " → " + bean.getClass().getSimpleName());
                });
    }

    @Test
    void testExpectedBeanNames() {
        // Repository Bean 이름 확인
        System.out.println("========== Expected Bean Names ==========");
        
        String[] expectedNames = {
                "orderEntityRepository",
                "orderStorageMapper",
                "orderItemEntityRepository",
                "orderItemStorageMapper"
        };

        for (String expectedName : expectedNames) {
            boolean exists = context.containsBean(expectedName);
            System.out.println(expectedName + " → " + (exists ? "✅ EXISTS" : "❌ NOT FOUND"));
        }
    }

    @Test
    void testAutoJpaAdapter() {
        // AutoJpaAdapter가 Bean을 찾을 수 있는지 테스트
        System.out.println("========== AutoJpaAdapter Test ==========");

        try {
            TestOrderAdapter adapter = new TestOrderAdapter(context);
            System.out.println("✅ AutoJpaAdapter 생성 성공!");
            
            // count 호출 테스트
            long count = adapter.countOrders();
            System.out.println("✅ count() 호출 성공: " + count);
            
        } catch (Exception e) {
            System.out.println("❌ AutoJpaAdapter 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 테스트용 Adapter
     */
    static class TestOrderAdapter extends AutoJpaAdapter<Order, Long> {

        public TestOrderAdapter(ApplicationContext context) {
            super(Order.class, context);
        }

        public long countOrders() {
            return count();
        }
    }
}
