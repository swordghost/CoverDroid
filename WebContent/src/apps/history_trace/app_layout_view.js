import {LayoutView} from 'marionette';
import template from './app_layout.hbs';

require('./app_layout.scss');

export default LayoutView.extend({
    className: 'cover-droid',
    tagName: 'div',
    template: template,

    regions: {
        traceFileList: '.trace-file-list',
        dexFileList: '.dex-file-list',
        clzList: '.method-class',
        methodInfo: '.method-info'
    },

    triggers: {
        'click #load-dex-zip': 'load:dexZip'
    }
});