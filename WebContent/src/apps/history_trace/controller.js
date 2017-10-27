import * as Marionette from 'marionette';
import {Collection} from 'backbone';
import $ from 'jquery';

import App from '../../application/application.js';

import AppLayout from './app_layout_view.js';

import TraceListView from './file_list/trace_file_list_view.js';
import ClassListView from './class_list/class_list_view.js';
import MethodInfoListView from './method_info_list/method_info_list_view.js';
import TableElementInput from './summary_table/table_element.hbs';

export default Marionette.Object.extend({
    initialize: function (options) {
        this.container = options.container;
        this.view = new AppLayout();
        this.container.show(this.view);
        let _controller = this;
        this.view.on({
            'load:dexZip': function () {
                let traceFile = $('#trace-file').val();
                if (App.getLocalVal('traceFile') != traceFile) {
                    App.setLocalVal('dexFile', '');
                }
                App.setLocalVal('traceFile', traceFile);
                if (location.href.indexOf('?') === -1) {
                    Backbone.history.navigate(`#history?`, {trigger: true});
                } else {
                    location.reload();
                }
            }
        });
        this.summaryTableEl = $(TableElementInput());
    },

    showPage: function () {
        this.view.traceFileList.show(
            new TraceListView({collection: new Collection([]), controller: this})
        );
        this.loadFileList();

        let classListView = new ClassListView({collection: new Collection([]), controller: this});
        this.view.clzList.show(classListView);
        let methodInfoListView = new MethodInfoListView({collection: new Collection([]), controller: this});
        this.view.methodInfo.show(methodInfoListView);
    },

    loadFileList: function () {
        let _controller = this;
        let _href = location.href;
        $.ajax({
            url: `./nativequery?type=lsDir&succ=.trace`,
            dataType: "json",
            timeout: 10000,
            success: function (resp) {
                if (resp && resp.length) {
                    let traceFiles = resp.map(function (x) {
                        return {file_name: x};
                    });

                    _controller.view.traceFileList.currentView.updateData({collection: new Collection(traceFiles)});
                    if (App.getLocalVal('traceFile')) {
                        $('#trace-file').val(App.getLocalVal('traceFile'));
                    }
                    if (_href && _href.split('?').length > 1) {
                        _controller.loadDexZip(_href.split('?')[1]);
                    }
                }
            },
            error: function (resp) {
            }
        })
    },

    loadDexZip: function (clzName) {
        let traceFile = App.getLocalVal('traceFile');
        let dexFile = App.getLocalVal('dexFile');
        let _controller = this;
        $.ajax({
            url: `./nativequery?type=queryClassCode&fileName=${dexFile}&coverage=${traceFile}&clzName=${clzName}`,
            dataType: "json",
            timeout: 100000,
            success: function (resp) {
                if (resp.name || resp.className) {
                    let collection;
                    let index = 1;
                    if (resp.name) {
                        collection = resp.content;
                        collection = collection.map(function (x) {
                            return {
                                clz_name: x.name,
                                method_name: x.name,
                                index: `method_${index++}`,
                                href_class: x.pkgInfo.clzInfo.substring(0, 1) === '0' ? '' : 'covered',
                                code: `${_controller.parseLine('COVERAGE SUMMARY FOR PACKAGE [' + x.name + ']', [x.pkgInfo])}` +
                                `${_controller.parseLine('COVERAGE BREAKDOWN BY CLASS', x.content, true)}`
                            };
                        });
                        collection.unshift({
                            clz_name: 'SUMMARY',
                            method_name: 'Summary',
                            index: 'method_0',
                            href_class: '',
                            code: `${_controller.parseLine('OVERALL COVERAGE SUMMARY', [resp.overall])}` +
                            `<table class="table"><caption>OVERALL STATS SUMMARY</caption>` +
                            `<tbody><tr><td>total packages</td><td>${resp.totalPkgs}</td></tr>` +
                            `<tr><td>total classes</td><td>${resp.totalClzs}</td></tr>` +
                            `<tr><td>total method</td><td>${resp.totalMethods}</td></tr>` +
                            `<tr><td>total executable lines</td><td>${resp.totalLines}</td></tr></tbody></table>` +
                            `${_controller.parseLine('COVERAGE BREAKDOWN BY PACKAGE', resp.pkgSummary)}`
                        })
                    } else {
                        collection = resp.methods;
                        collection = collection.map(function (x) {
                            let _name = x.methodName.replace('Covered ', '') + '()';
                            return {
                                clz_name: _name,
                                method_name: _name,
                                index: `method_${index++}`,
                                href_class: x.methodName.indexOf('Covered ') !== -1 ? 'covered' : '',
                                code: _controller.parseCode(x.code)
                            }
                        });
                        setTimeout(_controller.bindStyle, 1000);
                        resp.methodReports.unshift('');
                        collection.unshift({
                            clz_name: 'SUMMARY',
                            method_name: resp.className,
                            index: 'method_0',
                            href_class: '',
                            code: `${_controller.parseLine('COVERAGE SUMMARY FOR CLASS [' + resp.className + ']', [resp.classSummary], true)}` +
                            `<table class="table"><caption>COVERAGE BREAKDOWN BY METHOD</caption>` +
                            `<thead><tr><th>[branch,%]</th><th>[line,%]</th><th>[insn,%]</th><th>[name]</th></tr></thead>` +
                            `<tbody>${resp.methodReports.reduce(function (x, y) {
                                return x + '<tr class="' + (y.methodInfo.substring(0, 1) !== '0' ? 'covered' : '') + '">' +
                                    '<td>' + y.branchInfo + '</td><td>' + y.lineInfo + '</td>' +
                                    '<td>' + y.insnInfo + '</td><td>' + y.name.replace('<', '&lt').replace('>', '&gt') + '</td></tr>';
                            })}</tbody>` +
                            `</table>`
                        })
                    }
                    let methodClassCollection = new Collection(collection);
                    _controller.view.clzList.currentView.updateData({collection: methodClassCollection});
                    _controller.view.methodInfo.currentView.updateData({collection: methodClassCollection});
                    $('a[href="#method_0"]').parent().tab("show");
                    $('div#method_0').addClass('active');
                }
            },
            error: function (resp) {
            }
        });
    },

    parseLine: function (caption, lines, isClz) {
        let summaryTable = this.summaryTableEl.clone(true);
        summaryTable.find('caption').text(caption);
        summaryTable.find('thead').append(
            $(`<tr>${isClz ? '' : '<th>[class,%]</th>'}<th>[method,%]</th><th>[branch,%]</th>` +
                `<th>[line,%]</th><th>[insn,%]</th><th>[name]</th></tr>`)
        );
        lines.unshift('');
        summaryTable.find('tbody').append($(lines.reduce(function (x, y) {
            return x + `<tr class="${caption.indexOf('BREAKDOWN') !== -1 && y.clzInfo.substring(0, 1) !== '0' ? 'covered' : ''}">` +
                `${isClz ? '' : '<td>' + y.clzInfo + '</td>'}<td>${y.methodInfo}</td><td>${y.branchInfo}</td>` +
                `<td>${y.lineInfo}</td><td>${y.insnInfo}</td>` +
                `<td>${isClz ? '<a href="#history?' + y.name + '">' + y.name + '</a>' : y.name}</td></tr>`
        })));

        return `<table class="table">${summaryTable.html()}</table>`
    },

    parseCode: function (code) {
        let codeSummaryTable = this.summaryTableEl.clone(true);
        codeSummaryTable.find('caption').text(`TotalReg: ${code['totalReg']}`);
        codeSummaryTable.find('thead').append(
            $(`<tr><th width="60">Off</th><th width="60">Line</th><th width="60">Cover</th><th> </th></tr>`)
        );
        code.content.unshift('');
        let currLine = -1;
        let content = code.content.reduce(function (x, y) {
            return x + `<tr><td>${y.offset}</td><td>${y.lineNumber > 0 ? currLine = y.lineNumber : currLine}</td>` +
                `<td class="cover">${y.coverInfo}</td><td>${y.content}</td></tr>`
        });
        codeSummaryTable.find('tbody').append($(content));

        return `<table class="table">${codeSummaryTable.html()}</table>`;
    },

    bindStyle: function () {
        $("tbody tr").each(function () {
            let cover = ($(this).find('.cover').text() + '').split(' ')[0] / 1.0 + 1.0;
            cover = Math.log2(cover) * 0.3 + 0.1;
            if (cover > 1) {
                cover = 1;
            }
            if (cover > 0.3) {
                $(this).css('background-color', 'skyblue');
                $(this).css('color', `rgb(${Math.round(255 * cover)},0,0)`)
            }
        })
    }

});