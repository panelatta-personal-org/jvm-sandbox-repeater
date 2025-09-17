var host = window.location.host;
var protocol = window.location.protocol;
jQuery(function ($) {
    $("[data-toggle='tooltip']").tooltip();
    $("[data-toggle='popover']").popover();
    $(".refresh-btn").on("click",function () {
        window.location.reload();
    });
});

var innerPost = function (uri, data, callback) {
    $.post("//" + host + uri, data, callback)
};

/**
 * 异步ajax请求
 * @param uri
 * @param data
 * @param type
 * @param callback
 */
var innerAsyncAjax = function (uri, data, type, callback) {
    showLoading(100)
    $.ajax({
        type: type,
        url: "//" + host + "/" + uri,
        data: data,
        success: function (res) {
            hideLoading(100)
            callback(res)
        },
        async: true,
        dataType: 'json',
        error: function (XMLHttpRequest) {
            hideLoading(100)
            notice(i18n.get("msg.network.error") + " " + XMLHttpRequest.responseText, false);
        }
    });
};

/**
 * 同步的ajax请求
 * @param uri
 * @param data
 * @param type
 * @param callback
 */
var innerSyncAjax = function (uri, data, type, callback) {
    showLoading(500)
    $.ajax({
        type: type,
        url: "//" + host + "/" + uri,
        data: data,
        success: function (res) {
            hideLoading(500)
            callback(res)
        },
        async: false,
        dataType: 'json',
        error: function (XMLHttpRequest) {
            hideLoading(500)
            notice(i18n.get("msg.network.error") + " " + XMLHttpRequest.responseText, false);
        }
    });
};

var rnd = function (n, m) {
    var random = Math.floor(Math.random() * (m - n + 1) + n);
    return random;
};

var showLoading = function (time) {
    jQuery("#fake-loader").fadeIn(time);
};

var hideLoading = function (time) {
    jQuery("#fake-loader").fadeOut(time);
};

var notice = function (message, status) {
    if (status) {
        jQuery("#success-message-area").html(message);
        jQuery("#success-modal").modal('show')
    } else {
        jQuery("#danger-message-area").html(message);
        jQuery("#danger-modal").modal('show')
    }
};

var noticeUrl = function (message, url) {
    jQuery("#url-notice-content").text(message);
    jQuery("#url-notice-url").attr('href', url);
    jQuery("#url-modal").modal('show')
};

var confirmTwice = function (message, callback) {
    jQuery(document).off('click', '.confirm-btn');
    jQuery("#confirm-modal-message").text(message);
    jQuery("#confirm-modal").modal("show");
    jQuery(document).on('click', '.confirm-btn', callback);
};

var checkTwice = function (message, callback) {
    jQuery(document).off('click', '.check-btn');
    jQuery("#check-modal-message").text(message);
    jQuery("#check-modal").modal("show");
    jQuery(document).on('click', '.check-btn', callback);
};

var confirmDismiss =  function () {
    jQuery("#confirm-modal").modal("hide");
};

var checkDismiss =  function () {
    jQuery("#check-modal").modal("hide");
};

var notify = function (msg) {
    jQuery.notify({
        // options
        icon: "fa fa-bell-o",
        message: msg
    }, {
        // settings
        type: 'warning',
        animate: {
            enter: 'animated bounceInRight',
            exit: 'animated bounceOutRight'
        },
        placement: {
            from: "top",
            align: "right"
        },
        delay: 1000,
        template: '<div data-notify="container" class="col-xs-4 col-sm-3 alert alert-{0}" role="alert"><button type="button" aria-hidden="true" class="close" data-notify="dismiss">&times;</button><span data-notify="icon"></span> <span data-notify="title">{1}</span> <span data-notify="message">{2}</span><div class="progress" data-notify="progressbar"><div class="progress-bar progress-bar-{0}" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;"></div></div><a href="{3}" target="{4}" data-notify="url"></a></div>'
    });
};
/**
 * bind data into dom
 * @param rootElement root element
 * @param json
 */
var bindData = function (rootElement, json) {
    var el = jQuery("#" + rootElement);
    if (el === undefined) {
        console.log("no valid root element found in dom,selector is " + rootElement)
        return false;
    }
    var inputBinders = el.find('input[data-bind]');
    inputBinders.each(function () {
        var key = jQuery(this).attr('data-bind');
        var value = json[key];
        if (value !== undefined) {
            jQuery(this).val(value)
        }
    });
    var textareaBinders = el.find('textarea[data-bind]');
    textareaBinders.each(function () {
        var key = jQuery(this).attr('data-bind');
        var value = json[key];
        if (value !== undefined) {
            jQuery(this).val(value)
        }
    });
};

var openNewWindow = function (url, msg) {
    var wd = window.open(url);
    if (wd == null) {
        noticeUrl(msg, url)
    }
};