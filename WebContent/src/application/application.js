import Backbone from 'backbone';
import {Application, RegionManager} from 'marionette';

import Router from '../router.js';

import LayoutView from './layout_view.js';
import AppController from '../apps/history_trace/controller.js'

let App = new (Application.extend({
    initialize: function () {
        this.layout = new LayoutView();
        this.layout.render();
    },

    onStart: function () {
        new Router();
        Backbone.history.start();
    }
}));

export default App;

App.getLocalVal = function (e) {
    let retVal = localStorage.getItem(e);
    if (!retVal) {
        retVal = '';
    }
    return retVal;
};

App.setLocalVal = function (e, v) {
    localStorage.setItem(e, v);
};

App.getEventTarget = function (e) {
    return $(e.target || e.srcElement);
};