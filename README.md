# springboot-swingui-give-up
~~SwingUIからSpringBoot webapp の起動・停止を制御しようとしたがいくつかのトラブルで一部ギブアップ。~~

-> SwingUIからSpringBoot webappの起動・停止をするデモ。

こだわりポイント：
- SwingUI上から SpringBoot webapp の起動/停止を制御できる。
- `application.properties` で `server.port=0` を指定することで、listening port をOS任せのランダム値にする。
  - SwingUI 上から、どのportになったか受け取り、`Desktop.browse(URI)` でデスクトップのデフォルトブラウザで開くことができる。
  - つまり、ユーザがログなどを目視で確認してポート番号を識別し、自分でURLをブラウザに打ち込む手間をゼロにしたかった。(けど結局できてない)
- SpringBootのログを、SwingUI上で確認できる。

以下の技術要素に分けて挑戦した。
1. SwingUI 上から SpringBoot webapp の起動/停止を制御する。
2. SpringBoot webapp で Servlet Container 起動後に、どのポートでlistenを開始したか取得し、SwingUI側に伝える。
   - ~~一部ギブアップ~~ -> 後述の通り、成功した。
3. logback の root logger にSwingUIの `JTextArea` にログを追記する appender を追加し、SpringBoot のログをSwingUI側に流し込む。
   - ~~一部ギブアップ~~ -> 後述の通り、成功した。

以下、詳細。

## SwingUI 上から SpringBoot webapp の起動/停止を制御(Success)

これは SpringBoot のドキュメントを読んだり `SpringApplication` の javadoc を読む + 適当にググって実現できた。

```
@SpringBootApplication
public class MainFrame extends JFrame {
    private ConfigurableApplicationContext springAppContext;
    // (...)
    public MainFrame(final String[] args) {
        // (...)
        menuItemStartSpringBootWebapp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // SpringBoot を起動
                springAppContext = SpringApplication.run(MainFrame.class, args);
            }
        });
        // (...)
        menuItemStopSpringBootWebapp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // ApplicationContext の close() = SpringBoot app の停止
                springAppContext.close();
            }
        });

```

## SpringBoot 起動時に listen http port を SwingUI側に伝える(Give-Up -> Success!!)

最初はうまく動かず断念しましたが、ちょっとしたきっかけで原因が分かり、なんとか対処し成功しました。

### うまく動かず断念したときのログ

以下を参考にしてみたが・・・最終的にうまく動かず、断念した。

- 77.5 Discover the HTTP Port at Runtime
  - https://docs.spring.io/spring-boot/docs/2.1.2.RELEASE/reference/htmlsingle/#howto-discover-the-http-port-at-runtime
- Spring Boot - How to get the running port - Stack Overflow
  - https://stackoverflow.com/questions/30312058/spring-boot-how-to-get-the-running-port

まず、SpringBootの中でhttp portを取得するのは問題なく動かせた。`ApplicationListener<ServletWebServerInitializedEvent>` を実装したBeanを登録して `onApplicationEvent(ServletWebServerInitializedEvent event)` を受けたら、`event.getApplicationContext().getWebServer()` でサーバ情報が取得でき、その中にlistening http port番号がある。よって以下のようなクラスを用意して、`@Service` でSpringBootのDI管理下に置いた。

```
@Service
public class HttpPortService implements ApplicationListener<ServletWebServerInitializedEvent> {

    private int httpPort;

    @Override
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        this.httpPort = event.getApplicationContext().getWebServer().getPort();
    }

    public int getRunningHttpPort() {
        return httpPort;
    }
}
```

これをそのまま controller クラスに `@Autowired` すれば、Springの世界の中では listening http port を取得できる。

```
@Controller
public class IndexController {

    @Autowired
    HttpPortService httpPortService;

    @GetMapping(path = "/")
    public String index(final Model model) {
        // (...)
        log.info("running http port = {}", httpPortService.getRunningHttpPort());
        return "index";
    }
}
```

ところが、これをSpringの外から取得するのが結局できなかった。

最初は singleton bean を外部からregisterしようと思ったが、よくよく考えたら servlet container 起動時点はまだSpring起動中だから、registerするためのcontextを外部から取得できない。`ConfigurableApplicationContext` を入手できるのは、同頑張っても SpringBoot 側で servlet container が起動を終えた後であり、そこで `ApplicationListener<ServletWebServerInitializedEvent>` に inject するための singleton bean を外部からregisterしても時すでに遅し。
(とはいえ、Spring管理外のインスタンスを外部からbean登録するなら、インスタンスのライフサイクルが異なる都合でsingleton beanとして登録する手法があることを学べたのは無駄では無かった)

そこで自分の中では禁じ手だが、クラスの static フィールドを経由して SpringBoot の中から外へ、port numberを渡そうとした。
まず以下のようなlistenerを作る。

```
public class HttpPortInitializedListener {
    private int httpPort;
    public void setHttpPort(final int v) {
        this.httpPort = v;
    }
    public int getHttpPort() {
        return httpPort;
    }
}
```

これを、SpringBootからもSwingUIからも可視なクラスのstaticフィールドにset/getできるようにしておく。

```
public class StaticGlobalRefs {
    private static HttpPortInitializedListener httpPortInitializedListener;

    public static void setHttpPortInitializedListener(final HttpPortInitializedListener listener) {
        httpPortInitializedListener = listener;
    }

    public static HttpPortInitializedListener getHttpPortInitializedListener() {
        return httpPortInitializedListener;
    }
}
```

SwingUI側では、予め `HttpPortInitializedListener` のインスタンスを `StaticGlobalRefs` にセットしておく。

```
        HttpPortInitializedListener httpPortInitializedListener = new HttpPortInitializedListener();
        StaticGlobalRefs.setHttpPortInitializedListener(httpPortInitializedListener);
```

SpringBootの `ApplicationListener<ServletWebServerInitializedEvent>` 実装側で、ポート番号を取得できたら以下のように global static フィールドから 外部のlistenerに値を入れる。

```
@Service
public class HttpPortService implements ApplicationListener<ServletWebServerInitializedEvent> {
    // (...)
    @Override
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        this.httpPort = event.getApplicationContext().getWebServer().getPort();
        StaticGlobalRefs.getHttpPortInitializedListener().setHttpPort(httpPort);
    }
    // (...)
}
```

これで行けるかな・・・と思ったのだが、 **なぜか SwingUI のスレッドから参照すると、0 になってる。**
ログを挟んでみると、SpringBoot 側のログでは確かに、起動したポート番号が `HttpPortInitializedListener` まで伝わっている。

```
2019-01-29 23:08:14.885  INFO 2228 --- [WT-EventQueue-0] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 64652 (http) with context path ''
2019-01-29 23:08:14.885  INFO 2228 --- [WT-EventQueue-0] com.example.demo.HttpPortService         : application-event : running http port = 64652
2019-01-29 23:08:14.895  INFO 2228 --- [WT-EventQueue-0] c.e.demo.HttpPortInitializedListener     : http-port = 64652
```

ところが、SwingUI側で取り出してみると、なぜか次のように0が返ってくる。( `Desktop.browser(URI)` を呼び出す処理の手前に入れたログ出力から)

```
2019-01-29 23:08:16.759  INFO 2228 --- [WT-EventQueue-0] c.e.demo.HttpPortInitializedListener     : http-port = 0
listener accepted http-port = 0
http-port is 0, skip opening browser.
```

**なぜじゃぁあああああ！！！！！！！！！！！！！！**

可能性として考えられるのは、SpringBootの場合クラスローダが特殊な上に、クラスローダの階層がDIのために独立している可能性が高い。よって、Swingの世界(= ClassLoader) から見えているインスタンスと、SpringBoot のDIの中から見えているインスタンスが違うのではないか？いやでも、SpringBootの中から `StaticGlobalRefs` にセットした参照は取れてて、nullでもない。

なんで？これ、マジでなんで？
おそらくあまりにもエッジケースと思われるため、調べようも無い。
よって、今回はJVMの内部だけでこれを完結させるのを諦めた。

### ちょっとしたきっかけから原因解明

上記で頭を抱えてから二日後、「JVMの内部が駄目なら、外部ファイル経由なら行けるだろう」と、`Files.createTempFile()` で一時ファイルを生成する処理を `MainFrame` のコンストラクタに埋め込みました。また `HttpPortInitializedListener` にそのPathを渡すようにして、setter/getter で試しに Path の toString() 、つまりファイル名をログ出力してみました。

・・・が・・・なぜか、`MainFrame` の中で生成したファイル名と、`HttpPortInitializedListener` の setter/getter が呼び出されたタイミングで出力されるファイル名が違う・・・。

実際、SwingUIからSpringBootを起動した段階で、 `Files.createTempFile()` で生成した一時ファイルが2つ存在する・・・。
ここでよーくログを見てみると、SpringBoot起動ログの中で MainFrame のコンストラクタ中で生成したファイル名のログ出力が動いていました。

**つまり、`SpringApplication.run(MainFrame.class, args);` の中でもう一度 MainFrame のコンストラクタが呼ばれていたのです。**

このためコンストラクタ中の次のコードがもう一度実行され、元の MainFrame インスタンスからは参照できない別の `HttpPortInitializedListener` が `StaticGlobalRefs` にセットされてしまっていた・・・。という次第。

```
        HttpPortInitializedListener httpPortInitializedListener = new HttpPortInitializedListener();
        StaticGlobalRefs.setHttpPortInitializedListener(httpPortInitializedListener);
```

また、おそらくですが MainFrame に `@SpringBootApplication` アノテーションを設定していたので、Beanとして自動生成されてしまったのかもしれません。

### 対処法

以下のような空のクラスを作成し、それを `SpringApplication.run()` に渡すようにしました。また `@SpringBootApplication` もこちらに移しています。

```
@SpringBootApplication
public class SpringBootEntryPoint {
}
```

これで `HttpPortInitializedListener` のインスタンスが正しく参照され、ポート番号の受け渡しに成功し、デフォルトブラウザで `http://localhost:(ポート番号)/` を開くことが可能になりました。

## logbackで独自appenderをroot loggerに追加し、SpringBootのログ取得(Give-Up -> Success!!)

最初はうまく動かず断念しましたが、ちょっとしたきっかけで原因が分かり、なんとか対処し成功しました。

### うまく動かず断念したときのログ
logbackで独自のappenderを作成し、loggerに追加すること自体は(パフォーマンスやスレッドセーフティを考えなければ)難しくない。以下の資料を参考にした。

- https://logback.qos.ch/manual/appenders.html#WriteYourOwnAppender
- https://logback.qos.ch/manual/appenders_ja.html#WriteYourOwnAppender
- http://oct.im/how-to-create-logback-loggers-dynamicallypragmatically.html

`JTextArea` のインスタンスを受け取り、そこにログを追記していくappenderを次のように作ってみた。

```
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
            textarea.append(logline);
        });
    }

    // root loggerに追加するためのショートカットユーティリティメソッド。
    public static void addToRootLogger(final JTextArea textarea) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        // pattern はとりあえずハードコードで決め打ち。
        encoder.setPattern("%d{ISO8601} [%thread] %marker %level %logger - %msg%n");
        encoder.start();
        // appender インスタンスを生成して開始する。
        LogbackSwingTextareaAppender newAppender = new LogbackSwingTextareaAppender(textarea);
        newAppender.setContext(loggerContext);
        newAppender.setEncoder(encoder);
        newAppender.start();
        // root logger に追加
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(newAppender);
    }
}
```

このappender単体では、問題なく動くことを確認している。 **SpringBoot が起動するまでは。**
以下のように起動前後でSwingUI側でログも出してみたのだが・・・

```
        menuItemStartSpringBootWebapp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Logger log = LoggerFactory.getLogger(this.getClass());
                log.info("spring boot starting..."); // これは JTextArea に反映される。
                springAppContext = SpringApplication.run(MainFrame.class, args);
                // (...)
                log.info("spring boot started."); // これが反映されない。SpringBootの起動中に、何かlogbackに仕掛けが入るから？
            }
        });
        // (...)
        menuItemStopSpringBootWebapp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Logger log = LoggerFactory.getLogger(this.getClass());
                log.info("spring boot stopping..."); // これも反映されない。謎。
                springAppContext.close();
                // (...)
                log.info("spring boot stopped"); // これも反映されない。謎。
            }
        });
```

コメントに書いたとおり、SpringBoot起動直前の `log.info()` は JTextArea に反映される、つまりこの時点では独自appenderは動いている。
**一度 SpringBoot が起動してしまうと、なぜか JTextArea に反映されなくなる。**
一度でも、だから、一度 SpringBoot を起動 -> 停止したあともう一度起動すると、最初は JTextArea に反映された `log.info("spring boot starting...")` のログは二度と反映されなくなる。

試しに `append()` 内部で `System.err.println()` など入れてみると、そちらは正しく出力されているし、内部で JTextArea インスタンスも参照できている(NPEなどは発生していない)

**なんなの！？なに、このキツネにつままれたような謎挙動は！？**

勘弁してくれぇ・・・。

ということで、同じJVM内のクラス・インスタンス間のやり取りによる logger の書き換えは、諦めることにした。

ついでに、SpringBoot起動時のログ全てが独自appenderに流れたわけでもなく、どうも途中からとなる。これもなぜなのか、不明。

### カスタム ClassLoader の作成 -> 断念！

十中八九は、SpringBootおよびSpringのDIによるクラスローダの影響と思われる。

SpringBootではexecutable jarの生成で、Javaエコシステムではよく使われている uber-jar とは異なる、nested-jar 方式を採用している。またSpringBoot用のmaven pluginもそのあたりを考慮しているようだ。

- Spring Boot Reference Guide
  - https://docs.spring.io/spring-boot/docs/2.1.2.RELEASE/reference/htmlsingle/#executable-jar
- Spring Boot Maven Plugin – Usage
  - https://docs.spring.io/spring-boot/docs/2.1.2.RELEASE/maven-plugin/usage.html

簡単に書くと、依存jarを特定フォルダ中にまとめた上で(= nested) jarでパッケージングしている。ではどうやってそれらのjarをロードしているか？というと、そうしたnested jarを見つける特製のClassLoader兼mainランチャーをSpringBootでは提供していて、SpringBoot Maven Plugin がそれをマニフェストのMain Classに埋め込むようにしている。つまり、一見ソースコード上はプログラマの作ったmain()メソッドがあるのでそれが実行されるように見えるのだが、実際にmain()として最初に呼ばれるのは特製ClassLoaderを兼ねたランチャークラスで、それがnestedされたjarをロードできるclass loaderを使ってプログラマが作成したmain()をキックする。

よって、クラスローディングの動きをカスタマイズするには、SpringBoot Maven PluginがデフォルトでマニフェストのMain Classとして仕込む特製class loaderをカスタマイズする、という手法が考えられる。実際に試した人の記事も見つけた。

- Spring Boot Classloader and Class Overriding - DZone Java
  - https://dzone.com/articles/spring-boot-classloader-and-class-override
- Spring Boot ClassLoader and Class Override by @dawidkublik
  - http://dkublik.github.io/spring-boot-class-loader-and-class-override/

ただ、さすがにこれは大仰すぎるし、上の記事でのカスタマイズは今回の趣旨と異なるためそのまま使えるわけでもない。

そもそも SpringBoot で classloader をカスタマイズしたいという要望は、既に出ているのではないか？とぐぐってみると・・・

- Provide support for custom class loader to bridge into LaunchedURLClassLoader · Issue #1668 · spring-projects/spring-boot
  - https://github.com/spring-projects/spring-boot/issues/1668
- Fix issue #1668: Added support for custom system class loader to enable loading of various classes that are packaged inside nested jars at the system level by aantono · Pull Request #1669 · spring-projects/spring-boot
  - https://github.com/spring-projects/spring-boot/pull/1669

無慈悲にも、２つ目のIssue 1669 で以下のように「classloaderのカスタマイズはエッジケース過ぎるので、そのためだけにカスタマイズ可能なclass loaderをメンテナンスすることはできない」と最終的に却下されてしまった・・・。

> We've discussed this today and have decided that this is an edge case that won't be of use to the vast majority of Boot users. As such, we don't want to take on the burden of maintaining the custom class loader. 

### ちょっとした寄り道から原因解明

JVMの外でやり取りさせようとして、logbackでTCP経由のログについて調べてみたら、logbackの場合socket経由でログを送受信できる appender と receiver が用意されてる。

- https://logback.qos.ch/manual/appenders.html
- https://logback.qos.ch/manual/appenders_ja.html
- https://logback.qos.ch/manual/receivers.html
- https://logback.qos.ch/manual/receivers_ja.html

これを試そうと思ったのだが、どうも appender と receiver で設定ファイルが完全に独立させる必要があるっぽい。一つの logback.xml 中にappenderとreceiverを同居させるのは難しそう。

そこで設定周りを見ていくうちに、context という概念が出てきて、設定のドキュメントを見ていると `context.reset();` というサンプルコードが出てきた。

・・・もしかして・・・SpringBootの起動時に、独自にcontextを調整してて、その前にroot logger に追加したappenderになにか影響出ているのでは・・・？？

そこで springboot logback context initialize みたいなキーワードでググってみたところ、以下が見つかった。

- logback - Spring is resetting my logging configuration - how do I work around this? - Stack Overflow
  - https://stackoverflow.com/questions/28419024/spring-is-resetting-my-logging-configuration-how-do-i-work-around-this
- Logback configuration is reinitialized for each context created in the application · Issue #6688 · spring-projects/spring-boot
  - https://github.com/spring-projects/spring-boot/issues/6688

1つ目はまさしく、SpringBoot起動時にlogback設定がリセットされるというIssueだが、これは 1.4 など大分昔の話なので、関係はなさそう。

どんぴしゃりなのが2つ目の記事で、回答としてもそのものズバリな解法が示されている。
解法としては、SpringBoot起動の途中でlogbackの設定がいじられるなら、それが終わったあたりのeventを捉えて、もう一度再設定するというアプローチ。
それで実際にやってみたところ、うまく動いてくれた。

まず以下のように `ApplicationContextInitializedEvent` イベントを捉えてroot logger に独自appenderを追加し直すlistenerクラスを用意する。

```
public class CustomLoggingConfigurationApplicationListener implements SmartApplicationListener {
    private final JTextArea taLog;
    public CustomLoggingConfigurationApplicationListener(final JTextArea taLog) {
        this.taLog = taLog;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        LogbackSwingTextareaAppender.addToRootLogger(taLog);
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return ApplicationContextInitializedEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return true;
    }
}
```

stackoverflowの記事では、`getOrder()` も override して順序を調整していたり `ApplicationEnvironmentPreparedEvent` を捉える書き方だったが、実際に試したところ `getOrder()` の override は不要。
また `ApplicationEnvironmentPreparedEvent` ではタイミングが早すぎたらしく動作せず、`ApplicationContextInitializedEvent` の方がちゃんと動いてくれた。

これを以下のように SpringApplication 起動時にlistenerとして登録する。

```
springAppContext = new SpringApplicationBuilder(SpringBootEntryPoint.class)
    .listeners(new CustomLoggingConfigurationApplicationListener(taLog)).run(args);
```

これでようやく、SpringBootのログを JTextArea に流し込むことが可能となった。(ブート時ロゴや、本当に初期のログは取れないけど、本格的にweb serverが起動してからのログは問題なく取れる)

## 一旦の結論

~~以上より、同じJVM内でクラスのstaticフィールドやインスタンスを通じて、SpringBootの内部と外部で情報をやり取りするのは非常に難易度が高いことが判明した。~~ -> ポート番号についてはJVM内でやり取り成功。残るは root logger の問題。

~~では望みが無いかというと、そうでもない。~~

~~JVM内がclass loader の関係で駄目なら、JVMのさらに外部を中継させれば良い。~~

~~例えばhttp port番号なら、それを特定の場所の一時ファイルに書き込んで、それを読み込むのはうまくいきそうだ。~~ -> そこまでしなくても成功した。

~~独自appenderなら、例えばネットワークを経由すればどうだろう？ SwingUI側で簡易なテキストベースのserverを起動し、appenderはそれに接続してログ文字列を送り込む。これなら class loader の影響は受けずにうまくいきそうではないか？~~

~~というのを、また挑戦してみたい。~~

## 次のターゲット

launch4j で jre をバンドルしてパッケージングする。
