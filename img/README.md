# CoverDroid-IMG 说明
本文件夹下提供的CoverDroid镜像使用分卷的方式进行打包，在本文件夹下使用如下命令解压：
```
cat CoverDroid-IMG.tar* | tar xzvf -
```
已编译好的system.img在./CoverDroid-IMG/img/x86/目录下。

可自行使用Genimotion等性能好的模拟器进行安装试用，也可使用如下代码进行快速试用：
```
sh start.sh
```
快速试用时，包括如下步骤：
1. 执行sh start.sh，并等待系统启动完毕
2. 安装iashelper.apk，使用命令adb install iashelper.apk
3. 可使用adb forward tcp:6161 tcp:6161，并将当前主机ip作为手机ip
4. 打开模拟器中安装好的app【猿帮】
5. 参照网页版使用手册，可获取代码覆盖测试报告。
