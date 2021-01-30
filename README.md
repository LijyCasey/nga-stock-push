# nga-stock-push
用于推送nga大时代区各个专楼的著名大佬的消息  


开启多个异步线程10秒钟扫描一次对应的楼层，扫描对应的每层消息。如果是大佬发的，直接post到钉钉的机器人中。  

1.0.6版本重新更改了线程生成逻辑，并从Properties文件改为解析yml文件  

现在推送的专帖如下：

-[牛神专楼](https://bbs.nga.cn/read.php?tid=24913158)

-[禾戈专楼](https://bbs.nga.cn/read.php?tid=24900465)

-[白博士专楼](https://bbs.nga.cn/read.php?tid=24906978)

-[2021主楼](https://bbs.nga.cn/read.php?tid=24929430)

-[乔帮主主楼](https://bbs.nga.cn/read.php?tid=24929177)

要新增修改帖子也可以在application.yml中进行修改

作者名单现在也在application.yml中进行配置

钉钉推送机器人直接使用钉钉的webhook机器人api:[API](https://developers.dingtalk.com/document/app/custom-robot-access)

也欢迎加入全禁言的**钉钉群：33102830** 接收推送消息

# 此爬虫及钉钉群，仅做学习交流用。切勿盲信他人言论，更不要盲信这些大师大神

有技术相关的问题可以直接发issue，股票帖相关的问题nga直接发私信给 casey_li。
