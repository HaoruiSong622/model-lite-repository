package com.huawei.modellite.repository.infrastructure.config;

import com.huawei.modellite.repository.common.enums.*;
import com.huawei.modellite.repository.infrastructure.persistence.typehandler.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan("com.huawei.modellite.repository.infrastructure.persistence.mapper")
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        sessionFactory.setConfiguration(configuration);

        sessionFactory.setTypeHandlers(new TypeHandler[]{
            new UUIDTypeHandler(),
            new VersionStatusTypeHandler(),
            new TaskStatusTypeHandler(),
            new LockTypeTypeHandler(),
            new SourceTypeTypeHandler(),
            new TagTypeTypeHandler(),
            new SourcePathTypeTypeHandler()
        });

        sessionFactory.setMapperLocations(
            new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/*.xml")
        );

        return sessionFactory.getObject();
    }
}