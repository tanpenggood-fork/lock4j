/*
 *  Copyright (c) 2018-2022, baomidou (63976799@qq.com).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.baomidou.lock.test.service;

import com.baomidou.lock.DefaultLockFailureStrategy;
import com.baomidou.lock.DefaultLockKeyBuilder;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.LocalLock;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.annotation.RedissonLock;
import com.baomidou.lock.executor.RedisTemplateLockExecutor;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.baomidou.lock.test.custom.CustomLockFailureStrategy;
import com.baomidou.lock.test.custom.CustomLockFailureStrategy2;
import com.baomidou.lock.test.custom.CustomLockKeyBuilder2;
import com.baomidou.lock.test.model.User;
import com.baomidou.lock.test.model.UserB;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    LockTemplate lockTemplate;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    public UserB userB;

    private int counter = 1;


    @LocalLock(keys = "1", expire = 500L)
    @Override
    public void localLock1(long blockTimeMillis) {
        System.out.println("执行本地锁方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        LockSupport.parkNanos(blockTimeMillis * 1000000);
    }

    @Lock4j(
        condition = "#id % 2 == 0",
        keys = "#id + '0'", keyBuilderStrategy = DefaultLockKeyBuilder.class,
        failStrategy = DefaultLockFailureStrategy.class,

        order = 0
    )
    @Lock4j(
        condition = "#id % 4 == 0",
        keys = "#id + '1'", keyBuilderStrategy = DefaultLockKeyBuilder.class,
        failStrategy = DefaultLockFailureStrategy.class,
        order = 1
    )
    @Lock4j(
        condition = "#id % 6 == 0",
        keys = "#id + '2'", keyBuilderStrategy = DefaultLockKeyBuilder.class,
        failStrategy = DefaultLockFailureStrategy.class,
        order = 2
    )
    @Override
    public void multiCondition1(Integer id) {
        System.out.println("执行multiCondition1方法 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
    }

    @Lock4j(
        condition = "#id > 0",
        keys = "#id", keyBuilderStrategy = DefaultLockKeyBuilder.class,
        failStrategy = DefaultLockFailureStrategy.class
    )
    @Override
    public void condition1(Integer id) {
        System.out.println("执行condition1方法 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
    }

    @Override
    @Lock4j(executor = RedissonLockExecutor.class)
    public void simple1() {
        System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
    }

    @Override
    @Lock4j(keys = "#myKey")
    public void simple2(String myKey) {
        System.out.println("执行简单方法2 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));

    }

    @Override
    @Lock4j(keys = "#user.id", acquireTimeout = 15000, expire = 1000, executor = RedissonLockExecutor.class)
    public User method1(User user) {
        System.out.println("执行spel方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        //模拟锁占用
        try {
            int count = 0;
            do {
                Thread.sleep(1000);
                System.out.println("执行spel方法1 , 当前线程:" + Thread.currentThread().getName() + " , 休眠秒：" + (count++));
            } while (count < 5);
//            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    @RedissonLock(keys = {"#user.id", "#user.name"}, acquireTimeout = 5000, expire = 5000)
    public User method2(User user) {
        System.out.println("执行spel方法2 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        //模拟锁占用
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    @Lock4j(keys = {"userB.id", "userB.name"}, acquireTimeout = 5000, expire = 5000)
    public void method3() {
        System.out.println("执行spel方法3 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        //模拟锁占用
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void programmaticLock(String userId) {

        // 各种查询操作 不上锁
        // ...
        // 获取锁
        final LockInfo lockInfo = lockTemplate.lock(userId, 30000L, 5000L, RedissonLockExecutor.class);
        if (null == lockInfo) {
            throw new RuntimeException("业务处理中,请稍后再试");
        }
        // 获取锁成功，处理业务
        try {
            System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        } finally {
            //释放锁
            lockTemplate.releaseLock(lockInfo);
        }
        //结束
    }


    @Override
    @Lock4j(keys = "1", expire = 60000)
    public void reentrantMethod1() {
        System.out.println("reentrantMethod1" + getClass());
        counter++;
    }

    @Override
    @Lock4j(keys = "1")
    public void reentrantMethod2() {
        System.out.println("reentrantMethod2" + getClass());
        counter++;
    }

    @Override
    @Lock4j(acquireTimeout = 0,expire = 5000, autoRelease = false)
    public void nonAutoReleaseLock() {
        System.out.println("执行nonAutoReleaseLock方法 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
    }

    @Override
    @Lock4j(keys ="1",expire = -1,executor = RedisTemplateLockExecutor.class)
    public void renewExpirationTemplate() {
        System.out.println("执行renewExpirationTemplate方法 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        Long expire = stringRedisTemplate.getExpire("lock4j:com.baomidou.lock.test.service.UserServiceImplrenewExpirationTemplate#1", TimeUnit.MILLISECONDS);
        System.out.println("获取锁后起始时间:"+expire);
        try {
            //超过默认过期时间
            Thread.sleep(30000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Long newExpire = stringRedisTemplate.getExpire("lock4j:com.baomidou.lock.test.service.UserServiceImplrenewExpirationTemplate#1",TimeUnit.MILLISECONDS);
        System.out.println("处理业务逻辑后续期时间:"+newExpire);

    }

    @Override
    public void usedInInterface() {
        System.out.println("执行usedInInterface方法 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
    }

    @Override
    @Lock4j(keys ="1",expire = -1,executor = RedisTemplateLockExecutor.class,failStrategy = CustomLockFailureStrategy2.class,keyBuilderStrategy = CustomLockKeyBuilder2.class)
    public void customLockFailureStrategy() {
        System.out.println("执行customLockFailureStrategy方法 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    @Lock4j(keys ="1",expire = -1,executor = RedisTemplateLockExecutor.class)
    public void customLockFailureStrategy2() {
        System.out.println("执行customLockFailureStrategy1方法 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + (counter++));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    @Lock4j(keys = {"'pay'", "#amount"},
            expire = -1,
//            executor = RedissonLockExecutor.class,
            executor = RedisTemplateLockExecutor.class,
            failStrategy = CustomLockFailureStrategy.class,
            autoRelease = false)
    public void autoRenewAndAutoReleaseLock(BigDecimal amount) {
        log.info("================================================");
        log.info("支付了{}元", amount.toPlainString());
        log.info("================================================");
        try {
            TimeUnit.SECONDS.sleep(40);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}