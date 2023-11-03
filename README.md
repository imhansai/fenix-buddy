# fenix-buddy
IntelliJ IDEA Fenix 插件

## 获取

[fenix-buddy](https://plugins.jetbrains.com/plugin/23045-fenix-buddy)

## 起源
项目中用到了 [fenix](https://github.com/blinkfox/fenix) 库，但是由于没有插件，导致 Java/kotlin 和 xml 之间的跳转比较麻烦，
需要手动去点击、去查找。不过后来有个老哥写了一款[插件](https://plugins.jetbrains.com/plugin/17158-fenix)，虽然有些问题，但是起码是能用的，不过后来升级了几次版本，插件基本上不生效了。
所以就萌生了自己写一个的念头，开干！

## 功能
由于第一次接触写 IntelliJ IDEA 插件，官方文档也写的让人摸不着头脑，不过通过摸索，基础核心的相互跳转是OK了

- Java/Kotlin 和 XML 之间相互跳转

## 路线图
1. 相互跳转的逻辑优化一下，例如查找 domElement 的地方现在是通过 fenixId 匹配的，有些浪费时间，可以通过命名空间和 id 匹配
2. 复刻一下之前那个老哥写的插件的功能
3. 看情况，一边学习一边加

## 致谢

- [fenix](https://github.com/blinkfox/fenix)
- [IntelliJ Platform Plugin SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [fenix-plugin](https://github.com/jgaybjone/fenix-plugin)

## 许可
[GPL-3.0](LICENSE)