let currentPage = 'intro', lastPage;
const hostName = 'https://localhost:2018/';

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

function fadeIn(ele, speed, onDone) {
    ele.style.opacity = 0;
    let s = 0;
    let timer = setInterval(function () {
        if (ele.style.opacity < 1) {
            s += 0.12;
            ele.style.opacity = s;
        } else {
            ele.style.opacity = 1;
            clearInterval(timer);
            if (typeof onDone === 'function') onDone()
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
    }, from, to, 300, done);
}

let userData = {r: -1};

function updateUserData(success, error) {
    $.ajax({
        url: hostName + 'user?request=check',
        success: function (r) {
            console.info(r);
            userData = r;
            if (typeof success === 'function') success(r)
        },
        error: error
    });
}

let navUpdate, onDisable, navToolbar;

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

const colors = ['BLACK', 'DARK_BLUE', 'DARK_GREEN', 'DARK_AQUA', 'DARK_RED', 'DARK_PURPLE', 'GOLD', 'GREY', 'DARK_GRAY', 'BLUE', 'GREEN', 'AQUA', 'RED', 'LIGHT_PURPLE', 'YELLOW', 'WHITE'];
const colorHex = [
    '#000000', '#303F9F', '#388E3C', '#1976D2',
    '#d32f2f', '#7B1FA2', '#FFD740', '#9E9E9E',
    '#616161', '#2196F3', '#4CAF50', '#03A9F4',
    '#f44336', '#AB47BC', '#FFEB3B', '#FAFAFA'
];

function getCustomizedText(text) {
    colors.forEach((v, i) => {
        let value;

        function once() {
            let index = text.indexOf(value);
            while (index !== -1) {
                text = text.replace(value, '<span style="color: ' + colorHex[i] + '">') + '</span>';
                index = text.indexOf(value);
            }
        }

        value = '$' + v.toLowerCase();
        once();
        value = '§';
        if (i <= 9) {
            value += i;
        } else {
            switch (i) {
                case 10:
                    value += 'a';
                    break;
                case 11:
                    value += 'b';
                    break;
                case 12:
                    value += 'c';
                    break;
                case 13:
                    value += 'd';
                    break;
                case 14:
                    value += 'e';
                    break;
                case 15:
                    value += 'f';
                    break;
            }
        }
        once()
    });
    let index = text.indexOf('${');
    while (index !== -1) {
        let b = text.indexOf('}', index);
        if (b === -1) break;
        let lang = text.substring(index + 2, b);
        let result = '[unknown]';
        $.ajax({
            url: hostName + 'lang?get=' + lang,
            async: false,
            success: function (r) {
                result = r;
            }
        });
        text = text.substring(0, index) + result + text.substring(b + 1);
    }
    return text;
}

function showTopProgress() {
    fadeIn(document.getElementById('prgs'), 20)
}

function hideTopProgress() {
    fadeOut(document.getElementById('prgs'), 20)
}

let bottomDialogID = 0;

function BottomDialog(ele, options) {
    this.element = ele;
    this.id = bottomDialogID++;
    ele.style.display = 'none';

    $(document.body).append('<div class="modal-overlay" id="modal-overlay-' + this.id + '"></div>');
    let overlay = $('#modal-overlay-' + this.id);
    this.isShown = false;

    overlay.click(() => {
        if (this.isShown)
            this.dismiss()
    });

    overlay.css('opacity', 0);
    if (options) {
        if (typeof options.onShow === 'function') this._onShow = options.onShow;
        if (typeof options.onDismiss === 'function') this._onDismiss = options.onDismiss;
    }

    this.show = () => {
        overlay.css('zIndex', 1000)
            .css('display', 'block');
        $.extend(this.element.style, {
            display: 'block',
            opacity: 0
        });
        startAnimation((s) => {
            overlay.css('opacity', s);
        }, 0, 0.5, 200, 20, () => {
            this.isShown = true
        });

        setTimeout(() => {
            this.element.style.opacity = 1;

            let targetHeight = this.element.clientHeight;
            startAnimation((s) => {
                this.element.style.marginBottom = s + 'px';
            }, -targetHeight, 0, 220);
            let actionBar = $(this.element).children('.dialog-actionbar');
            if (actionBar.length > 0) {
                let actionBarHeight = actionBar.get(0).clientHeight;
                startAnimation(
                    (s) => {
                        actionBar.css('bottom', s + 'px')
                    },
                    -actionBarHeight,
                    0,
                    300 * actionBarHeight / this.element.clientHeight
                );
            }

            if (this._onShow)
                this._onShow()
        }, 10)
    };

    this.dismiss = () => {
        this.isShown = false;
        startAnimation((s) => {
            overlay.css('opacity', s);
        }, 0.5, 0, 300, 20, () => overlay.css('display', 'none'));
        startAnimation((s) => {
            this.element.style.marginBottom = s + 'px';
        }, 0, -this.element.clientHeight, 300, () => this.element.style.display = 'none');

        let actionBar = document.getElementsByClassName('dialog-actionbar')[0];
        startAnimation(
            (s) => {
                actionBar.style.bottom = s + 'px'
            },
            0,
            -actionBar.clientHeight,
            300 * actionBar.clientHeight / this.element.clientHeight
        );
        if (this._onDismiss)
            this._onDismiss()
    }
}

function renderSize(value) {
    if (null == value || value === '') {
        return "空文件";
    }
    let unitArr = ["字节", "千字节", "兆字节", "吉字节", "太字节", "拍字节", "艾字节", "泽字节", "尧字节"];
    let index, srcsize = parseFloat(value);
    if (srcsize === 0) {
        return "空文件"
    }
    index = Math.floor(Math.log(srcsize) / Math.log(1024));
    let size = srcsize / Math.pow(1024, index);
    //  保留的小数位数
    size = size.toFixed(2);
    return size + unitArr[index];
}

function showFab(ele, callback) {
    if (ele.style.transform !== 'scale(1)')
        startAnimation((s) => ele.style.transform = 'scale(' + s.toFixed(3) + ')', 0, 1, 200, callback)
    else if (typeof callback === 'function')
        callback()
}

function hideFab(ele, callback) {
    if (ele.style.transform !== 'scale(0)')
        startAnimation((s) => ele.style.transform = 'scale(' + s.toFixed(3) + ')', 1, 0, 200, callback)
    else if (typeof callback === 'function')
        callback()
}

let toolbarID = 0;

function Toolbar(ele, items, darkMode) {
    this.items = {};
    if (typeof items === 'boolean') {
        darkMode = items;
        items = undefined
    }
    this.id = toolbarID++;

    this.element = ele;
    $(ele).append('<div id="bar-' + this.id + '" style="display: inline"></div>');

    this.push = (icon, name, priority, isActive, onClick, tooltipPosition) => {
        if (this.items.hasOwnProperty(name)) return;

        if (typeof icon !== 'string') {
            pushFrom(icon);
            return this.items[icon.name]
        } else {
            if (typeof onClick === 'string') {
                tooltipPosition = onClick;
                onClick = undefined
            }

            let bar = $('#bar-' + this.id);

            let appendToBar = () => {
                let jquery = $('<i class="mdi ' + icon + ' mdi-24px' + (darkMode ? ' black-text' : '') + '" style="display: inline; margin-right: 5px; margin-left: 5px" data-tooltip="' + name + '" data-position="' + (tooltipPosition || 'top') + '"></i>');
                jquery.tooltip();
                bar.append(jquery);
                this.items[name] = {
                    name: name,
                    priority: priority,
                    jquery: jquery,
                    isInMenu: false
                }
            };

            let appendToMenu = () => {
                // Check if menu exists
                let bar = $(ele);
                if (bar.children('.dropdown-content').length < 1) {
                    bar.append('<ul class="dropdown-content" id="dropdown-' + this.id + '"></ul>');
                }
                if (bar.children('.dropdown-trigger').length < 1) {
                    bar.append('<i class="dropdown-trigger mdi mdi-dots-vertical mdi-24px ' + (darkMode ? ' black-text' : '') + ' waves-effect black-text" style="margin-top: -7px; display: inline" data-target="dropdown-' + this.id + '" id="trigger-' + this.id + '"></i>');
                    $('.dropdown-trigger').dropdown();
                }
                let jquery = $('<li><a href="#" class="black-text">' + name + '</a></li>');
                $('#dropdown-' + this.id).append(jquery);
                this.items[name] = {
                    name: name,
                    priority: priority,
                    jquery: jquery,
                    isInMenu: true
                }
            };
            switch (priority) {
                case Toolbar.DisplayPriority.showAlways:
                    appendToBar();
                    break;
                case Toolbar.DisplayPriority.hideAlways:
                    appendToMenu();
                    break;
                case Toolbar.DisplayPriority.smart:
                    if (this.items.length > 4) {
                        appendToMenu()
                    } else {
                        appendToBar()
                    }
                    break;
                default:
                    appendToBar()
            }

            let result = this.items[name];
            result.setActive = (active, force) => {
                if (force || isActive !== active) {
                    function animate(from, to) {
                        let element = result.jquery.get(0);
                        startAnimation(s => element.style.color = 'rgba(0, 0, 0, ' + s + ')', from, to, 200)
                    }

                    if (!active) {
                        animate(1, 0.26);
                    } else {
                        animate(0.26, 1)
                    }
                    isActive = active;
                }
            };
            result.isActive = () => {
                return isActive;
            };
            if (isActive === false) {
                result.setActive(false, true)
            } else {
                result.setActive(true, true)
            }

            result.click = l => {
                result.jquery.off('click')
                    .on('click', () => {
                        if (l) l(result)
                    })
            };
            result.click(onClick);

            return result;
        }
    };
    this.remove = (names) => {
        if (typeof names === 'string') {
            if (this.items.hasOwnProperty(names)) {
                let r = this.items[names];
                r.jquery.remove();
                delete this.items[names];
                if (r.isInMenu) {
                    for (let value in this.items) {
                        if (value.isInMenu) return
                    }
                    $('#trigger-' + this.id).remove()
                }
            }
        } else if (typeof names === 'object') {
            for (let i in names) {
                this.remove(names[i])
            }
        }
    };

    this.contains = (name) => {
        return this.items.hasOwnProperty(name)
    };

    this.show = () => $(ele).show();
    this.hide = () => $(ele).hide();

    let pushFrom = (it) => {
        let hasChildren = it.hasOwnProperty('children');
        let result = this.push(it.icon, it.name, it.priority, it.active, it.onclick, it.tooltipPosition);

        if (hasChildren) {
            let id = new Date().getTime();
            let root = $(ele);

            function addChildren(parent, children) {
                if (darkMode !== undefined) {
                    if (darkMode) parent.classList.add('black-text');
                    else parent.classList.add('mdi-light');
                }
                parent.classList.add('dropdown-trigger');
                parent.dataset.target = 'dropdown-' + id;
                let content = $('<ul class="dropdown-content" id="dropdown-' + id + '"></ul>');
                root.append(content);
                let parentJquery = $(parent);
                parentJquery.dropdown({
                    constrainWidth: false
                });
                for (let index in children) {
                    if (children.hasOwnProperty(index)) {
                        let childConfig = children[index], hasChildren = childConfig.hasOwnProperty('children');
                        if (hasChildren) id = new Date().getTime();
                        let child = $('<li><a class="black-text center" style="padding: 10px; font-size: 16px" ' + (hasChildren ? 'data-target="dropdown-' + id + '"' : '') + '>' +
                            '<i class="mdi ' + childConfig.icon + ' mdi-18px" style="margin-right: auto;"></i>' +
                            childConfig.name +
                            '</a></li>');
                        content.append(child);
                        child.click(childConfig.onclick);
                        if (hasChildren) {
                            addChildren(child.get(0).firstChild, childConfig.children)
                        }
                    }
                }
            }

            addChildren(result.jquery.get(0), it.children)
        }
    };
    for (let i in items) {
        if (items.hasOwnProperty(i)) {
            pushFrom(items[i])
        }
    }
}

Toolbar.DisplayPriority = {
    showAlways: Symbol('showAlways'),
    hideAlways: Symbol('hideAlways'),
    smart: Symbol('smart')
};

function isUnderSameDomain(a, b) {
    if (typeof a !== 'string' || typeof b !== 'string') return false;

    function getDomain(str) {
        if (str.startsWith('http://')) {
            let index = str.indexOf('/', 8);
            return str.substring(7, index === -1 ? null : index)
        } else if (str.startsWith('https://')) {
            let index = str.indexOf('/', 9);
            return str.substring(8, index === -1 ? null : index)
        } else {
            let index = str.indexOf('/');
            return str.substring(0, index === -1 ? null : index)
        }
    }

    return getDomain(a) === getDomain(b)
}

function renderMarkdown(parent, hrefClickHandler) {
    if (typeof parent === 'function') {
        hrefClickHandler = parent;
        parent = undefined
    }
    parent = parent || $('main');

    parent.find('markdown').each((index, ele) => {
        let original = ele.innerHTML, markdown = "";
        let start;
        original.split('\n').forEach(line => {
            if (start === undefined) {
                start = 0;
                for (; start < line.length; start++) {
                    let char = line.charAt(start);
                    if (char !== ' ' && char !== '\t')
                        break
                }
            }
            markdown += line.substr(start) + '\n'
        });
        ele.innerHTML = markdownit({
            highlight: function (str, lang) {
                if (lang && hljs.getLanguage(lang)) {
                    try {
                        return hljs.highlight(lang, str).value;
                    } catch (__) {
                    }
                }
            }
        }).render(markdown)
    });
    parent.find('a').on('click', hrefClickHandler)
}