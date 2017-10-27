import {LayoutView} from 'marionette';
import HomePageLayout from './home_page.hbs';

require('./home_page.scss');

export default LayoutView.extend({
    className: 'home col-md-6 col-md-offset-3',
    template: HomePageLayout,

    regions: {
        pkgList: '.pkg-name-list'
    },

    triggers: {
        'click #show-trace-component': 'show:traceComponent',
        'click #update-pkg-names': 'update:pkgNames',
        'click #auto-trace': 'auto:trace',
        'click #dump-dex': 'dump:dex'
    }
})