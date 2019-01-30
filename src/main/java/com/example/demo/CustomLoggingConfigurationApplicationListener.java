package com.example.demo;

import javax.swing.JTextArea;

import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;

/**
 * SpringBoot の起動中に Logger 設定を上書きするらしい（？正確なところはコードまで追ってないので不明）。
 * @see https://github.com/spring-projects/spring-boot/issues/6688
 * 
 * 以下の記事を参考に、{@link ApplicationContextInitializedEvent} を捕まえてそこで
 * root logger を追加し直すlistenerを登録してみたら、ちゃんとroot logger に独自appenderが追加して動くようになった。
 * @see https://stackoverflow.com/questions/28419024/spring-is-resetting-my-logging-configuration-how-do-i-work-around-this
 */
public class CustomLoggingConfigurationApplicationListener implements SmartApplicationListener {

    private final JTextArea taLog;

    public CustomLoggingConfigurationApplicationListener(final JTextArea taLog) {
        this.taLog = taLog;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.err.println("#### customize logger in spring application ####");
        LogbackSwingTextareaAppender.addToRootLogger(taLog);
    }

    //これは不要だった。
    //@Override
    //public int getOrder() {
    //    return Ordered.HIGHEST_PRECEDENCE + 12;
    //}

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        //System.err.println(eventType);
        //return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(eventType);
        // 実際に動かしてみたら、↑のイベントでは早すぎるせいか動かず。↓のイベントがちょうど良かったっぽい。
        return ApplicationContextInitializedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }
}