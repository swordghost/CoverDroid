import {CompositeView, ItemView} from 'marionette';
import entryTpl from './method_info.hbs';
import listTpl from './method_info_list.hbs';

let methodInfoView = ItemView.extend({
    className: 'tab-pane panel panel-primary',
    template: entryTpl,

    initialize: function (_options) {
        this.model = _options.model;
        this.el.id = this.model.get('index');
    }
});

export default CompositeView.extend({
    className: 'tab-content',
    childView: methodInfoView,
    childViewContainer: '',
    template: listTpl,

    updateData: function (_options) {
        this.collection = _options.collection;
        if (_options.controller) {
            this.controller = _options.controller;
        }
        this.render();
    }
});