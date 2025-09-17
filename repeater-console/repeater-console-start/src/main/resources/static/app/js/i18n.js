/**
 * 国际化模块
 * 提供前端页面的国际化支持
 */
var i18n = {
    // 当前语言
    locale: 'zh_CN',
    
    // 消息缓存
    messages: {},
    
    // 支持的语言列表
    supportedLocales: [],
    
    // 调试模式开关（可在浏览器控制台中通过 i18n.debug = true 开启）
    debug: false,
    
    /**
     * 初始化国际化模块
     * @param locale 指定的语言，如果不指定则从Cookie中获取
     */
    init: function(locale) {
        // 设置语言
        this.locale = locale || this.getLocaleFromCookie() || 'zh_CN';
        
        // 加载消息
        this.loadMessages();
        
        // 加载支持的语言列表
        this.loadSupportedLocales();
        
        // 更新页面语言显示
        this.updateLanguageDisplay();
    },
    
    /**
     * 加载国际化消息
     */
    loadMessages: function() {
        var self = this;
        $.ajax({
            url: '/api/i18n/messages',
            type: 'GET',
            data: { locale: this.locale },
            async: false,
            success: function(data) {
                self.messages = data;
                if (self.debug) {
                    console.log('[i18n] Loaded ' + Object.keys(data).length + ' messages for locale: ' + self.locale);
                }
            },
            error: function() {
                console.error('[i18n] Failed to load i18n messages for locale: ' + self.locale);
                self.messages = {};
            }
        });
    },
    
    /**
     * 加载支持的语言列表
     */
    loadSupportedLocales: function() {
        var self = this;
        $.ajax({
            url: '/api/i18n/supported-locales',
            type: 'GET',
            async: false,
            success: function(data) {
                self.supportedLocales = data;
            },
            error: function() {
                console.error('Failed to load supported locales');
                self.supportedLocales = [
                    { locale: 'zh_CN', displayName: '中文' },
                    { locale: 'en_US', displayName: 'English' }
                ];
            }
        });
    },
    
    /**
     * 获取国际化消息
     * @param key 消息key
     * @param args 参数数组
     * @returns {string} 国际化后的消息
     */
    get: function(key) {
        var message = this.messages[key];
        
        // 调试支持：如果找不到消息键，记录警告
        if (!message) {
            if (this.debug) {
                console.warn('[i18n] Message key not found: "' + key + '" for locale: ' + this.locale);
            }
            message = key; // 使用键名作为fallback
        }
        
        // 处理参数替换
        if (arguments.length > 1) {
            for (var i = 1; i < arguments.length; i++) {
                var placeholder = '{' + (i - 1) + '}';
                message = message.replace(new RegExp(placeholder, 'g'), arguments[i]);
            }
        }
        
        return message;
    },
    
    /**
     * 切换语言
     * @param locale 目标语言
     */
    switchLocale: function(locale) {
        if (locale && locale !== this.locale) {
            this.setLocaleToCookie(locale);
            
            // 获取当前URL，移除现有的lang参数
            var url = window.location.href;
            url = url.replace(/[?&]lang=[^&]*/g, '');
            
            // 重新判断分隔符：移除lang参数后，如果URL中仍有其他参数则用&，否则用?
            var separator = url.indexOf('?') !== -1 ? '&' : '?';
            
            // 添加新的lang参数并跳转
            window.location.href = url + separator + 'lang=' + locale;
        }
    },
    
    /**
     * 从Cookie中获取语言设置
     * @returns {string|null} 语言代码
     */
    getLocaleFromCookie: function() {
        var name = 'repeater_locale=';
        var decodedCookie = decodeURIComponent(document.cookie);
        var ca = decodedCookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) === ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) === 0) {
                return c.substring(name.length, c.length);
            }
        }
        return null;
    },
    
    /**
     * 将语言设置保存到Cookie
     * @param locale 语言代码
     */
    setLocaleToCookie: function(locale) {
        var d = new Date();
        d.setTime(d.getTime() + (30 * 24 * 60 * 60 * 1000)); // 30天
        var expires = 'expires=' + d.toUTCString();
        document.cookie = 'repeater_locale=' + locale + ';' + expires + ';path=/';
    },
    
    /**
     * 更新页面上的语言显示
     */
    updateLanguageDisplay: function() {
        var currentLocale = this.locale;
        var displayName = '中文'; // 默认
        
        for (var i = 0; i < this.supportedLocales.length; i++) {
            if (this.supportedLocales[i].locale === currentLocale) {
                displayName = this.supportedLocales[i].displayName;
                break;
            }
        }
        
        $('#current-language').text(displayName);
    },
    
    /**
     * 获取当前语言
     * @returns {string} 当前语言代码
     */
    getCurrentLocale: function() {
        return this.locale;
    },
    
    /**
     * 判断是否为中文
     * @returns {boolean}
     */
    isChinese: function() {
        return this.locale.indexOf('zh') === 0;
    },
    
    /**
     * 判断是否为英文
     * @returns {boolean}
     */
    isEnglish: function() {
        return this.locale.indexOf('en') === 0;
    },
    
    /**
     * 调试功能：列出所有可用的消息键
     * 在浏览器控制台中使用：i18n.listKeys()
     */
    listKeys: function() {
        var keys = Object.keys(this.messages);
        console.log('[i18n] Available message keys (' + keys.length + ' total):');
        keys.sort().forEach(function(key) {
            console.log('  - ' + key + ': "' + this.messages[key] + '"', '');
        }.bind(this));
        return keys;
    },
    
    /**
     * 调试功能：搜索包含指定文本的消息键
     * @param {string} searchText 搜索文本
     */
    searchKeys: function(searchText) {
        if (!searchText) {
            console.warn('[i18n] Please provide search text');
            return [];
        }
        
        var results = [];
        var keys = Object.keys(this.messages);
        
        keys.forEach(function(key) {
            if (key.toLowerCase().indexOf(searchText.toLowerCase()) !== -1 || 
                this.messages[key].toLowerCase().indexOf(searchText.toLowerCase()) !== -1) {
                results.push({
                    key: key,
                    value: this.messages[key]
                });
            }
        }.bind(this));
        
        console.log('[i18n] Search results for "' + searchText + '" (' + results.length + ' found):');
        results.forEach(function(result) {
            console.log('  - ' + result.key + ': "' + result.value + '"');
        });
        
        return results;
    }
};

// 页面加载完成后初始化
$(document).ready(function() {
    i18n.init();
});
