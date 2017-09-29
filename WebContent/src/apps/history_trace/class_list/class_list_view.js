import {CompositeView, ItemView} from 'marionette';
import entryTpl from './class_entry.hbs';
import listTpl from './class_list.hbs';

let classEntryView = ItemView.extend({
    tagName: 'li',
    template: entryTpl
});

export default CompositeView.extend({
    className: 'nav nav-pills nav-stacked',
    childView: classEntryView,
    tagName: 'ul',
    template: listTpl,

    updateData: function (_options) {
        this.collection = _options.collection;
        if (_options.controller) {
            this.controller = _options.controller;
        }
        this.render();
    }
});