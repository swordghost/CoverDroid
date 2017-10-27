import Backbone from 'backbone';

import App from './application/application.js'
import HomePageController from './apps/home_page/controller.js';
import HistoryTraceController from './apps/history_trace/controller.js';

export default Backbone.Router.extend({
    routes: {
        '': 'redirect',
        'home': 'home',
        'history': 'history'
    },

    redirect: function () {
        Backbone.history.navigate('#home', {trigger: true});
    },

    home: function () {
        this.controller = new HomePageController({container: App.layout.container});
    },

    history: function () {
        this.controller = new HistoryTraceController({container: App.layout.container});
        this.controller.showPage();
    }
});