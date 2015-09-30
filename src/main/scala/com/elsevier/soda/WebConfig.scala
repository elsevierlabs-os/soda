package com.elsevier.soda

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Bean

import org.springframework.web.servlet.view.InternalResourceViewResolver
import org.springframework.web.servlet.view.JstlView

@ComponentScan(basePackages=Array("com.elsevier.soda"))
class WebConfig {

    @Bean
    def viewResolver = {
        val viewResolver = new InternalResourceViewResolver()
        viewResolver.setViewClass(classOf[JstlView])
        viewResolver.setPrefix("/WEB-INF/views/")
        viewResolver.setSuffix(".jsp")
        viewResolver
    }
}
