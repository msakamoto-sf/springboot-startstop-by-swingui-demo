package com.example.demo;

/**
 * @see https://stackoverflow.com/questions/4540713/add-bean-programmatically-to-spring-web-app-context
 * 上記をヒントに、singletonのbeanを登録すれば・・・と思ったのだが、
 * そもそも <code>ConfigurableApplicationContext</code> を取得できるのは <code>SpringApplication.run()</code>
 * の戻り値なので、その時点ですでにSpringApplicationは起動が終わっている = http portの初期化も完了している。
 * その後で listener を regsiter したとしても、ナンセンス・・・。
 * 
 * ということで、泣く泣くグローバル変数の参照をブリッジするクラスを用意して、そこを経由させることにした。
 */
public class StaticGlobalRefs {
    private static HttpPortInitializedListener httpPortInitializedListener;

    public static void setHttpPortInitializedListener(final HttpPortInitializedListener listener) {
        httpPortInitializedListener = listener;
    }

    public static HttpPortInitializedListener getHttpPortInitializedListener() {
        return httpPortInitializedListener;
    }
}
