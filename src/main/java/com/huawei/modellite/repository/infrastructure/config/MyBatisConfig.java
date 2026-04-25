package com.huawei.modellite.repository.infrastructure.config;

import com.huawei.modellite.repository.common.enums.*;
import com.huawei.modellite.repository.infrastructure.persistence.typehandler.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        sessionFactory.setConfiguration(configuration);

        sessionFactory.setTypeHandlers(new TypeHandler[]{
            new VersionStatusTypeHandler(),
            new TaskStatusTypeHandler(),
            new LockTypeTypeHandler(),
            new SourceTypeTypeHandler(),
            new TagTypeTypeHandler()
        });

        return sessionFactory.getObject();
    }
}