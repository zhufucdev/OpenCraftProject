function openGithub() {
    window.open("https://github.com/zhufucdev/OpenCraftProject")
}

function github() {
    let html = '<span>项目地址</span><a class="btn-flat toast-action" onclick="openGithub()">访问</a>';
    M.toast({
        html: html,
        displayLength: 5000
    });
}

function fadeOut(ele, speed, done) {
    ele.style.opacity = 1;
    let timer = setInterval(function () {
        if (ele.style.opacity > 0) {
            ele.style.opacity -= 0.12;
        } else {
            ele.style.opacity = 0;
            clearInterval(timer);
            if (done !== undefined)
                done.call()
        }
    }, speed)
}

function fadeIn(ele, speed) {
    ele.style.opacity = 0;
    let s = 0;
    let timer = setInterval(function () {
        if (ele.style.opacity < 1) {
            s += 0.12;
            ele.style.opacity = s;
        } else {
            ele.style.opacity = 1;
            clearInterval(timer)
        }
    }, speed)
}

function fadeSwitch(loadData, ele, async) {
    fadeOut(ele, 20, function () {
        if (!async) {
            loadData();
            $('#onload');
            fadeIn(ele, 20)
        } else {
            loadData(
                function () {
                    $('#onload');
                    fadeIn(ele, 20)
                }
            );
        }
    });
}

function col(ele, from, to, done) {
    startAnimation((s) => {
        ele.style.height = s + 'px'
    },from,to,300,done);
}

let userData;

function updateUserData(success, error) {
    $.ajax({
        url: 'user?request=check',
        success: function (r) {
            console.info(r);
            userData = JSON.parse(r);
            if (success !== undefined) success.call()
        },
        error: error
    });
}

let navUpdate;
let onDisable;

function isWideScreen() {
    return window.innerWidth >= 992;
}

const sec = 1000;
const min = sec * 60;
const hour = min * 60;
const day = hour * 24;
const year = day * 365;

function gameTime(tick) {
    if (tick >= year) {
        let y = parseInt(tick / year);
        let t = tick - year * y;
        if (t > 0) {
            let d = parseInt(t / day);
            t -= day * d;
            if (t > 0) {
                let h = parseInt(t / hour);
                t -= hour * h;
                if (t > 0) {
                    let m = parseInt(t / min);
                    t -= min * m;
                    if (t > 0) {
                        let s = t / sec;
                        return y + '年' + d + '天' + h + '时' + m + '分' + s + '秒'
                    } else {
                        return y + '年' + d + '天' + h + '时' + m + '分钟整'
                    }
                } else {
                    return y + '年' + d + '天' + h + '小时整'
                }
            } else {
                return y + '年' + d + '天整'
            }
        } else {
            return y + '年整'
        }
    } else if (tick >= day) {
        let d = parseInt(tick / day);
        let t = tick - day * d;
        if (t > 0) {
            let h = parseInt(t / hour);
            t -= hour * h;
            if (t > 0) {
                let m = parseInt(t / min);
                t -= min * m;
                if (t > 0) {
                    let s = t / sec;
                    return d + '天' + h + '时' + m + '分' + s + '秒'
                } else {
                    return d + '天' + h + '时' + m + '分钟整'
                }
            } else {
                return d + '天' + h + '小时整'
            }
        } else {
            return d + '天整'
        }
    } else if (tick >= hour) {
        let h = parseInt(tick / hour);
        let t = tick - hour * h;
        if (t > 0) {
            let m = parseInt(t / min);
            t -= min * m;
            if (t > 0) {
                let s = t / sec;
                return h + '时' + m + '分' + s + '秒'
            } else {
                return h + '时' + m + '分钟整'
            }
        } else {
            return h + '小时整'
        }
    } else if (tick >= min) {
        let m = parseInt(tick / min);
        let t = tick - min * m;
        if (t > 0) {
            let s = t / sec;
            return m + '分' + s + '秒'
        } else {
            return m + '分钟整'
        }
    } else if (tick > 0) {
        return tick / sec + '秒'
    } else {
        return '0秒'
    }
}

let faceDate = new Date().getTime();

function resetFace() {
    faceDate = new Date().getTime()
}

function getFace() {
    if (userData.face === true) {
        //To avoid cache
        return '<img class="circle face" src="/user?request=check&check=face&time=' + faceDate + '"/>';
    } else return getDefaultFace()
}

function getDefaultFace() {
    return '<i class="mdi mdi-account-circle" style="font-size: 50px"></i>'
}
const colors = ['BLACK','DARK_BLUE','DARK_GREEN','DARK_AQUA','DARK_RED','DARK_PURPLE','GOLD','GREY','DARK_GRAY','BLUE','GREEN','AQUA','RED','LIGHT_PURPLE','YELLOW','WHITE'];
const colorHex = [
    '#000000','#303F9F','#388E3C','#1976D2',
    '#d32f2f','#7B1FA2','#FFD740','#9E9E9E',
    '#616161','#2196F3','#4CAF50','#03A9F4',
    '#f44336','#AB47BC','#FFEB3B','#FAFAFA'
];
function getCustomizedText(text) {
    colors.forEach((v,i) => {
        let value = '$'+v.toLowerCase();
        let index = text.indexOf(value);
        while (index !== -1){
            text = text.replace(value,'<span style="color: '+colorHex[i]+'">') + '</span>';
            index = text.indexOf(value);
        }
    });
    let index = text.indexOf('${');
    while (index !== -1){
        let b = text.indexOf('}',index);
        if (b === -1) break;
        let lang = text.substring(index+2,b);
        let result = '[unknown]';
        $.ajax({
            url: 'lang?get='+lang,
            async: false,
            success: function (r) {
                result = r;
            }
        });
        text = text.substring(0,index) + result + text.substring(b+1);
    }
    return text;
}