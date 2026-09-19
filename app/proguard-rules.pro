# 保留行号，release 崩溃栈才能定位到源码位置；文件名统一混淆为 SourceFile 以免泄露结构。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
