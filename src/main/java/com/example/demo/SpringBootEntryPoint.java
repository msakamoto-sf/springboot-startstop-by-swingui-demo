package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <code>SpringBootApplication</code>アノテーションを付けたクラスを
 * {@link SpringApplication#run(Class, String...)} の第一引数に渡すと
 * そのクラスのインスタンスが作成される。
 * {@link MainFrame} を渡してしまうと内部的に JFrame が作成されてしまう。
 * そこで空のダミークラスを用意してそこに <code>SpringBootApplication</code>アノテーションを付与する。
 */
@SpringBootApplication
public class SpringBootEntryPoint {
}
