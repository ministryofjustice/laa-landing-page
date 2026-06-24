package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class LiquibaseSessionOrderingConfig {

    //@Bean
    public static BeanFactoryPostProcessor forceLiquibaseBeforeSession() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("sessionRepository")) {
                BeanDefinition sessionRepoDef = beanFactory.getBeanDefinition("sessionRepository");
                sessionRepoDef.setDependsOn("liquibase");
            }
        };
    }
}
