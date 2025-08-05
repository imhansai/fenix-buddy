# fenix-buddy
IntelliJ IDEA Fenix 插件

## 获取

[fenix-buddy](https://plugins.jetbrains.com/plugin/24357-fenix-buddy)

## 起源
项目中用到了 [fenix](https://github.com/blinkfox/fenix) 库，但是由于没有插件，导致 Java/kotlin 和 xml 之间的跳转比较麻烦，
需要手动去点击、去查找。不过后来有个老哥写了一款[插件](https://plugins.jetbrains.com/plugin/17158-fenix)，虽然有些问题，但是起码是能用的，不过后来升级了几次版本，插件基本上不生效了。
所以就萌生了自己写一个的念头，开干！

## 功能
由于第一次接触写 IntelliJ IDEA 插件，官方文档也写的让人摸不着头脑，不过通过摸索，基础核心的相互跳转是OK了

- Java/Kotlin 和 XML 之间相互跳转

## 一些想法
1. 能否将 Java/Kotlin 和 xml 元素关联，这样重构的时候会方便，或许也能代码提示，或许可以直接点击元素相互跳转而不是行标记
2. 根据写的Java/kotlin方法生成xml代码，反之亦然
3. xml 中的代码提示，虽然可以注入 sql 语言，但是对于 fenix 的自定义标签可能要做些处理

## 致谢

- [fenix](https://github.com/blinkfox/fenix)
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [fenix-plugin](https://github.com/jgaybjone/fenix-plugin)

## 许可
[GPL-3.0](LICENSE)
