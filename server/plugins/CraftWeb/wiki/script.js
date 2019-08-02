function init() {
    /** Widgets **/
    function scrollToTop() {
        $('html').scrollTop(0);
    }

    function openEdit() {
        updateUserData(function (userData) {
            if (userData.r === -1) {
                M.toast({
                    html: '<span>您必须登录才能使用编辑工具</span>'
                })
            } else {
                function next() {
                    let navItemSearch = navToolbar.items['搜索'];
                    navItemSearch.setActive(false);
                    $('main').load(hostName + 'ui?request=wikiEditor', scrollToTop)
                        .on('callback', function (ele) {
                            navItemSearch.setActive(true);
                            $('main').off('callback')
                        });
                }

                if (currentPage !== 'wikiNew') {
                    $.ajax({
                        url: hostName + 'wiki?operation=lock&title=' + currentPage.substring(currentPage.indexOf('/') + 1),
                        success: function (json) {
                            if (json.r === 0) {
                                next();
                            } else {
                                let html = '无法获得文件锁';
                                if (json.r === 1) html += '，或许有其他人正在编辑该页面。请稍后重试';
                                else if (json.r === 2) html += '：无法编辑历史版本';
                                M.toast({
                                    html: html
                                })
                            }
                        }
                    })
                } else {
                    next()
                }
            }
        });

    }

    let navSearch = $('#nav-search'), searchInput = $("#search-input");
    let searchResult, btnEdit = $('#btn-edit');
    hideFab(document.getElementById('btn-message'), () => {
        btnEdit.children('a').children('i').removeClass('mdi-content-save').addClass('mdi-pencil');
        btnEdit.attr('data-tooltip', '编辑此页面');

        btnEdit.off('click').click(() => {
            openEdit();
            hideResults()
        });
        showFab(btnEdit.get(0));

        navToolbar.push('mdi-file-plus mdi-light', '新建', Toolbar.DisplayPriority.showAlways, true, function () {
            currentPage = 'wikiNew';
            openEdit()
        }, 'bottom');
        navToolbar.push('mdi-magnify mdi-light', '搜索', Toolbar.DisplayPriority.showAlways, true, function (ele) {
            if (ele.isActive()) {
                fadeIn(navSearch.get(0), 30, () => {
                    navSearch.show();
                    searchInput.focus()
                });
            }
        }, 'left');
    });
    $('#search-close').off('click').click(function () {
        hideResults()
    });
    searchInput.off('keydown').on('keydown', (event) => {
        if (event.keyCode === 13) {
            searchHandler()
        }
    });
    $('#btn-search').off('click').on('click', searchHandler);

    function searchHandler() {
        let search = searchInput.val();
        if (search) {
            searchResult.empty();
            showTopProgress();
            $.ajax({
                url: hostName + 'wiki?operation=search&key=' + search,
                success: function (json) {
                    appendResultBackground();
                    if (json.r.length > 0) {
                        for (let i in json.r) {
                            if (json.r.hasOwnProperty(i)) {
                                let value = json.r[i], title = value.title;
                                let info = value.info;
                                let html = '<li class="collection-item ' + (!info.isImage ? 'avatar waves-effect' : '') + '" style="width: 100%">';
                                if (!info.isImage) {
                                    html += '<i class="mdi mdi-text circle"></i>' +
                                        '<span class="title">' + info.title + '</span>';
                                } else {
                                    html += '<img src="wiki/' + title + '" class="materialboxed" width="100%"/>' +
                                        '<span class="title">' + title + '</span>' +
                                        '<p>图片档案</p>';
                                }
                                if (info.hasOwnProperty('subtitle'))
                                    html += '<p>' + info.subtitle + '</p>';
                                if (value.hasOwnProperty('keywords') && value.keywords.length > 0)
                                    html += '<p> 包括关键字: ' + value.keywords + '</p>';
                                html += '</li>';
                                let jquery = $(html);
                                searchResult.append(jquery);
                                if (info.isImage) jquery.children('.materialboxed').materialbox();
                                else {
                                    jquery.click(function () {
                                        hideResults(() => {
                                            initContent(title);
                                            searchInput.val(undefined)
                                        });
                                    });
                                }
                            }
                        }
                    } else {
                        searchResult.append('<h5 class="center">未找到结果</h5>');
                    }
                    showResults();
                    hideTopProgress();
                }
            })
        }
    }

    function appendResultBackground() {
        searchResult = $('#search-result');
        if (searchResult.length < 1) {
            $('main').append('<div class="container"><ul class="collection" style="background: white; ' +
                'display: none; top: 0;" id="search-result"></ul></div>');
            searchResult = $('#search-result');
        }
    }

    function showResults() {
        if (searchResult.is(':visible')) return;
        appendResultBackground();
        scrollToTop();

        hideFab(btnEdit.get(0));
        searchInput.blur();

        let height = searchResult.height();
        searchResult.css('top', -height + 'px').css('overflowY', 'auto').show();
        startAnimation((s) => searchResult.css('top', s), -height, 0, 200);

        function scrollHider() {
            let height = searchResult.parent().get(0).scrollHeight;
            if (window.pageYOffset >= height) {
                searchResult.hide();
                hideResults();
                $(document).off('scroll', scrollHider)
                    .scrollTop(window.pageYOffset - height);
            }
        }

        $(document).off('scroll', scrollHider).on('scroll', scrollHider)
    }

    function hideResults(callback) {
        let height = searchResult.outerHeight();
        fadeOut(navSearch.get(0), 20, () => navSearch.hide());
        showFab(btnEdit.get(0));
        if (searchResult.is(':visible')) {
            startAnimation((s) => searchResult.css('top', s), 0, -height, 200, () => {
                searchResult.hide().empty();
                if (typeof callback === 'function') callback();
            })
        } else {
            if (typeof callback === 'function') callback()
        }
    }

    onDisable = () => {
        if (lastPage.startsWith('wikiEditor')) {
            M.toast({
                html: '请在编辑器中保存或放弃修改'
            });
            return false
        }
        if (!currentPage.startsWith('wiki')) {
            hideFab(document.getElementById('btn-edit'),
                () => showFab(document.getElementById('btn-message')));
            navToolbar.remove(['搜索', '新建']);
        }
        document.body.style.overflowY = 'auto';
        hideResults();
    };

    function initializeMarkdown() {
        hideTopProgress();
        renderMarkdown();
        $('pre').css('overflowX', 'auto')
    }

    /** Content **/
    function initContent(href, initial, callback) {
        if (!initial) showFab(btnEdit.get(0));
        $.ajax({
            url: hostName + 'wiki?title=' + href,
            success: function (json) {
                let main = $('main');
                main.empty();
                appendResultBackground();
                if (json.hasOwnProperty('info')) {
                    if (href !== 'frontPage') {
                        let info = json.info, title = '<div class="container"><h1>' + info.title + '</h1>',
                            isCardAppended = false;

                        function appendCard() {
                            if (isCardAppended) return;
                            title += '<div class="card white black-text"><div class="card-content">';
                            isCardAppended = true
                        }

                        function encloseCard() {
                            if (!isCardAppended) return;
                            title += '</div></div>'
                        }

                        if (info.hasOwnProperty('subtitle') && info.subtitle) {
                            appendCard();
                            title += '<i><i class="mdi mdi-text mdi-dark"></i> ' + info.subtitle + '</i>'
                        }
                        if (info.hasOwnProperty('author')) {
                            appendCard();
                            title += '<p><i class="mdi mdi-account mdi-dark"></i> 作者: ' + info.author.toString() + '</p>'
                        }
                        if (info.hasOwnProperty('backup')) {
                            appendCard();
                            title += '<p><i class="mdi mdi-history mdi-dark"></i> 旧版本档案' + info.backup + '</p>'
                        }
                        encloseCard();
                        title += '</div>';
                        main.append(title);
                    }
                    main.append(json.content);
                    initializeMarkdown();

                    $('img').each(function (index, ele) {
                        if (!ele.style.maxWidth) {
                            ele.style.maxWidth = '100%'
                        }
                    });
                    scrollToTop();

                    currentPage = 'wiki/' + href
                } else {
                    switch (json.r) {
                        case 404:
                            main.append('<h3 class="center"><i class="mdi mdi-cancel mdi-dark"></i> 内容未找到</h3>');
                            currentPage = 'wikiNotFound';
                            break;
                        default:
                            main.append('<h3 class="center"><i class="mdi mdi-alert-circle mdi-dark"></i> 出错了</h3>');
                    }
                    hideTopProgress()
                }
                if (typeof callback === 'function') callback()
            }
        });
    }

    initContent('$title', true)
}

init();