package com.example.demo;

import java.nio.charset.StandardCharsets;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * @see https://logback.qos.ch/manual/appenders.html#WriteYourOwnAppender
 * @see https://logback.qos.ch/manual/appenders_ja.html#WriteYourOwnAppender
 * @see http://oct.im/how-to-create-logback-loggers-dynamicallypragmatically.html
 */
public class LogbackSwingTextareaAppender extends AppenderBase<ILoggingEvent> {

    private final JTextArea textarea;
    private PatternLayoutEncoder encoder;

    public LogbackSwingTextareaAppender(final JTextArea ta) {
        this.textarea = ta;
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        byte[] logdata = this.encoder.encode(eventObject);
        final String logline = new String(logdata, StandardCharsets.UTF_8) + "\n";
        SwingUtilities.invokeLater(() -> {
            /* 原因がはっきりわからないが、SpringBoot の Controller 内から出力したログについては、
             * なぜか↓の append() メソッドが画面に反映されない。
             * 特に例外が出ているわけでもない。
             * 
             * GUIのコードからの出力も、SpringBootの初回起動直前は反映されるが、
             * 一度起動したあとは反映されない。
             * 
             * 可能性として考えられるのは、SpringBoot の Controller 内からだとclass loaderが
             * 専用のものになるので、その影響。
             * あと、SpringBoot の起動中にlogbackのコンテキストで何か破壊的な変更が発生している可能性。
             * 
             * 謎。
             * 今回は諦める。
             */
            textarea.append(logline);
        });
    }

    public static void addToRootLogger(final JTextArea textarea) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{ISO8601} [%thread] %marker %level %logger - %msg%n");
        encoder.start();
        LogbackSwingTextareaAppender newAppender = new LogbackSwingTextareaAppender(textarea);
        newAppender.setContext(loggerContext);
        newAppender.setEncoder(encoder);
        newAppender.start();
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(newAppender);
    }
}
