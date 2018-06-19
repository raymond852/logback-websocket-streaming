logback-websocket-streaming
===========================
A logback appender which starts up a websocket server for log streaming
### Usage
Add dependency to your build gradle
```
repositories {
  maven {
    url  "https://dl.bintray.com/raymond852/logback-websocket-streaming"
  }
}

dependencies {
    compile group: 'org.logback-websocket-streaming', name:"logback-websocket-streaming", version: '1.0.0'
}
```

logback.xml
```xml
<configuration>
    <appender name="WEBSOCKET" class="org.logback.plugin.websocketstreaming.WebSocketServerAppender">
        <port>7777</port>
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>%d [%thread] %level %logger - %m%n</Pattern>
        </layout>
    </appender>
    <root level="DEBUG">
        <appender-ref ref="WEBSOCKET" />
    </root>
</configuration>    
```
open browser and navigate to `http://targethost:7777/`
