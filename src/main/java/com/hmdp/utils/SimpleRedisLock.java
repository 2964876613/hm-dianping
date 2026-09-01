package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SimpleRedisLock implements ILock {

    private final String name;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程表示
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }


    @Override
    public void unlock() {
        // 调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId());
    }

//    @Override
//    public void unlock() {
//        String key = KEY_PREFIX + name;
//        String expectedVal = ID_PREFIX + Thread.currentThread().getId();
//
//        // 1. 打印看看当前 Redis 里的真实值
//        String actualVal = stringRedisTemplate.opsForValue().get(key);
//        System.out.println("【Debug 解锁】Key: " + key);
//        System.out.println("【Debug 解锁】Redis中的值: " + actualVal);
//        System.out.println("【Debug 解锁】准备传递的ARGV[1]: " + expectedVal);
//        System.out.println("【Debug 解锁】两者是否相等: " + expectedVal.equals(actualVal));
//
//        // 调用lua脚本
//        Long result = stringRedisTemplate.execute(
//                UNLOCK_SCRIPT,
//                Collections.singletonList(key),
//                expectedVal);
//
//        System.out.println("【Debug 解锁】Lua执行返回结果: " + result); // 返回 1 表示删除了，返回 0 表示未删除
//    }

//    @Override
//    public void unlock() {
//        // 获取线程表示
//        String threadId = ID_PREFIX + Thread.currentThread().getId();
//        // 获取锁中的标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//
//        // 判断标识是否一致
//        if (threadId.equals(id)) {
//            // 释放锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//
//    }
}
