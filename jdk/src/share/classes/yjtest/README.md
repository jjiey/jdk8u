## How to run yjtest/HelloWord.java
Preferences -> Build, Execution, Deployment -> Compiler -> Excludes
排除到只剩需要的源码，比如只剩 jdk8u/jdk/src/share/classes/lang 和 jdk8u/jdk/src/share/classes/util
再排除掉 jdk8u/jdk/src/share/classes/util/jar
发现只有 java.lang.System 报错了，放开 StaticProperty 即可
运行 HelloWord