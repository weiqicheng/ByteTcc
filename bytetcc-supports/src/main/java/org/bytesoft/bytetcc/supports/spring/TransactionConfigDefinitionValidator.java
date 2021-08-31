/**
 * Copyright 2014-2016 yangming.liu<bytefox@126.com>.
 * <p>
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytetcc.supports.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

/**
 * 校验事务的配置，只支持注解事务
 */
public class TransactionConfigDefinitionValidator implements BeanFactoryPostProcessor {

    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 获取定义的Bean的名称
        String[] beanNameArray = beanFactory.getBeanDefinitionNames();
        for (int i = 0; i < beanNameArray.length; i++) {
            String beanName = beanNameArray[i];
            BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
            String beanClassName = beanDef.getBeanClassName();

            // 不支持声明式事务
            if (org.springframework.transaction.interceptor.TransactionProxyFactoryBean.class.getName().equals(beanClassName)) {
                throw new FatalBeanException(String.format(
                        "Declaring transactions by configuration is not supported yet, please use annotations to declare transactions(beanId= %s).",
                        beanName));
            }

            if (org.springframework.transaction.interceptor.TransactionInterceptor.class.getName().equals(beanClassName)) {
                boolean errorExists = true;

                MutablePropertyValues mpv = beanDef.getPropertyValues();
                PropertyValue pv = mpv.getPropertyValue("transactionAttributeSource");
                Object value = pv == null ? null : pv.getValue();
                if (value != null && RuntimeBeanReference.class.isInstance(value)) {
                    RuntimeBeanReference reference = (RuntimeBeanReference) value;
                    BeanDefinition refBeanDef = beanFactory.getBeanDefinition(reference.getBeanName());
                    String refBeanClassName = refBeanDef.getBeanClassName();
                    errorExists = AnnotationTransactionAttributeSource.class.getName().equals(refBeanClassName) == false;
                }

                if (errorExists) {
                    throw new FatalBeanException(String.format(
                            "Declaring transactions by configuration is not supported yet, please use annotations to declare transactions(beanId= %s).",
                            beanName));
                } // end-if (errorExists)
            }

        } // end-for (int i = 0; i < beanNameArray.length; i++)
    }

}
