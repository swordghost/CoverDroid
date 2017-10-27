import * as Marionette from 'marionette';
import Backbone from 'backbone';
import {Collection} from 'backbone';
import $ from 'jquery';

import App from '../../application/application.js';

import HomePageView from './home_page_view.js'
import PkgNameListView from './pkg_name_list/pkg_name_list_view.js';

export default Marionette.Object.extend({
    initialize: function (options) {
        this.container = options.container;
        this.view = new HomePageView();
        this.container.show(this.view);
        this.view.on({
            'update:pkgNames': this.updatePkgNames.bind(this),
            'show:traceComponent': function () {
                $('#trace-component').show();
            },
            'auto:trace': this.autoTrace.bind(this),
            'dump:dex': this.dumpDex.bind(this)
        });
        this.showPage();
    },

    showPage: function () {
        let pkgNameView = new PkgNameListView({collection: new Collection([]), controller: this});
        this.view.pkgList.show(pkgNameView);

        $('#trace-component').hide();

        $('#phone-ip').val(App.getLocalVal('phoneIp'));
        this.updatePkgNames();
    },

    updatePkgNames: function () {
        let phoneIp = $('#phone-ip').val();
        if (!phoneIp) {
            if (event && App.getEventTarget(event) === $('#update-pkg-names'))
                alert("Can not read phone's ip!");
            return;
        }
        let _controller = this;
        $.ajax({
            url: `./nativequery?type=queryPackages&ip=${phoneIp}`,
            timeout: 10000,
            success: function (resp) {
                if (resp[0] && resp[0]['pkgName']) {
                    let pkgNameCollection = new Collection(resp);
                    _controller.view.pkgList.currentView.updateData({
                        collection: pkgNameCollection,
                        controller: _controller
                    });
                    App.setLocalVal('phoneIp', phoneIp);
                    $('.pkg-name-list').show();
                }
            },
            error: function (resp) {
            }
        })
    },

    autoTrace: function () {
        let phoneIp = $('#phone-ip').val();
        let pkgName = $('#pkg-list').val();
        let _controller = this;
        if (!phoneIp || !pkgName) {
            alert("Please set the phone's ip and select a package!");
            return;
        }

        let traceInfo = $('#trace-info');
        let traceBtn = $('button#auto-trace');

        $.ajax({
            url: `./nativequery?type=autoTrace&ip=${phoneIp}&pkg_name=${pkgName}`,
            timeout: 10000,
            success: function (resp) {
                if (resp["status"] && resp["status"] === 'start') {
                    traceBtn.removeClass('btn-primary');
                    traceBtn.addClass('btn-danger');
                    traceBtn.text('Stop');
                    traceInfo.text('Recording...');
                } else if (!resp['status'] && resp['traceFile']) {
                    traceInfo.text("Parsing Trace...");
                    $.ajax({
                        url: `./nativequery?type=dumpDex&ip=${phoneIp}&pkg_name=${pkgName}`,
                        timeout: 100000,
                        success: function (resp2) {
                            if (resp2["status"] && resp2["status"] === 'success') {
                                traceBtn.removeClass('btn-danger');
                                traceBtn.addClass('btn-primary');
                                traceBtn.text('Record');

                                traceInfo.text("Parsing is done! Going to show the coverage...");
                                App.setLocalVal('traceFile', resp["traceFile"]);
                                App.setLocalVal('dexFile', resp2['dexFile']);
                                setTimeout(function () {
                                    Backbone.history.navigate(`#history?`, {trigger: true});
                                }, 1500);
                            }
                        },
                        error: function () {
                            alert('Error! Please retry.');
                        }
                    })

                } else {
                    alert('Error! Please retry.');
                }
            },
            error: function (resp) {
                alert('Error! Please retry.');
            }
        })
    },

    dumpDex: function () {
        let traceInfo = $('#trace-info');
        let phoneIp = $('#phone-ip').val();
        let pkgName = $('#pkg-list').val();
        traceInfo.text('Dumping dex, please wait ...');
        $.ajax({
            url: `./nativequery?type=dumpDex&ip=${phoneIp}&pkg_name=${pkgName}`,
            timeout: 100000,
            success: function (resp) {
                if (resp["status"] && resp["status"] === 'success') {
                    traceInfo.text('dumping dex success');
                }
            },
            error: function (resp) {
                alert('Error! Please retry.');
            }
        });
    },

    showFiles: function (succ, target) {
        $.ajax({
            url: "./nativequery?type=lsDir" + "&succ=" + succ,
            dataType: "json"
        }).done(function (e) {
            this.showFilesAt(e, target);
        }).fail(function (t) {
            global.errt = t;
        });
    },

    showFilesAt: function (list, target) {
        var ret = "";
        for (var i = 0; i < list.length; i++) {
            ret += "<option>";
            ret += list[i];
            ret += "</option>";
        }
        $(target).html(ret);
    }
});