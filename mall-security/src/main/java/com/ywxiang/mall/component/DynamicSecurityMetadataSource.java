package com.ywxiang.mall.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 动态权限数据源，用于获取动态权限规则
 * 通过当前的请求地址，获取该地址需要的用户角色
 *
 * @author ywxiang
 * @date 2020/8/11 下午8:11
 */
public class DynamicSecurityMetadataSource implements FilterInvocationSecurityMetadataSource {

    private static Map<String, ConfigAttribute> configAttributeMap = null;

    @Autowired
    private DynamicSecurityService dynamicSecurityService;

    @PostConstruct
    public void loadDataSource() {
        configAttributeMap = dynamicSecurityService.loadDataSource();
    }

    public void clearDataSource() {
        configAttributeMap.clear();
        configAttributeMap = null;
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        if (configAttributeMap == null) {
            this.loadDataSource();
        }
        List<ConfigAttribute> configAttributes = new ArrayList<>();
        // 获取当前访问路径
        String url = ((FilterInvocation) object).getRequestUrl();
        PathMatcher pathMatcher = new AntPathMatcher();
        // 获取访问路径所需资源
        for (String pattern : configAttributeMap.keySet()) {
            if (pathMatcher.match(pattern, url)) {
                configAttributes.add(configAttributeMap.get(pattern));
            }
        }
        // 未设置操作请求权限，返回空集合
        return configAttributes;
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        return null;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }
}
