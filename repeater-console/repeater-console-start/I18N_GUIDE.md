# JVM-Sandbox Repeater Console 国际化使用指南

## 概述

本项目已经实现了完整的国际化（i18n）支持，包括中文和英文两种语言。国际化功能覆盖了后端Velocity模板和前端JavaScript代码。

## 测试国际化功能

1. 启动应用后，访问测试页面：
   ```
   http://localhost:8001/test/i18n.htm
   ```

2. 在页面右上角可以看到语言切换器，点击可以切换中英文

3. 切换语言后页面会自动刷新，显示对应语言的内容

## 项目结构

### 1. 后端国际化相关文件

- **资源文件**
  - `/src/main/resources/i18n/messages.properties` - 默认（中文）资源文件
  - `/src/main/resources/i18n/messages_zh_CN.properties` - 中文资源文件
  - `/src/main/resources/i18n/messages_en_US.properties` - 英文资源文件

- **Java类**
  - `I18nTool.java` - Velocity模板中使用的国际化工具类
  - `I18nConfig.java` - Spring国际化配置类
  - `GlobalControllerAdvice.java` - 全局控制器增强，注入i18n工具
  - `I18nApi.java` - 提供前端访问的国际化API

### 2. 前端国际化相关文件

- `/static/app/js/i18n.js` - 前端国际化核心模块
- `/velocity/templates/blocks/navbar.vm` - 包含语言切换器的导航栏
- `/velocity/templates/blocks/header-scripts.vm` - 通用脚本引入

## 使用方法

### 1. 在Velocity模板中使用

```velocity
## 简单文本
<h1>$i18n.get("title.online.traffic")</h1>

## 带参数的文本
<p>$i18n.get("msg.welcome", "张三")</p>

## 判断当前语言
#if($i18n.isChinese())
    中文特定内容
#else
    English specific content
#end
```

### 2. 在JavaScript中使用

```javascript
// 获取简单文本
var title = i18n.get("title.online.traffic");

// 获取带参数的文本
var msg = i18n.get("msg.welcome", "张三");

// 切换语言
i18n.switchLocale('en_US');

// 判断当前语言
if (i18n.isChinese()) {
    // 中文特定逻辑
}
```

### 3. 在Java代码中使用

```java
@Autowired
private MessageSource messageSource;

// 获取国际化消息
String message = messageSource.getMessage("button.query", null, LocaleContextHolder.getLocale());

// 带参数的消息
String welcome = messageSource.getMessage("msg.welcome", new Object[]{"张三"}, LocaleContextHolder.getLocale());
```

## 添加新的国际化文本

1. 在所有资源文件中添加相同的key：

   `/i18n/messages.properties`:
   ```properties
   new.feature.title=新功能
   ```

   `/i18n/messages_en_US.properties`:
   ```properties
   new.feature.title=New Feature
   ```

2. 在模板中使用：
   ```velocity
   $i18n.get("new.feature.title")
   ```

3. 在JavaScript中使用前，需要将新的key添加到`I18nApi.java`的`loadMessages`方法中的keys数组。

## 国际化最佳实践

1. **Key命名规范**
   - 使用点分层级结构：`模块.类型.名称`
   - 例如：`nav.online.traffic`、`button.query`、`msg.success`

2. **资源文件维护**
   - 保持所有语言资源文件的key同步
   - 定期检查是否有遗漏的翻译

3. **参数化消息**
   - 使用占位符而不是字符串拼接
   - 例如：`msg.welcome=欢迎 {0} 访问系统`

4. **前端优化**
   - 常用的国际化文本在页面加载时一次性获取
   - 避免频繁的Ajax请求

## 需要继续完成的工作

1. **完成所有页面的国际化**
   - `/velocity/templates/config/*.vm` - 配置管理相关页面
   - `/velocity/templates/module/*.vm` - 模块管理相关页面
   - `/velocity/templates/online/*.vm` - 在线流量相关页面
   - `/velocity/templates/replay/*.vm` - 回放相关页面

2. **后端响应消息国际化**
   - Controller返回的消息
   - 异常信息
   - 业务提示信息

3. **扩展语言支持**
   - 如需支持更多语言，创建对应的资源文件
   - 在`I18nApi.java`的`getSupportedLocales`方法中添加新语言

## 注意事项

1. 用户的语言偏好保存在Cookie中，有效期30天
2. 可以通过URL参数`?lang=en_US`临时切换语言
3. 如果找不到对应的国际化文本，系统会返回key本身作为默认值
4. 确保所有资源文件使用UTF-8编码

## 示例页面

已完成国际化的示例页面：
- `/templates/index.vm` - 首页
- `/templates/test/i18n-test.vm` - 国际化测试页面

这些页面可以作为其他页面国际化改造的参考。
